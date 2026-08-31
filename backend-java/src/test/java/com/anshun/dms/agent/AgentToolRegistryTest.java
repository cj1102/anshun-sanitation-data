package com.anshun.dms.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolRegistryTest {
    private final AgentToolRegistry registry = new AgentToolRegistry(List.of(new SampleTool()), new ObjectMapper());
    private final Authentication allowedUser = new UsernamePasswordAuthenticationToken("tester", "n/a",
            List.of(new SimpleGrantedAuthority("stats:view")));

    @Test
    void onlyAuthorizedToolsAreExposedToTheModel() {
        assertThat(registry.definitions(allowedUser)).hasSize(1);
        assertThat(registry.definitions(allowedUser).get(0).get("type")).isEqualTo("function");

        Authentication deniedUser = new UsernamePasswordAuthenticationToken("viewer", "n/a",
                List.of(new SimpleGrantedAuthority("position:view")));
        assertThat(registry.definitions(deniedUser)).isEmpty();
    }

    @Test
    void rejectsUndeclaredModelArgumentsBeforeToolExecution() {
        AgentToolExecution result = registry.execute("sample_tool", "{\"unexpected\":true}", allowedUser, context());

        assertThat(result.display().success()).isFalse();
        assertThat(result.errorMessage()).contains("未授权字段");
    }

    @Test
    void rechecksPermissionWhenTheModelAttemptsExecution() {
        Authentication deniedUser = new UsernamePasswordAuthenticationToken("viewer", "n/a",
                List.of(new SimpleGrantedAuthority("position:view")));

        AgentToolExecution result = registry.execute("sample_tool", "{}", deniedUser, context());

        assertThat(result.display().success()).isFalse();
        assertThat(result.errorMessage()).contains("无权");
    }

    @Test
    void returnsStructuredToolDataToTheModel() {
        AgentToolExecution result = registry.execute("sample_tool", "{}", allowedUser, context());

        assertThat(result.display().success()).isTrue();
        assertThat(registry.serializeForModel(result)).contains("\"ok\":true", "\"count\":1");
    }

    @Test
    void readOnlyModeBlocksWriteToolsEvenWhenBusinessPermissionExists() {
        AgentToolRegistry writeRegistry = new AgentToolRegistry(List.of(new WriteTool()), new ObjectMapper());
        Authentication evaluationUser = new UsernamePasswordAuthenticationToken("admin", "n/a", List.of(
                new SimpleGrantedAuthority("position:create"),
                new SimpleGrantedAuthority(AgentToolRegistry.READ_ONLY_AUTHORITY)));

        assertThat(writeRegistry.definitions(evaluationUser)).isEmpty();
        assertThat(writeRegistry.execute("write_tool", "{}", evaluationUser, context()).display().success()).isFalse();
    }

    private static class SampleTool implements AgentTool {
        @Override public String name() { return "sample_tool"; }
        @Override public String description() { return "用于测试的只读工具"; }
        @Override public String requiredPermission() { return "stats:view"; }
        @Override public Set<String> allowedArguments() { return Set.of(); }
        @Override public Map<String, Object> parameterSchema() {
            return Map.of("type", "object", "properties", Map.of(), "additionalProperties", false);
        }
        @Override public AgentToolOutput execute(JsonNode arguments, AgentToolContext context) {
            return new AgentToolOutput(Map.of("count", 1), "已完成示例查询");
        }
    }

    private static final class WriteTool extends SampleTool {
        @Override public String name() { return "write_tool"; }
        @Override public String requiredPermission() { return "position:create"; }
        @Override public ToolSideEffect sideEffect() { return ToolSideEffect.WRITE; }
    }

    private AgentToolContext context() { return new AgentToolContext("tester", 1L, "/dashboard"); }
}
