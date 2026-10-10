package com.titanium.product.pricing.aggregate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.titanium.metadata.enums.product.RateDimensionKey;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.valueobject.rate.RateTableValidationResult;

/**
 * Product 费率表定义聚合。
 * <p>
 * 草稿允许整体替换费率行；发布后内容不可修改，修订必须创建新版本。
 * </p>
 */
public class RateTableDefinition {

    private final String tableId;
    private final String productId;
    private final String tableCode;
    private final String tableVersion;
    private final RateUnit rateUnit;
    private final String currency;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final List<String> dimensionKeys;
    private final String tenantId;
    private RateTableStatus status;
    private List<RateTableRow> rows;
    private String contentHash;

    private RateTableDefinition(
            String tableId,
            String productId,
            String tableCode,
            String tableVersion,
            RateTableStatus status,
            RateUnit rateUnit,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            List<String> dimensionKeys,
            String tenantId,
            List<RateTableRow> rows,
            String contentHash) {
        this.tableId = requireText(tableId, "费率表ID");
        this.productId = requireText(productId, "产品ID");
        this.tableCode = requireText(tableCode, "费率表编码");
        this.tableVersion = requireText(tableVersion, "费率表版本");
        this.status = Objects.requireNonNull(status, "费率表状态不能为空");
        this.rateUnit = Objects.requireNonNull(rateUnit, "费率单位不能为空");
        this.currency = normalizeCurrency(currency);
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.dimensionKeys = List.copyOf(Objects.requireNonNull(dimensionKeys, "维度定义不能为空"));
        this.tenantId = requireText(tenantId, "租户ID");
        this.rows = List.copyOf(rows == null ? List.of() : rows);
        this.contentHash = contentHash == null ? "" : contentHash;
        validateMetadata();
    }

    public static RateTableDefinition createDraft(
            String tableId,
            String productId,
            String tableCode,
            String tableVersion,
            RateUnit rateUnit,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            List<String> dimensionKeys,
            String tenantId) {
        return new RateTableDefinition(
                tableId, productId, tableCode, tableVersion, RateTableStatus.DRAFT, rateUnit, currency,
                effectiveFrom, effectiveTo, dimensionKeys, tenantId, List.of(), "");
    }

    public static RateTableDefinition restore(
            String tableId,
            String productId,
            String tableCode,
            String tableVersion,
            RateTableStatus status,
            RateUnit rateUnit,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            List<String> dimensionKeys,
            String tenantId,
            List<RateTableRow> rows,
            String contentHash) {
        return new RateTableDefinition(
                tableId, productId, tableCode, tableVersion, status, rateUnit, currency, effectiveFrom,
                effectiveTo, dimensionKeys, tenantId, rows, contentHash);
    }

    /** 整体替换草稿费率行。 */
    public void replaceRows(List<RateTableRow> newRows) {
        requireDraft();
        List<RateTableRow> candidates = List.copyOf(Objects.requireNonNull(newRows, "费率行不能为空"));
        requireNoDuplicateDimension(candidates);
        rows = candidates;
        contentHash = "";
    }

    /**
     * 保存期守卫：匹配维度完全相同的行在库侧有唯一约束（{@code uk_product_rate_row_dimension}），
     * 落库必冲突。
     * <p>
     * 重复维度是行集合自身的不变量（不依赖任何外部数据），故内聚在聚合根、在保存入口拦截，
     * 而非等到发布期才校验、更不是把 DB 唯一约束当唯一防线（D-501-34）。
     * </p>
     */
    private void requireNoDuplicateDimension(List<RateTableRow> candidates) {
        Map<String, String> seenHashes = new LinkedHashMap<>();
        for (RateTableRow row : candidates) {
            String duplicatedRowId = seenHashes.putIfAbsent(row.dimensionHash(), row.rowId());
            if (duplicatedRowId != null) {
                throw invalid(ProductErrorCode.RATE_TABLE_ROW_CONFLICT,
                        "费率行维度重复: " + duplicatedRowId + ", " + row.rowId()
                                + "（同一维度组合只能有一行费率）");
            }
        }
    }

    /** 校验发布约束并生成稳定内容摘要，不改变状态。 */
    public RateTableValidationResult validateForPublish() {
        requireDraft();
        if (rows.isEmpty()) {
            throw invalid(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED, "费率表至少需要一行费率");
        }
        for (int left = 0; left < rows.size(); left++) {
            for (int right = left + 1; right < rows.size(); right++) {
                if (overlaps(rows.get(left), rows.get(right))) {
                    throw invalid(ProductErrorCode.RATE_TABLE_ROW_CONFLICT,
                            "费率行可能重复命中: " + rows.get(left).rowId() + ", " + rows.get(right).rowId());
                }
            }
        }
        return new RateTableValidationResult(rows.size(), hash(canonicalContent()));
    }

