package com.anshun.dms.agent.tools;

import com.anshun.dms.agent.AgentTool;
import com.anshun.dms.agent.AgentToolContext;
import com.anshun.dms.agent.AgentToolArguments;
import com.anshun.dms.agent.AgentToolOutput;
import com.anshun.dms.service.LeaseService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GetLeaseContractDetailTool implements AgentTool {
    private final LeaseService leaseService;
    public GetLeaseContractDetailTool(LeaseService leaseService) { this.leaseService = leaseService; }

    @Override public String name() { return "get_lease_contract_detail"; }
    @Override public String description() { return "根据租赁合同记录 ID 查询单份合同的详细字段。仅在用户已提供或已查询到合同记录 ID 时调用。"; }
    @Override public String requiredPermission() { return "lease:view"; }
    @Override public Set<String> allowedArguments() { return Set.of("leaseId"); }
    @Override public Map<String, Object> parameterSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "leaseId", Map.of("type", "integer", "description", "租赁合同记录 ID")
        ), "required", List.of("leaseId"), "additionalProperties", false);
    }
    @Override public AgentToolOutput execute(JsonNode arguments, AgentToolContext context) {
        long leaseId = AgentToolArguments.requiredPositiveLong(arguments, "leaseId");
        return new AgentToolOutput(leaseService.detail(leaseId), "已查询租赁合同记录 " + leaseId + " 的详情");
    }
}
