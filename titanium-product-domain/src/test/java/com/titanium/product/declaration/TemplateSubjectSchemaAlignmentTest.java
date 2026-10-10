package com.titanium.product.declaration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.insurance.InsuranceTypeCapability;

/**
 * 模板标的要素 Schema 与险种权威声明的对齐守卫（G11/g11-01，AC-09）。
 * <p>
 * 守的是「配置口径对齐」：{@link InsuranceTypeCapability}（G10/g10-05 权威表）为各险种声明的
 * {@code requiredSubjectFields} 必须<b>全部</b>出现在对应模板的实际 {@code required} 配置中
 * （差集为空）——即「运营照目录/权威表配置，出单校验必过关」。历史缺口实例：
 * {@code TPL-TEST-AUTO-001} 缺 {@code usageType}、{@code TPL-TEST-PROPERTY-001} 缺
 * {@code buildingStructure}（g11-01 已补）。
 * </p>
 *
 * <h3>判据分层（本测试只是三层之一，缺一不可）</h3>
 * <ol>
 *   <li><b>本测试</b>：解析两 SQL 夹具文件的模板行（纯文件读取，单测无 DB），
 *       做 4 组断言——正向子集、标的类型一致、反向发现（新模板须登记）、值域声明；</li>
 *   <li><b>验收脚本</b> {@code ~/.g11-snapshots/validate-g11-01.sh}：独立 Python 实现
 *       全量对账 + 真机导入后从 DB 读回——「SQL 转义 → MySQL 真解析」的权威实现是 MySQL 本身，
 *       本测试的 Java 解码器只是其近似（配「未知转义即抛」防静默错译）；</li>
 *   <li><b>反向注入</b>（验收脚本执行）：删一条 required（如 {@code vin}）后本测试必须判红。</li>
 * </ol>
 *
 * <h3>🔴 修改纪律（触发本测试的变更须同批处理）</h3>
 * <ul>
 *   <li>改任一 SQL 文件的模板 {@code required} → 本测试即校验声明子集，缺字段判红；</li>
 *   <li><b>新增模板</b>（两文件内）→ 必须补 {@link #TEMPLATE_TYPE_REGISTRY}，否则反向发现断言判红
 *       （迫使维护者显式决定新模板归属哪个险种）；</li>
 *   <li>改 {@code VehicleUsageType}（underwriting-common）→ 同批改两条 AUTO 模板 schema 的
 *       {@code properties.usageType.enum} 与本测试 T5 期望值（跨模块无编译期依赖，靠三处纪律钉住）。</li>
 * </ul>
 *
 * <h3>🔴 文件方言说明（勿被转义写法困惑）</h3>
 * <p>
 * 两文件的 JSON-in-SQL 转义写法<b>不同</b>：{@code multi-insurance-nine-products.sql} 用
 * {@code \\"}（三字符组），{@code multi-insurance-product-test-data.sql} 用 {@code \\\"}
 * （四字符组，引号前多一层）；经 MySQL 字符串解析<b>终态一致</b>（均为 {@code \"} 即外层
 * JSON 的转义引号）。本测试的 {@link #decodeSqlLiteral} 按 MySQL 语义解码，两种写法
 * 等价通过——判据锁「语义」不锁「转义形态」。
 * </p>
 *
 * @author wei.sun
 * @since 2026/10/10
 * @see DeclarationRegistrationGuardTest 同族守卫（G10/g10-06，声明即登记）
 */
class TemplateSubjectSchemaAlignmentTest {

    /** 数据源：两 SQL 夹具文件（相对仓库根 titanium-test/data/） */
    private static final List<String> SQL_FILES = List.of(
            "multi-insurance-nine-products.sql",
            "multi-insurance-product-test-data.sql");

    /** 三险种（AC-09 判据面）在注册表中的模板 */
    private static final List<String> AUTO_TEMPLATES = List.of("STD-TPL-AUTO", "TPL-TEST-AUTO-001");

    /** 三险种声明字段总数下界（AC-09 独立口径：2026-10-10 从权威表核对时点数出，5+4+3；只升不降） */
    private static final int THREE_TYPE_DECLARED_LOWER_BOUND = 12;

