package com.anshun.dms.agent;

import com.anshun.dms.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Strict argument checks sit between model-generated JSON and business services. */
public final class AgentToolArguments {
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private AgentToolArguments() { }

    public static void validateAllowedFields(JsonNode arguments, Set<String> allowed) {
        if (arguments == null || !arguments.isObject()) throw BusinessException.badRequest("工具参数必须是 JSON 对象");
        arguments.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) throw BusinessException.badRequest("工具参数包含未授权字段：" + name);
        });
    }

    public static String optionalText(JsonNode arguments, String field, int maxLength) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw BusinessException.badRequest("工具参数 " + field + " 必须是文本");
        String text = value.asText().trim();
        if (!StringUtils.hasText(text)) return null;
        if (text.length() > maxLength) throw BusinessException.badRequest("工具参数 " + field + " 过长");
        return text;
    }

    public static String requiredIdentifier(JsonNode arguments, String field) {
        String value = optionalText(arguments, field, 64);
        if (!StringUtils.hasText(value) || !IDENTIFIER.matcher(value).matches()) {
            throw BusinessException.badRequest("工具参数 " + field + " 格式不合法");
        }
        return value;
    }

    public static long requiredPositiveLong(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.canConvertToLong() || value.asLong() < 1) {
            throw BusinessException.badRequest("工具参数 " + field + " 必须是正整数");
        }
        return value.asLong();
    }

    public static int requiredPositiveInt(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.canConvertToInt() || value.asInt() < 1) {
            throw BusinessException.badRequest("工具参数 " + field + " 必须是正整数");
        }
        return value.asInt();
    }

    public static BigDecimal optionalDecimal(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isNumber() && !value.isTextual()) throw BusinessException.badRequest("工具参数 " + field + " 必须是数字");
        try {
            BigDecimal decimal = new BigDecimal(value.asText());
            if (decimal.precision() > 15 || decimal.scale() > 8) throw BusinessException.badRequest("工具参数 " + field + " 精度过高");
            return decimal;
        } catch (NumberFormatException exception) {
            throw BusinessException.badRequest("工具参数 " + field + " 必须是数字");
        }
    }

    public static int limit(JsonNode arguments, int defaultValue, int maxValue) {
        JsonNode value = arguments.get("limit");
        if (value == null || value.isNull()) return defaultValue;
        if (!value.canConvertToInt() || value.asInt() < 1 || value.asInt() > maxValue) {
            throw BusinessException.badRequest("工具参数 limit 必须在 1 到 " + maxValue + " 之间");
        }
        return value.asInt();
    }

    public static String enumValue(JsonNode arguments, String field, Set<String> values) {
        String value = optionalText(arguments, field, 32);
        if (value == null) return null;
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!values.contains(normalized)) throw BusinessException.badRequest("工具参数 " + field + " 不合法");
        return normalized;
    }
}
