package com.titanium.product.web.export;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.titanium.product.query.result.ProductQueryResult;

/**
 * 产品列表 CSV 导出器（表现层格式转换，不含业务规则）。
 * <p>
 * Excel 兼容三要点：① 前置 UTF-8 BOM——否则 Excel 按 GBK 解析导致中文乱码；
 * ② 行分隔符用 CRLF；③ 含逗号/引号/换行的字段以双引号包裹、内部引号翻倍（RFC 4180）。
 * </p>
 */
public final class ProductCsvExporter {

    /** Excel 识别 UTF-8 所需的字节顺序标记 */
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final String CRLF = "\r\n";

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 导出列顺序（与 {@link #toRow} 严格对应） */
    private static final String[] HEADERS = {
            "产品ID", "产品编号", "产品代码", "产品名称", "产品描述",
            "产品形态", "险种类型", "产品分类", "版本", "状态",
            "生效时间", "失效时间", "创建时间",
    };

    private ProductCsvExporter() {
    }

    /**
     * 将产品列表序列化为 CSV 字节流（含表头与 BOM）。
     *
     * @param products 产品查询结果列表
     * @return UTF-8 编码、带 BOM 的 CSV 字节流
     */
    public static byte[] toCsv(List<ProductQueryResult> products) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", HEADERS)).append(CRLF);
        if (products != null) {
            for (ProductQueryResult product : products) {
                csv.append(toRow(product)).append(CRLF);
            }
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, withBom, UTF8_BOM.length, body.length);
        return withBom;
    }

    /** 拼接单行数据（列顺序须与 {@link #HEADERS} 一致） */
    private static String toRow(ProductQueryResult product) {
        return String.join(",",
                cell(product.getProductId()),
                cell(product.getProductNo()),
                cell(product.getProductCode()),
                cell(product.getProductName()),
                cell(product.getProductDesc()),
                cell(enumName(product.getForm())),
                cell(enumName(product.getInsuranceType())),
                cell(enumName(product.getCategory())),
                cell(product.getVersion()),
                cell(enumName(product.getStatus())),
                cell(formatTime(product.getEffectiveTime())),
                cell(formatTime(product.getInvalidTime())),
                cell(formatTime(product.getCreatedAt())));
    }

    /** 枚举取常量名（与 JSON 序列化口径一致），空值转空串 */
    private static String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    /** 时间格式化，空值转空串 */
    private static String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME_FORMAT);
    }

    /** CSV 单元格转义（RFC 4180）：含分隔符/引号/换行时以双引号包裹，内部引号翻倍 */
    private static String cell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needsQuoting = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        return needsQuoting ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }
}
