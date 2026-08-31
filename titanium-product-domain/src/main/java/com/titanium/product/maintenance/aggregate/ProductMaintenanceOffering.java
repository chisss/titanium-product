package com.titanium.product.maintenance.aggregate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.common.enums.ProductMaintenanceOfferingStatus;
import com.titanium.product.exception.ProductMaintenanceOfferingException;

/** Product 域版本化保全 Offering。 */
public class ProductMaintenanceOffering {

    private static final Pattern ITEM_CODE = Pattern.compile("[A-Z][A-Z0-9_]{2,63}");
    private static final Pattern SHA_256 = Pattern.compile("[a-f0-9]{64}");

    private final String offeringId;
    private final String tenantId;
    private final String productId;
    private final String productVersion;
    private final String planVersion;
    private final String offeringVersion;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final Set<String> allowedPolicyStatuses;
    private final Set<String> allowedChannels;
    private final Set<String> allowedItemCodes;
    private ProductMaintenanceOfferingStatus status;
    private String contentHash;

    private ProductMaintenanceOffering(
            String offeringId,
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String offeringVersion,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            Set<String> allowedPolicyStatuses,
            Set<String> allowedChannels,
            Set<String> allowedItemCodes,
            ProductMaintenanceOfferingStatus status,
            String contentHash) {
        this.offeringId = requireText("offeringId", offeringId);
        this.tenantId = requireText("tenantId", tenantId);
        this.productId = requireText("productId", productId);
        this.productVersion = requireText("productVersion", productVersion);
        this.planVersion = requireText("planVersion", planVersion);
        this.offeringVersion = requireText("offeringVersion", offeringVersion);
        this.effectiveFrom = requireValue("effectiveFrom", effectiveFrom);
        this.effectiveTo = effectiveTo;
        this.allowedPolicyStatuses = immutableCodes("allowedPolicyStatuses", allowedPolicyStatuses, false);
        this.allowedChannels = immutableCodes("allowedChannels", allowedChannels, false);
        this.allowedItemCodes = immutableCodes("allowedItemCodes", allowedItemCodes, true);
        this.status = requireValue("status", status);
        this.contentHash = normalizeHash(status, contentHash);
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw failure(ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID,
                    "Offering失效时间必须晚于生效时间");
        }
    }

    /** 创建 Offering 草稿。 */
    public static ProductMaintenanceOffering createDraft(
            String offeringId,
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String offeringVersion,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            Set<String> allowedPolicyStatuses,
            Set<String> allowedChannels,
            Set<String> allowedItemCodes) {
        return new ProductMaintenanceOffering(
                offeringId, tenantId, productId, productVersion, planVersion, offeringVersion,
                effectiveFrom, effectiveTo, allowedPolicyStatuses, allowedChannels, allowedItemCodes,
                ProductMaintenanceOfferingStatus.DRAFT, "");
    }

    /** 从持久化事实恢复 Offering。 */
    public static ProductMaintenanceOffering restore(
            String offeringId,
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String offeringVersion,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            Set<String> allowedPolicyStatuses,
            Set<String> allowedChannels,
            Set<String> allowedItemCodes,
            ProductMaintenanceOfferingStatus status,
            String contentHash) {
        return new ProductMaintenanceOffering(
                offeringId, tenantId, productId, productVersion, planVersion, offeringVersion,
                effectiveFrom, effectiveTo, allowedPolicyStatuses, allowedChannels, allowedItemCodes,
                status, contentHash);
    }

    /** 发布 Offering 并冻结规范化内容摘要。 */
    public String publish() {
        requireStatus(ProductMaintenanceOfferingStatus.DRAFT, "发布");
        contentHash = calculateContentHash();
        status = ProductMaintenanceOfferingStatus.PUBLISHED;
        return contentHash;
    }

    /** 退役 Offering，仅阻止新案件继续解析。 */
    public void retire() {
        requireStatus(ProductMaintenanceOfferingStatus.PUBLISHED, "退役");
        status = ProductMaintenanceOfferingStatus.RETIRED;
    }

    /** 校验当前 Offering 是否适用于指定案件上下文。 */
    public boolean appliesTo(String policyStatus, String channel, LocalDateTime businessTime) {
        return status == ProductMaintenanceOfferingStatus.PUBLISHED
                && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo))
                && allowedPolicyStatuses.contains(normalizeCode(policyStatus))
                && allowedChannels.contains(normalizeCode(channel));
    }

    private String calculateContentHash() {
        String canonical = String.join("|",
                tenantId, productId, productVersion, planVersion, offeringVersion,
                effectiveFrom.toString(), effectiveTo == null ? "" : effectiveTo.toString(),
                String.join(",", allowedPolicyStatuses),
                String.join(",", allowedChannels),
                String.join(",", allowedItemCodes));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK缺少SHA-256实现", exception);
        }
    }

    private void requireStatus(ProductMaintenanceOfferingStatus expected, String operation) {
        if (status != expected) {
            throw failure(ProductMaintenanceOfferingFailureReason.STATE_INVALID,
                    operation + "要求Offering状态为" + expected.getCode());
        }
    }

    private static Set<String> immutableCodes(String fieldName, Set<String> values, boolean itemCodes) {
        if (values == null || values.isEmpty()) {
            throw failure(ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID,
                    fieldName + "不能为空");
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String value : values) {
            String code = normalizeCode(value);
            if (itemCodes && !ITEM_CODE.matcher(code).matches()) {
                throw failure(ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID,
                        "保全项编码格式非法: " + code);
            }
            normalized.add(code);
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static String normalizeCode(String value) {
        return requireText("code", value).toUpperCase(Locale.ROOT);
    }

    private static String normalizeHash(ProductMaintenanceOfferingStatus status, String value) {
        String hash = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        boolean frozen = status == ProductMaintenanceOfferingStatus.PUBLISHED
                || status == ProductMaintenanceOfferingStatus.RETIRED;
        if (frozen && !SHA_256.matcher(hash).matches()) {
            throw failure(ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID,
                    "已发布或已退役Offering必须包含SHA-256摘要");
        }
        if (!frozen && !hash.isEmpty()) {
            throw failure(ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID,
                    "草稿Offering不能包含内容摘要");
        }
        return hash;
    }

    private static String requireText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw failure(ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID,
                    fieldName + "不能为空");
        }
        return value.trim();
    }

    private static <T> T requireValue(String fieldName, T value) {
        if (value == null) {
            throw failure(ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID,
                    fieldName + "不能为空");
        }
        return value;
    }

    private static ProductMaintenanceOfferingException failure(
            ProductMaintenanceOfferingFailureReason reason, String message) {
        return new ProductMaintenanceOfferingException(reason, message);
    }

    public String offeringId() {
        return offeringId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String productId() {
        return productId;
    }

    public String productVersion() {
        return productVersion;
    }

    public String planVersion() {
        return planVersion;
    }

    public String offeringVersion() {
        return offeringVersion;
    }

    public LocalDateTime effectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime effectiveTo() {
        return effectiveTo;
    }

    public Set<String> allowedPolicyStatuses() {
        return allowedPolicyStatuses;
    }

    public Set<String> allowedChannels() {
        return allowedChannels;
    }

    public Set<String> allowedItemCodes() {
        return allowedItemCodes;
    }

    public ProductMaintenanceOfferingStatus status() {
        return status;
    }

    public String contentHash() {
        return contentHash;
    }

    // ==================== 持久化映射访问器（MapStruct getXxx 约定） ====================

    public String getOfferingId() {
        return offeringId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public String getPlanVersion() {
        return planVersion;
    }

    public String getOfferingVersion() {
        return offeringVersion;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public Set<String> getAllowedPolicyStatuses() {
        return allowedPolicyStatuses;
    }

    public Set<String> getAllowedChannels() {
        return allowedChannels;
    }

    public Set<String> getAllowedItemCodes() {
        return allowedItemCodes;
    }

    public ProductMaintenanceOfferingStatus getStatus() {
        return status;
    }

    public String getContentHash() {
        return contentHash;
    }
}
