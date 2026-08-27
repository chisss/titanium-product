package com.titanium.product.aggregate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.CalculationNodeType;
import com.titanium.product.common.enums.CalculationOperator;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.CalculationEdge;
import com.titanium.product.valueobject.pricing.CalculationModelRef;
import com.titanium.product.valueobject.pricing.CalculationNode;

import lombok.Getter;

/**
 * Product 版本化费用计算 DAG。
 */
@Getter
public final class CalculationModelDefinition {

    private static final int MAX_NODE_COUNT = 100;

    private final String modelId;
    private final String productId;
    private final String modelCode;
    private final String modelVersion;
    private final String modelName;
    private final String description;
    private final String currency;
    private final List<CalculationNode> nodes;
    private final List<CalculationEdge> edges;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private CalculationModelDefinition(
            String modelId,
            String productId,
            String modelCode,
            String modelVersion,
            String modelName,
            String description,
            String currency,
            List<CalculationNode> nodes,
            List<CalculationEdge> edges,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.modelId = requireText(modelId, "计算模型ID");
        this.productId = requireText(productId, "产品ID");
        this.modelCode = requireText(modelCode, "计算模型编码").toUpperCase(Locale.ROOT);
        this.modelVersion = requireText(modelVersion, "计算模型版本");
        this.modelName = requireText(modelName, "计算模型名称");
        this.description = description == null ? "" : description.trim();
        this.currency = requireText(currency, "币种").toUpperCase(Locale.ROOT);
        this.nodes = List.copyOf(nodes == null ? List.of() : nodes);
        this.edges = List.copyOf(edges == null ? List.of() : edges);
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "计算模型状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        validateMetadata();
    }

    public static CalculationModelDefinition createDraft(
            String modelId,
            String productId,
            String modelCode,
            String modelVersion,
            String modelName,
            String description,
            String currency,
            List<CalculationNode> nodes,
            List<CalculationEdge> edges,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new CalculationModelDefinition(
                modelId, productId, modelCode, modelVersion, modelName, description, currency, nodes, edges,
                effectiveFrom, effectiveTo, tenantId, ActuarialDefinitionStatus.DRAFT, "");
    }

