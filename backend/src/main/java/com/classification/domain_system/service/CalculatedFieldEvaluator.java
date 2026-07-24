package com.classification.domain_system.service;

import com.classification.domain_system.entity.FieldDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class CalculatedFieldEvaluator {

    private final FieldDefinitionService fieldDefinitionService;
    private final ObjectMapper mapper = new ObjectMapper();

    public String recomputeCalculatedFields(java.util.UUID nodeId, String dataJson) {
        if (dataJson == null || dataJson.isBlank()) return dataJson;
        try {
            Map<String, Object> data = mapper.readValue(dataJson, new TypeReference<Map<String, Object>>() {});
            List<FieldDefinition> fields = fieldDefinitionService.getEffectiveFields(nodeId);

            for (FieldDefinition field : fields) {
                if ("CALCULATED".equals(field.getType()) && field.getOptions() != null) {
                    try {
                        JsonNode opts = mapper.readTree(field.getOptions());
                        if (opts.has("formula")) {
                            String formula = opts.get("formula").asText();
                            Double result = evaluateFormula(formula, data);
                            if (result != null && !result.isNaN() && !result.isInfinite()) {
                                data.put(field.getKey(), result);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Failed to calculate formula for field: {}", field.getKey(), e);
                    }
                }
            }

            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return dataJson;
        }
    }

    public Double evaluateFormula(String formula, Map<String, Object> data) {
        try {
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
            Matcher matcher = pattern.matcher(formula);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String key = matcher.group(1);
                if (!data.containsKey(key)) {
                    log.warn("Referenced key '{}' in formula '{}' not found in record data", key, formula);
                }
                Object val = data.get(key);
                double numVal = 0;
                if (val instanceof Number) {
                    numVal = ((Number) val).doubleValue();
                } else if (val != null) {
                    try { numVal = Double.parseDouble(val.toString()); } catch (Exception e) { numVal = 0; }
                }
                matcher.appendReplacement(sb, String.valueOf(numVal));
            }
            matcher.appendTail(sb);

            return evalExpr(sb.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public double evalExpr(String expr) {
        return new Object() {
            int pos = 0;

            double parse() {
                return parseAddSub();
            }

            double parseAddSub() {
                skipSpaces();
                double left = parseMulDiv();
                skipSpaces();
                while (pos < expr.length()) {
                    char op = expr.charAt(pos);
                    if (op == '+' || op == '-') {
                        pos++;
                        skipSpaces();
                        double right = parseMulDiv();
                        left = op == '+' ? left + right : left - right;
                        skipSpaces();
                    } else break;
                }
                return left;
            }

            double parseMulDiv() {
                skipSpaces();
                double left = parseUnary();
                skipSpaces();
                while (pos < expr.length()) {
                    char op = expr.charAt(pos);
                    if (op == '*' || op == '/') {
                        pos++;
                        skipSpaces();
                        double right = parseUnary();
                        left = op == '*' ? left * right : left / right;
                        skipSpaces();
                    } else break;
                }
                return left;
            }

            double parseUnary() {
                skipSpaces();
                if (pos < expr.length() && expr.charAt(pos) == '-') {
                    pos++;
                    return -parseUnary();
                }
                return parsePrimary();
            }

            double parsePrimary() {
                skipSpaces();
                for (String fn : new String[]{"CEIL", "FLOOR", "ROUND", "ABS"}) {
                    if (pos + fn.length() <= expr.length() && expr.substring(pos, pos + fn.length()).equals(fn)) {
                        pos += fn.length();
                        skipSpaces();
                        if (pos < expr.length() && expr.charAt(pos) == '(') {
                            pos++;
                            double val = parseAddSub();
                            double decimals = 0;
                            skipSpaces();
                            if (pos < expr.length() && expr.charAt(pos) == ',') {
                                pos++;
                                decimals = parseAddSub();
                            }
                            skipSpaces();
                            if (pos < expr.length() && expr.charAt(pos) == ')') pos++;
                            switch (fn) {
                                case "CEIL": return Math.ceil(val);
                                case "FLOOR": return Math.floor(val);
                                case "ABS": return Math.abs(val);
                                case "ROUND": {
                                    double factor = Math.pow(10, decimals);
                                    return Math.round(val * factor) / factor;
                                }
                                default: return val;
                            }
                        }
                    }
                }
                if (pos < expr.length() && expr.charAt(pos) == '(') {
                    pos++;
                    double val = parseAddSub();
                    skipSpaces();
                    if (pos < expr.length() && expr.charAt(pos) == ')') pos++;
                    return val;
                }
                int start = pos;
                while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
                    pos++;
                }
                if (start == pos) return 0;
                return Double.parseDouble(expr.substring(start, pos));
            }

            void skipSpaces() {
                while (pos < expr.length() && expr.charAt(pos) == ' ') pos++;
            }
        }.parse();
    }
}