    /**
     * 模板 → 险种显式登记表（11 条 = 九险种 STD-TPL-* + 测试数据 2 条）。
     * 映射正确性由 T1（字段集合 ⊆）+ T2（标的类型一致）双重自证；新增模板未登记则 T3 判红。
     */
    private static final Map<String, InsuranceProductType> TEMPLATE_TYPE_REGISTRY = new LinkedHashMap<>();

    static {
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-TERM", InsuranceProductType.TERM_LIFE);
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-WHOLE", InsuranceProductType.WHOLE_LIFE);
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-CI", InsuranceProductType.CRITICAL_ILLNESS);
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-MED", InsuranceProductType.MEDICAL);
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-ACC", InsuranceProductType.ACCIDENT_COMPREHENSIVE);
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-AUTO", InsuranceProductType.AUTO);
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-ENT", InsuranceProductType.ENTERPRISE_PROPERTY);
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-HOUSE", InsuranceProductType.HOUSEHOLD_PROPERTY);
        TEMPLATE_TYPE_REGISTRY.put("STD-TPL-EL", InsuranceProductType.EMPLOYER_LIABILITY);
        TEMPLATE_TYPE_REGISTRY.put("TPL-TEST-AUTO-001", InsuranceProductType.AUTO);
        TEMPLATE_TYPE_REGISTRY.put("TPL-TEST-PROPERTY-001", InsuranceProductType.ENTERPRISE_PROPERTY);
    }

    @Test
    @DisplayName("T1 正向：各模板 required ⊇ 对应险种权威声明（差集为空——运营照权威表配置必可过校验）")
    void templateRequiredMustCoverDeclaredFields() {
        Map<String, TemplateSchema> parsed = parseAllTemplates();

        for (Map.Entry<String, InsuranceProductType> entry : TEMPLATE_TYPE_REGISTRY.entrySet()) {
            String templateId = entry.getKey();
            InsuranceProductType type = entry.getValue();

            TemplateSchema schema = parsed.get(templateId);
            assertNotNull(schema, templateId + " 已登记却未在两 SQL 文件中解析到（登记表与文件失配）");

            InsuranceTypeCapability capability = InsuranceTypeCapability.byType(type);
            assertNotNull(capability, type + " 不在权威表（g10-05）内，登记表引用无效险种");

            Set<String> declared = new LinkedHashSet<>(capability.getRequiredSubjectFields());
            Set<String> missing = new LinkedHashSet<>(declared);
            missing.removeAll(schema.required());

            assertTrue(missing.isEmpty(),
                    templateId + "（" + type + "）缺权威表声明字段: " + missing
                            + "；声明=" + declared + "，模板 required=" + schema.required());
        }
    }

    @Test
    @DisplayName("T2 标的类型一致：模板 subjectType == 权威表 subjectType（防登记表把模板挂错险种）")
    void templateSubjectTypeMustMatchCapability() {
        Map<String, TemplateSchema> parsed = parseAllTemplates();

        for (Map.Entry<String, InsuranceProductType> entry : TEMPLATE_TYPE_REGISTRY.entrySet()) {
            TemplateSchema schema = parsed.get(entry.getKey());
            assertNotNull(schema, entry.getKey() + " 未解析到");

            InsuranceTypeCapability capability = InsuranceTypeCapability.byType(entry.getValue());
            assertEquals(capability.getSubjectType().name(), schema.subjectType(),
                    entry.getKey() + " 的标的类型与权威表（" + entry.getValue() + "）不符；"
                            + "若确为映射登记错误，请修 TEMPLATE_TYPE_REGISTRY（而非改模板）");
        }
    }

    @Test
    @DisplayName("T3 反向发现：两文件内全部模板须在登记表内（新增模板漏登记即判红）")
    void allDiscoveredTemplatesMustBeRegistered() {
        Map<String, TemplateSchema> parsed = parseAllTemplates();

        Set<String> unregistered = new LinkedHashSet<>(parsed.keySet());
        unregistered.removeAll(TEMPLATE_TYPE_REGISTRY.keySet());

        assertTrue(unregistered.isEmpty(),
                "发现未登记模板: " + unregistered
                        + "；新增模板必须同批补 TEMPLATE_TYPE_REGISTRY 并核对所属险种的 required 声明");
    }