    /** 发布当前草稿，发布后的内容不可再修改。 */
    public RateTableValidationResult publish() {
        RateTableValidationResult validation = validateForPublish();
        status = RateTableStatus.PUBLISHED;
        contentHash = validation.contentHash();
        return validation;
    }

    /** 退役已发布版本。 */
    public void retire() {
        if (status != RateTableStatus.PUBLISHED) {
            throw invalid(ProductErrorCode.RATE_TABLE_STATUS_INVALID, "只有已发布费率表可以退役");
        }
        status = RateTableStatus.RETIRED;
    }

    private void validateMetadata() {
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED, "失效时间必须晚于生效时间");
        }
        if (currency.length() != 3) {
            throw invalid(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED, "币种必须是3位代码");
        }
        // 三类拒绝理由分开报，且不支持的键要点名并附受支持全集——合并成一句「为空、重复或不支持」
        // 会让运营不知道该改哪一个键，只能逐个试。
        if (dimensionKeys.isEmpty()) {
            throw invalid(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED, "维度定义不能为空");
        }
        if (dimensionKeys.size() != Set.copyOf(dimensionKeys).size()) {
            throw invalid(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED,
                    "维度定义存在重复: " + dimensionKeys);
        }
        // 🔴 成员判定一律走 RateDimensionKey.isSupported——它是该取值域的唯一入口。此前这里另有一份
        // 写死的维度键字面量（"age" / "gender" / …）与枚举各存一份清单，改一处必漂移，且漂移后表现为
        // 「维度在下拉里选不到」或「配了维度却永不命中」这类静默失效。
        // （措辞上刻意不写出「Set + of + 具体键」的原文：AC-05 的反向断言是文本级 grep，
        //   注释里复现该 token 会让「字面量已清零」这条机械判据恒假。）
        List<String> unsupported = dimensionKeys.stream()
                .filter(key -> !RateDimensionKey.isSupported(key))
                .distinct()
                .toList();
        if (!unsupported.isEmpty()) {
            throw invalid(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED,
                    "不支持的费率维度: " + String.join(", ", unsupported)
                            + "；受支持维度: " + String.join(", ", supportedDimensionCodes()));
        }
    }

    /**
     * 受支持维度键的全集（稳定排序，供错误提示与前端契约自证）。
     * <p>
     * 取自 {@link RateDimensionKey}，<b>不是</b>另写的字符串清单。
     * </p>
     */
    public static Set<String> supportedDimensionCodes() {
        return Arrays.stream(RateDimensionKey.values())
                .sorted(Comparator.comparingInt(RateDimensionKey::getEnumCode))
                .map(RateDimensionKey::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean overlaps(RateTableRow left, RateTableRow right) {
        return ageOverlaps(left, right)
                && textOverlaps(left.gender(), right.gender())
                && valueOverlaps(left.paymentTermYears(), right.paymentTermYears())
                && valueOverlaps(left.coverageTermYears(), right.coverageTermYears())
                && textOverlaps(left.occupationClass(), right.occupationClass())
                && textOverlaps(left.region(), right.region())
                && textOverlaps(left.vehicleType(), right.vehicleType())
                && amountOverlaps(left.sumInsuredFrom(), left.sumInsuredToExclusive(),
                        right.sumInsuredFrom(), right.sumInsuredToExclusive())
                && textOverlaps(left.smoking(), right.smoking())
                && textOverlaps(left.socialInsurance(), right.socialInsurance())
                && amountOverlaps(left.deductibleFrom(), left.deductibleToExclusive(),
                        right.deductibleFrom(), right.deductibleToExclusive());
    }

    private boolean ageOverlaps(RateTableRow left, RateTableRow right) {
        int leftStart = left.ageFrom() == null ? Integer.MIN_VALUE : left.ageFrom();
        int rightStart = right.ageFrom() == null ? Integer.MIN_VALUE : right.ageFrom();
        int leftEnd = left.ageToExclusive() == null ? Integer.MAX_VALUE : left.ageToExclusive();
        int rightEnd = right.ageToExclusive() == null ? Integer.MAX_VALUE : right.ageToExclusive();
        return Math.max(leftStart, rightStart) < Math.min(leftEnd, rightEnd);
    }

    /**
     * 区间型金额维度的重叠判定：两个半开区间 {@code [from, toExclusive)} 是否相交。
     * <p>
     * 与年龄区间同构——空端点表示该侧无界（{@code null} 起点即 −∞、{@code null} 终点即 +∞），
     * 两区间相交的充要条件是 {@code max(起点) < min(终点)}。
     * </p>
     * <p>
     * 🔴 用 {@link BigDecimal#compareTo} 而非 {@code equals}：{@code 1000} 与 {@code 1000.00}
     * 数值相等但 scale 不同，用 equals 会把「同一区间」判成不重叠，发布期漏过重复行。
     * </p>
     */
    private boolean amountOverlaps(
            BigDecimal leftFrom, BigDecimal leftToExclusive,
            BigDecimal rightFrom, BigDecimal rightToExclusive) {
        BigDecimal maxStart = maxStart(leftFrom, rightFrom);
        BigDecimal minEnd = minEnd(leftToExclusive, rightToExclusive);
        // 终点无界（+∞）或起点无界（−∞）时必然相交；此时另一侧无论取何值都构不成「起点 ≥ 终点」
        if (maxStart == null || minEnd == null) {
            return true;
        }
        return maxStart.compareTo(minEnd) < 0;
    }

    /** 取两个区间起点的较大者，{@code null} 视作 −∞（另一侧非空时由它胜出）。 */
    private BigDecimal maxStart(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    /** 取两个区间终点的较小者，{@code null} 视作 +∞（另一侧非空时由它胜出）。 */
    private BigDecimal minEnd(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) <= 0 ? left : right;
    }

    private boolean textOverlaps(String left, String right) {
        return left == null || right == null || "ALL".equals(left) || "ALL".equals(right)
                || left.equalsIgnoreCase(right);
    }

    private boolean valueOverlaps(Integer left, Integer right) {
        return left == null || right == null || left.equals(right);
    }

    private String canonicalContent() {
        String header = String.join("|", tableCode, tableVersion, rateUnit.name(), currency,
                effectiveFrom.toString(), effectiveTo == null ? "*" : effectiveTo.toString(),
                String.join(",", dimensionKeys));
        String rowContent = rows.stream()
                .map(this::canonicalRow)
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return header + "\n" + rowContent;
    }

    private String canonicalRow(RateTableRow row) {
        return String.join("|",
                nullable(row.ageFrom()), nullable(row.ageToExclusive()), nullable(row.gender()),
                nullable(row.paymentTermYears()), nullable(row.coverageTermYears()),
                nullable(row.occupationClass()), nullable(row.region()), nullable(row.vehicleType()),
                nullableDecimal(row.sumInsuredFrom()), nullableDecimal(row.sumInsuredToExclusive()),
                nullable(row.smoking()), nullable(row.socialInsurance()),
                nullableDecimal(row.deductibleFrom()), nullableDecimal(row.deductibleToExclusive()),
                row.rate().stripTrailingZeros().toPlainString(), nullableDecimal(row.minimumPremium()),
                nullableDecimal(row.maximumPremium()));
    }

    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持SHA-256", exception);
        }
    }

    private void requireDraft() {
        if (status != RateTableStatus.DRAFT) {
            throw invalid(ProductErrorCode.RATE_TABLE_STATUS_INVALID, "只有草稿费率表可以修改或发布");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalid(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED, fieldName + "不能为空");
        }
        return value.trim();
    }

    private static String normalizeCurrency(String currency) {
        return requireText(currency, "币种").toUpperCase(Locale.ROOT);
    }

    private static String nullable(Object value) {
        return value == null ? "*" : value.toString();
    }

    private static String nullableDecimal(BigDecimal value) {
        return value == null ? "*" : value.stripTrailingZeros().toPlainString();
    }

    private static PricingDomainException invalid(ProductErrorCode errorCode, String detail) {
        return new PricingDomainException(errorCode, detail);
    }

    public String tableId() {
        return tableId;
    }

    public String productId() {
        return productId;
    }

    public String tableCode() {
        return tableCode;
    }

    public String tableVersion() {
        return tableVersion;
    }

    public RateTableStatus status() {
        return status;
    }

    public RateUnit rateUnit() {
        return rateUnit;
    }

    public String currency() {
        return currency;
    }

    public LocalDateTime effectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime effectiveTo() {
        return effectiveTo;
    }

    public List<String> dimensionKeys() {
        return dimensionKeys;
    }

    public String tenantId() {
        return tenantId;
    }

    public List<RateTableRow> rows() {
        return rows;
    }

    public String contentHash() {
        return contentHash;
    }

    // ==================== 持久化映射访问器（MapStruct getXxx 约定） ====================

    public String getTableId() {
        return tableId;
    }

    public String getProductId() {
        return productId;
    }

    public String getTableCode() {
        return tableCode;
    }

    public String getTableVersion() {
        return tableVersion;
    }

    public RateUnit getRateUnit() {
        return rateUnit;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public List<String> getDimensionKeys() {
        return dimensionKeys;
    }

    public String getTenantId() {
        return tenantId;
    }

    public RateTableStatus getStatus() {
        return status;
    }

    public List<RateTableRow> getRows() {
        return rows;
    }

    public String getContentHash() {
        return contentHash;
    }
}
