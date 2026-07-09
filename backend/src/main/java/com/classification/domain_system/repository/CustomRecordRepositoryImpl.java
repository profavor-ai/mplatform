package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Record;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class CustomRecordRepositoryImpl implements CustomRecordRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<Record> findDynamicRecords(List<UUID> nodeIds, String status, Map<String, String> searchParams) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.* FROM record r " +
            "WHERE r.node_id IN (:nodeIds) " +
            "AND r.status NOT IN ('REJECTED', 'MISMATCHED') "
        );

        if (status != null && !status.isEmpty()) {
            sql.append(" AND r.status = :status ");
        }

        int paramIndex = 0;
        for (String key : searchParams.keySet()) {
            if (key.startsWith("op_") || key.endsWith("_max")) continue;
            
            String safeKey = key.replaceAll("[^a-zA-Z0-9_]", "_");
            String op = searchParams.getOrDefault("op_" + key, "EQ");
            
            if ("EQ".equals(op)) {
                sql.append(" AND (CAST(r.data AS jsonb) @> CAST(:searchValStr").append(paramIndex).append(" AS jsonb) ");
                sql.append(" OR CAST(r.data AS jsonb) @> CAST(:searchValNum").append(paramIndex).append(" AS jsonb)) ");
                paramIndex++;
            } else if ("BETWEEN".equals(op)) {
                sql.append(" AND NULLIF(r.data->>'").append(safeKey).append("', '') ~ '^[0-9]+(\\.[0-9]+)?$' ");
                sql.append(" AND CAST(NULLIF(r.data->>'").append(safeKey).append("', '') AS NUMERIC) BETWEEN :searchValMin").append(paramIndex).append(" AND :searchValMax").append(paramIndex).append(" ");
                paramIndex++;
            } else {
                String sqlOp = switch (op) {
                    case "GT" -> ">";
                    case "LT" -> "<";
                    case "GTE" -> ">=";
                    case "LTE" -> "<=";
                    default -> "=";
                };
                sql.append(" AND NULLIF(r.data->>'").append(safeKey).append("', '') ~ '^[0-9]+(\\.[0-9]+)?$' ");
                sql.append(" AND CAST(NULLIF(r.data->>'").append(safeKey).append("', '') AS NUMERIC) ").append(sqlOp).append(" :searchVal").append(paramIndex).append(" ");
                paramIndex++;
            }
        }

        Query query = entityManager.createNativeQuery(sql.toString(), Record.class);
        query.setParameter("nodeIds", nodeIds);

        if (status != null && !status.isEmpty()) {
            query.setParameter("status", status);
        }

        paramIndex = 0;
        for (String key : searchParams.keySet()) {
            if (key.startsWith("op_") || key.endsWith("_max")) continue;
            String op = searchParams.getOrDefault("op_" + key, "EQ");
            String val = searchParams.get(key);
            String safeKey = key.replaceAll("[^a-zA-Z0-9_]", "_");
            
            if ("EQ".equals(op)) {
                query.setParameter("searchValStr" + paramIndex, "{\"" + safeKey + "\": \"" + val.replace("\"", "\\\"") + "\"}");
                if (val.matches("-?\\d+(\\.\\d+)?")) {
                    query.setParameter("searchValNum" + paramIndex, "{\"" + safeKey + "\": " + val + "}");
                } else if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                    query.setParameter("searchValNum" + paramIndex, "{\"" + safeKey + "\": " + val.toLowerCase() + "}");
                } else {
                    query.setParameter("searchValNum" + paramIndex, "{\"" + safeKey + "\": null}");
                }
            } else if ("BETWEEN".equals(op)) {
                String maxVal = searchParams.get(key + "_max");
                query.setParameter("searchValMin" + paramIndex, Double.parseDouble(val));
                query.setParameter("searchValMax" + paramIndex, Double.parseDouble(maxVal != null && !maxVal.isEmpty() ? maxVal : val));
            } else {
                query.setParameter("searchVal" + paramIndex, Double.parseDouble(val));
            }
            paramIndex++;
        }

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Record> findDynamicRecordsByDomain(UUID domainId, Map<String, String> searchParams) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.* FROM record r " +
            "JOIN classification_node n ON r.node_id = n.id " +
            "WHERE n.domain_id = :domainId " +
            "AND r.status NOT IN ('REJECTED', 'MISMATCHED') "
        );

        int paramIndex = 0;
        for (String key : searchParams.keySet()) {
            if (key.startsWith("op_") || key.endsWith("_max")) continue;
            
            String safeKey = key.replaceAll("[^a-zA-Z0-9_]", "_");
            String op = searchParams.getOrDefault("op_" + key, "EQ");
            
            if ("EQ".equals(op)) {
                sql.append(" AND (CAST(r.data AS jsonb) @> CAST(:searchValStr").append(paramIndex).append(" AS jsonb) ");
                sql.append(" OR CAST(r.data AS jsonb) @> CAST(:searchValNum").append(paramIndex).append(" AS jsonb)) ");
                paramIndex++;
            } else if ("BETWEEN".equals(op)) {
                sql.append(" AND NULLIF(r.data->>'").append(safeKey).append("', '') ~ '^[0-9]+(\\.[0-9]+)?$' ");
                sql.append(" AND CAST(NULLIF(r.data->>'").append(safeKey).append("', '') AS NUMERIC) BETWEEN :searchValMin").append(paramIndex).append(" AND :searchValMax").append(paramIndex).append(" ");
                paramIndex++;
            } else {
                String sqlOp = switch (op) {
                    case "GT" -> ">";
                    case "LT" -> "<";
                    case "GTE" -> ">=";
                    case "LTE" -> "<=";
                    default -> "=";
                };
                sql.append(" AND NULLIF(r.data->>'").append(safeKey).append("', '') ~ '^[0-9]+(\\.[0-9]+)?$' ");
                sql.append(" AND CAST(NULLIF(r.data->>'").append(safeKey).append("', '') AS NUMERIC) ").append(sqlOp).append(" :searchVal").append(paramIndex).append(" ");
                paramIndex++;
            }
        }

        Query query = entityManager.createNativeQuery(sql.toString(), Record.class);
        query.setParameter("domainId", domainId);

        paramIndex = 0;
        for (String key : searchParams.keySet()) {
            if (key.startsWith("op_") || key.endsWith("_max")) continue;
            String op = searchParams.getOrDefault("op_" + key, "EQ");
            String val = searchParams.get(key);
            String safeKey = key.replaceAll("[^a-zA-Z0-9_]", "_");
            
            if ("EQ".equals(op)) {
                query.setParameter("searchValStr" + paramIndex, "{\"" + safeKey + "\": \"" + val.replace("\"", "\\\"") + "\"}");
                if (val.matches("-?\\d+(\\.\\d+)?")) {
                    query.setParameter("searchValNum" + paramIndex, "{\"" + safeKey + "\": " + val + "}");
                } else if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                    query.setParameter("searchValNum" + paramIndex, "{\"" + safeKey + "\": " + val.toLowerCase() + "}");
                } else {
                    query.setParameter("searchValNum" + paramIndex, "{\"" + safeKey + "\": null}");
                }
            } else if ("BETWEEN".equals(op)) {
                String maxVal = searchParams.get(key + "_max");
                query.setParameter("searchValMin" + paramIndex, Double.parseDouble(val));
                query.setParameter("searchValMax" + paramIndex, Double.parseDouble(maxVal != null && !maxVal.isEmpty() ? maxVal : val));
            } else {
                query.setParameter("searchVal" + paramIndex, Double.parseDouble(val));
            }
            paramIndex++;
        }

        return query.getResultList();
    }
}