    @Test
    @DisplayName("T4 计数下界：三险种声明字段总数 >= 12（AC-09 独立口径，只升不降）")
    void threeTypeDeclaredFieldsLowerBound() {
        int total = 0;
        for (InsuranceProductType type : List.of(InsuranceProductType.AUTO,
                InsuranceProductType.ENTERPRISE_PROPERTY, InsuranceProductType.HOUSEHOLD_PROPERTY)) {
            InsuranceTypeCapability capability = InsuranceTypeCapability.byType(type);
            assertNotNull(capability, type + " 不在权威表内");
            total += capability.getRequiredSubjectFields().size();
        }

        assertTrue(total >= THREE_TYPE_DECLARED_LOWER_BOUND,
                "三险种声明字段总数=" + total + " < 下界 " + THREE_TYPE_DECLARED_LOWER_BOUND
                        + "（=5+4+3，2026-10-10 独立口径；减少意味着声明被误删）");
    }

    @Test
    @DisplayName("T5 值域声明：两条 AUTO 模板 usageType.enum == VehicleUsageType 三值（防自由文本回潮）")
    void autoUsageTypeMustDeclareEnum() {
        Map<String, TemplateSchema> parsed = parseAllTemplates();
        Set<String> expected = Set.of("FAMILY", "NON_OPERATING", "OPERATING");

        for (String templateId : AUTO_TEMPLATES) {
            TemplateSchema schema = parsed.get(templateId);
            assertNotNull(schema, templateId + " 未解析到");

            JSONObject properties = schema.innerSchema().getJSONObject("properties");
            assertNotNull(properties, templateId + " 缺 properties（usageType 值域未声明，自由文本回潮）");

            JSONObject usageType = properties.getJSONObject("usageType");
            assertNotNull(usageType, templateId + " 缺 properties.usageType（值域未声明）");

            JSONArray enumValues = usageType.getJSONArray("enum");
            assertNotNull(enumValues, templateId + " 的 usageType 缺 enum");

            assertEquals(expected, new HashSet<>(enumValues.toJavaList(String.class)),
                    templateId + " 的 usageType 值域与 VehicleUsageType（underwriting-common）不一致；"
                            + "改枚举须同批改两处 schema 与本断言");
        }
    }

    // ────────────────────────── 解析实现 ──────────────────────────

    /** 一条模板的解析结果：ID / 标的类型 / required 字段集 / 内层 Schema（供值域断言） */
    private record TemplateSchema(String templateId, String subjectType,
                                  List<String> required, JSONObject innerSchema) {
    }

