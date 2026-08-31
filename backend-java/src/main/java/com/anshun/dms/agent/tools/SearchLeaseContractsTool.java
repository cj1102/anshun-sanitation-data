package com.anshun.dms.agent.tools;

import com.anshun.dms.agent.AgentTool;
import com.anshun.dms.agent.AgentToolContext;
import com.anshun.dms.agent.AgentToolArguments;
import com.anshun.dms.agent.AgentToolOutput;
import com.anshun.dms.dto.LeasePageQuery;
import com.anshun.dms.service.LeaseService;
import com.anshun.dms.vo.LeaseVO;
import com.anshun.dms.vo.PageData;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SearchLeaseContractsTool implements AgentTool {
    private final LeaseService leaseService;
    public SearchLeaseContractsTool(LeaseService leaseService) { this.leaseService = leaseService; }

    @Override public String name() { return "search_lease_contracts"; }
    @Override public String description() { return "按合同编号、广告点位编码、点位位置或承租单位查询租赁合同，并返回审批状态。仅返回合同摘要，不执行任何修改。"; }
    @Override public String requiredPermission() { return "lease:view"; }
    @Override public Set<String> allowedArguments() { return Set.of("search", "lesseeCompany", "limit"); }
    @Override public Map<String, Object> parameterSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "search", Map.of("type", "string", "description", "合同编号、点位编码或位置关键字"),
                "lesseeCompany", Map.of("type", "string", "description", "承租单位名称关键字"),
                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 10, "description", "返回数量，默认 5")
        ), "additionalProperties", false);
    }
    @Override public AgentToolOutput execute(JsonNode arguments, AgentToolContext context) {
        LeasePageQuery query = new LeasePageQuery();
        query.setPage(1);
        query.setLimit(AgentToolArguments.limit(arguments, 5, 10));
        query.setSearch(AgentToolArguments.optionalText(arguments, "search", 60));
        query.setLesseeCompany(AgentToolArguments.optionalText(arguments, "lesseeCompany", 80));
        PageData<LeaseVO> page = leaseService.page(query);
        List<Map<String, Object>> items = page.data().stream().map(this::summary).toList();
        return new AgentToolOutput(Map.of("total", page.total(), "items", items), "已查询到 " + page.total() + " 份匹配租赁合同，返回前 " + items.size() + " 条");
    }

    private Map<String, Object> summary(LeaseVO lease) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("adLeaseId", lease.adLeaseId()); item.put("contractCode", lease.contractCode());
        item.put("adPositionCode", lease.adPositionCode()); item.put("adLocation", lease.adLocation());
        item.put("lesseeCompany", lease.lesseeCompany()); item.put("leaseRent", lease.leaseRent());
        item.put("leaseStartDate", lease.leaseStartDate()); item.put("leaseEndDate", lease.leaseEndDate());
        item.put("approvalStatus", lease.approvalStatus());
        return item;
    }
}
