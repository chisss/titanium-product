package com.titanium.product.pricing.aggregate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

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

    private static final Set<String> SUPPORTED_DIMENSIONS =
            Set.of("age", "gender", "paymentTerm", "coverageTerm");

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
        rows = List.copyOf(Objects.requireNonNull(newRows, "费率行不能为空"));
        contentHash = "";
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
        if (dimensionKeys.isEmpty() || dimensionKeys.size() != Set.copyOf(dimensionKeys).size()
                || !SUPPORTED_DIMENSIONS.containsAll(dimensionKeys)) {
            throw invalid(ProductErrorCode.RATE_TABLE_VALIDATION_FAILED, "维度定义为空、重复或包含不支持的维度");
        }
    }

    private boolean overlaps(RateTableRow left, RateTableRow right) {
        return ageOverlaps(left, right)
                && textOverlaps(left.gender(), right.gender())
                && valueOverlaps(left.paymentTermYears(), right.paymentTermYears())
                && valueOverlaps(left.coverageTermYears(), right.coverageTermYears());
    }

    private boolean ageOverlaps(RateTableRow left, RateTableRow right) {
        int leftStart = left.ageFrom() == null ? Integer.MIN_VALUE : left.ageFrom();
        int rightStart = right.ageFrom() == null ? Integer.MIN_VALUE : right.ageFrom();
        int leftEnd = left.ageToExclusive() == null ? Integer.MAX_VALUE : left.ageToExclusive();
        int rightEnd = right.ageToExclusive() == null ? Integer.MAX_VALUE : right.ageToExclusive();
        return Math.max(leftStart, rightStart) < Math.min(leftEnd, rightEnd);
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

    private static String nullableDecimal(java.math.BigDecimal value) {
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