    public static CalculationModelDefinition restore(
            String modelId,
            String productId,
            String modelCode,
            String modelVersion,
            String modelName,
            String description,
            String currency,
            List<CalculationNode> nodes,
            List<CalculationEdge> edges,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new CalculationModelDefinition(
                modelId, productId, modelCode, modelVersion, modelName, description, currency, nodes, edges,
                effectiveFrom, effectiveTo, tenantId, status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿计算模型可以审批");
        validateGraph();
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批计算模型可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布计算模型可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    public CalculationModelRef toRef() {
        if (status != ActuarialDefinitionStatus.PUBLISHED) {
            throw invalid("只有已发布计算模型可以被定价包引用");
        }
        return new CalculationModelRef(modelCode, modelVersion, contentHash);
    }

    public List<CalculationNode> topologicalNodes() {
        validateGraph();
        Map<String, CalculationNode> nodeByCode = nodes.stream()
                .collect(Collectors.toMap(CalculationNode::nodeCode, node -> node));
        Map<String, Integer> indegrees = new HashMap<>();
        Map<String, List<String>> outgoing = new HashMap<>();
        nodes.forEach(node -> indegrees.put(node.nodeCode(), 0));
        for (CalculationEdge edge : edges) {
            indegrees.compute(edge.toNodeCode(), (key, value) -> value + 1);
            outgoing.computeIfAbsent(edge.fromNodeCode(), key -> new ArrayList<>()).add(edge.toNodeCode());
        }
        Comparator<CalculationNode> order = Comparator.comparingInt(CalculationNode::executionOrder)
                .thenComparing(CalculationNode::nodeCode);
        var ready = new java.util.PriorityQueue<>(order);
        nodes.stream().filter(node -> indegrees.get(node.nodeCode()) == 0).forEach(ready::add);
        List<CalculationNode> result = new ArrayList<>();
        while (!ready.isEmpty()) {
            CalculationNode current = ready.remove();
            result.add(current);
            for (String target : outgoing.getOrDefault(current.nodeCode(), List.of())) {
                int remaining = indegrees.compute(target, (key, value) -> value - 1);
                if (remaining == 0) {
                    ready.add(nodeByCode.get(target));
                }
            }
        }
        return List.copyOf(result);
    }

    public List<String> predecessors(String nodeCode) {
        return edges.stream()
                .filter(edge -> edge.toNodeCode().equals(nodeCode))
                .map(CalculationEdge::fromNodeCode)
                .sorted()
                .toList();
    }

    private void validateMetadata() {
        if (currency.length() != 3) {
            throw invalid("币种必须是3位代码");
        }
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("计算模型失效时间必须晚于生效时间");
        }
    }

    private void validateGraph() {
        if (nodes.isEmpty() || nodes.size() > MAX_NODE_COUNT) {
            throw invalid("计算模型节点数量必须在1到" + MAX_NODE_COUNT + "之间");
        }
        Map<String, CalculationNode> nodeByCode = new LinkedHashMap<>();
        for (CalculationNode node : nodes) {
            if (nodeByCode.putIfAbsent(node.nodeCode(), node) != null) {
                throw invalid("计算节点编码不能重复: " + node.nodeCode());
            }
        }
        Set<String> edgeKeys = new HashSet<>();
        Map<String, Integer> indegrees = new HashMap<>();
        nodes.forEach(node -> indegrees.put(node.nodeCode(), 0));
        Map<String, List<String>> outgoing = new HashMap<>();
        for (CalculationEdge edge : edges) {
            if (!nodeByCode.containsKey(edge.fromNodeCode()) || !nodeByCode.containsKey(edge.toNodeCode())) {
                throw invalid("计算依赖引用不存在的节点");
            }
            if (!edgeKeys.add(edge.fromNodeCode() + "->" + edge.toNodeCode())) {
                throw invalid("计算依赖不能重复");
            }
            indegrees.compute(edge.toNodeCode(), (key, value) -> value + 1);
            outgoing.computeIfAbsent(edge.fromNodeCode(), key -> new ArrayList<>()).add(edge.toNodeCode());
        }
        validateNodeContracts(indegrees);
        ArrayDeque<String> ready = new ArrayDeque<>();
        indegrees.forEach((code, degree) -> {
            if (degree == 0) {
                ready.add(code);
            }
        });
        int visited = 0;
        while (!ready.isEmpty()) {
            String current = ready.remove();
            visited++;
            for (String target : outgoing.getOrDefault(current, List.of())) {
                int remaining = indegrees.compute(target, (key, value) -> value - 1);
                if (remaining == 0) {
                    ready.add(target);
                }
            }
        }
        if (visited != nodes.size()) {
            throw invalid("计算模型存在循环依赖");
        }
    }

    private void validateNodeContracts(Map<String, Integer> indegrees) {
        long standardInputs = nodes.stream()
                .filter(node -> node.nodeType() == CalculationNodeType.INPUT
                        && node.operator() == CalculationOperator.STANDARD_PREMIUM)
                .count();
        long outputs = nodes.stream().filter(node -> node.nodeType() == CalculationNodeType.OUTPUT).count();
        if (standardInputs != 1 || outputs != 1) {
            throw invalid("计算模型必须且只能包含一个标准保费输入和一个输出节点");
        }
        for (CalculationNode node : nodes) {
            int inbound = indegrees.get(node.nodeCode());
            if ((node.operator() == CalculationOperator.STANDARD_PREMIUM
                    || node.operator() == CalculationOperator.FIXED_AMOUNT) && inbound != 0) {
                throw invalid("标准保费和固定金额节点不能有前序依赖: " + node.nodeCode());
            }
            if ((node.operator() == CalculationOperator.PERCENTAGE_OF
                    || node.operator() == CalculationOperator.SUM) && inbound == 0) {
                throw invalid("比例和合计节点必须有前序依赖: " + node.nodeCode());
            }
            if (node.nodeType() == CalculationNodeType.OUTPUT
                    && (node.operator() != CalculationOperator.SUM || node.hasComponent())) {
                throw invalid("输出节点必须使用SUM且不能绑定费用项");
            }
            if (node.nodeType() != CalculationNodeType.OUTPUT && !node.hasComponent()) {
                throw invalid("非输出节点必须绑定费用项: " + node.nodeCode());
            }
        }
    }

    private String canonicalContent() {
        String nodeContent = nodes.stream()
                .sorted(Comparator.comparing(CalculationNode::nodeCode))
                .map(node -> String.join(":", node.nodeCode(), node.nodeName(), node.nodeType().name(),
                        node.operator().name(), nullable(node.componentCode()), nullable(node.componentVersion()),
                        nullable(node.parameterValue()), Integer.toString(node.executionOrder())))
                .collect(Collectors.joining(","));
        String edgeContent = edges.stream()
                .map(edge -> edge.fromNodeCode() + "->" + edge.toNodeCode())
                .sorted()
                .collect(Collectors.joining(","));
        return String.join("|", productId, modelCode, modelVersion, modelName, description, currency,
                effectiveFrom.toString(), nullable(effectiveTo), tenantId, nodeContent, edgeContent);
    }

    private void requireStatus(ActuarialDefinitionStatus expected, String message) {
        if (status != expected) {
            throw new PricingDomainException(ProductErrorCode.ACTUARIAL_MODEL_STATUS_INVALID, message);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + "不能为空");
        }
        return value.trim();
    }

    private static String nullable(Object value) {
        return value == null ? "*" : value.toString();
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, detail);
    }

    private static String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持SHA-256", exception);
        }
    }
}