    private static final Pattern INSERT_PATTERN =
            Pattern.compile("INSERT INTO t_product_template_view", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_START_PATTERN = Pattern.compile("\\(\\s*'");

    /**
     * 解析两 SQL 文件全部模板条目。
     * <p>
     * 链条：INSERT 语句窗口（至分号）→ 按 {@code (\s*'} 切 VALUES 组 → 组内扫描 SQL 单引号
     * 字面量（转义感知）→ 第一个字面量=模板 ID → 含 {@code subjectFieldsSchema} 的字面量
     * = policy_structure_json → MySQL 解码 → 外层 JSON → 内层 Schema → required。
     * </p>
     */
    private static Map<String, TemplateSchema> parseAllTemplates() {
        Map<String, TemplateSchema> result = new LinkedHashMap<>();

        for (String fileName : SQL_FILES) {
            String text = readTestDataFile(fileName);
            Matcher insertMatcher = INSERT_PATTERN.matcher(text);
            while (insertMatcher.find()) {
                int statementEnd = text.indexOf(';', insertMatcher.start());
                assertTrue(statementEnd > 0, fileName + " 的模板 INSERT 语句缺分号");
                String statement = text.substring(insertMatcher.start(), statementEnd);

                List<Integer> groupStarts = new ArrayList<>();
                Matcher groupMatcher = GROUP_START_PATTERN.matcher(statement);
                while (groupMatcher.find()) {
                    groupStarts.add(groupMatcher.start());
                }

                for (int k = 0; k < groupStarts.size(); k++) {
                    int from = groupStarts.get(k);
                    int to = k + 1 < groupStarts.size() ? groupStarts.get(k + 1) : statement.length();
                    TemplateSchema schema = parseOneGroup(statement.substring(from, to), fileName);
                    TemplateSchema existing = result.put(schema.templateId(), schema);
                    assertTrue(existing == null,
                            "模板 ID 在两文件间重复: " + schema.templateId());
                }
            }
        }

        assertTrue(!result.isEmpty(), "两 SQL 文件均未解析到任何模板条目（文件被清空或解析口径失效）");
        return result;
    }

    /** 解析单个 VALUES 组；结构缺失（无 schema 字面量）即抛——不静默跳过 */
    private static TemplateSchema parseOneGroup(String group, String fileName) {
        List<String> literals = scanSqlLiterals(group);
        assertTrue(!literals.isEmpty(), fileName + " 存在无字面量的 VALUES 组: " + group);

        String templateId = decodeSqlLiteral(literals.get(0));

        for (String raw : literals) {
            String decoded = decodeSqlLiteral(raw);
            if (decoded.startsWith("{") && decoded.contains("\"subjectFieldsSchema\"")) {
                JSONObject outer = JSON.parseObject(decoded);
                String innerText = outer.getString("subjectFieldsSchema");
                assertTrue(innerText != null, templateId + " 缺 subjectFieldsSchema 字段值");
                JSONObject inner = JSON.parseObject(innerText);

                JSONArray requiredArray = inner.getJSONArray("required");
                List<String> required = requiredArray == null
                        ? List.of() : requiredArray.toJavaList(String.class);

                return new TemplateSchema(templateId, outer.getString("subjectType"), required, inner);
            }
        }

        throw new IllegalStateException(
                fileName + " 的模板组缺含 subjectFieldsSchema 的字面量（结构变更须同批更新本解析器）: " + templateId);
    }

    /** 扫描 SQL 段内全部单引号字面量（返回原始内容，转义序列未解码） */
    private static List<String> scanSqlLiterals(String segment) {
        List<String> literals = new ArrayList<>();
        int i = 0;
        while (i < segment.length()) {
            if (segment.charAt(i) != '\'') {
                i++;
                continue;
            }
            StringBuilder raw = new StringBuilder();
            int j = i + 1;
            while (j < segment.length() && segment.charAt(j) != '\'') {
                if (segment.charAt(j) == '\\' && j + 1 < segment.length()) {
                    raw.append(segment.charAt(j)).append(segment.charAt(j + 1));
                    j += 2;
                } else {
                    raw.append(segment.charAt(j));
                    j++;
                }
            }
            literals.add(raw.toString());
            i = j + 1;
        }
        return literals;
    }

    /**
     * 按 MySQL 字符串语义解码字面量（{@code \\} / {@code \'} / {@code \"} 三种转义）。
     * <p>
     * 🔴 未知转义序列<b>直接抛出</b>而非原样保留——静默错译会让「解析成功」掩盖文件损坏。
     * 本文件语料内不应出现其它转义；一旦出现，须人工确认后扩展本方法（同批更新验收脚本的 Python 实现）。
     * </p>
     */
    private static String decodeSqlLiteral(String raw) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (i + 1 >= raw.length()) {
                throw new IllegalStateException("字面量尾部悬空反斜杠: " + raw);
            }
            char next = raw.charAt(++i);
            if (next == '\\' || next == '\'' || next == '"') {
                out.append(next);
            } else {
                throw new IllegalStateException("未知转义序列 \\" + next + "（防静默错译）: " + raw);
            }
        }
        return out.toString();
    }

    /** 从测试运行目录向上查找仓库根下的 titanium-test/data 目录（兼容模块目录/仓库根运行） */
    private static Path testDataDir() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("titanium-test").resolve("data");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "未找到 titanium-test/data（自 user.dir=" + System.getProperty("user.dir")
                        + " 向上逐级查找）；若仓库结构调整，须同批更新本测试与验收脚本的路径口径");
    }

    private static String readTestDataFile(String fileName) {
        Path file = testDataDir().resolve(fileName);
        assertTrue(Files.isRegularFile(file), "夹具文件不存在: " + file);
        try (Stream<String> lines = Files.lines(file)) {
            return lines.reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append)
                    .toString();
        } catch (IOException e) {
            throw new UncheckedIOException("读取夹具文件失败: " + file, e);
        }
    }
}
