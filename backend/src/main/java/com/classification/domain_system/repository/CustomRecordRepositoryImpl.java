package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Record;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.math.BigInteger;

@Repository
public class CustomRecordRepositoryImpl implements CustomRecordRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Record> findDynamicRecords(List<UUID> nodeIds, String status, Map<String, String> searchParams, Pageable pageable) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.* FROM record r " +
            "WHERE r.node_id IN (:nodeIds) " +
            "AND r.status NOT IN ('REJECTED', 'MISMATCHED') "
        );
        StringBuilder countSql = new StringBuilder(
            "SELECT COUNT(*) FROM record r " +
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
                String cond = " AND (CAST(r.data AS jsonb) @> CAST(:searchValStr" + paramIndex + " AS jsonb) " +
                              " OR CAST(r.data AS jsonb) @> CAST(:searchValStrLower" + paramIndex + " AS jsonb) " +
                              " OR CAST(r.data AS jsonb) @> CAST(:searchValNum" + paramIndex + " AS jsonb) " +
                              " OR CAST(r.data AS jsonb) @> CAST(:searchValNumLower" + paramIndex + " AS jsonb)) ";
                sql.append(cond);
                countSql.append(cond);
                paramIndex++;
            } else if ("BETWEEN".equals(op)) {
                String cond = " AND ( (NULLIF(r.data->>'" + safeKey + "', '') ~ '^[0-9]+(\\.[0-9]+)?$' " +
                              " AND CAST(NULLIF(r.data->>'" + safeKey + "', '') AS NUMERIC) BETWEEN :searchValMin" + paramIndex + " AND :searchValMax" + paramIndex + ") " +
                              " OR (NULLIF(r.data->>'" + safeKey.toLowerCase() + "', '') ~ '^[0-9]+(\\.[0-9]+)?$' " +
                              " AND CAST(NULLIF(r.data->>'" + safeKey.toLowerCase() + "', '') AS NUMERIC) BETWEEN :searchValMin" + paramIndex + " AND :searchValMax" + paramIndex + ") ) ";
                sql.append(cond);
                countSql.append(cond);
                paramIndex++;
            } else {
                String sqlOp = switch (op) {
                    case "GT" -> ">";
                    case "LT" -> "<";
                    case "GTE" -> ">=";
                    case "LTE" -> "<=";
                    default -> "=";
                };
                String cond = " AND ( (NULLIF(r.data->>'" + safeKey + "', '') ~ '^[0-9]+(\\.[0-9]+)?$' " +
                              " AND CAST(NULLIF(r.data->>'" + safeKey + "', '') AS NUMERIC) " + sqlOp + " :searchVal" + paramIndex + ") " +
                              " OR (NULLIF(r.data->>'" + safeKey.toLowerCase() + "', '') ~ '^[0-9]+(\\.[0-9]+)?$' " +
                              " AND CAST(NULLIF(r.data->>'" + safeKey.toLowerCase() + "', '') AS NUMERIC) " + sqlOp + " :searchVal" + paramIndex + ") ) ";
                sql.append(cond);
                countSql.append(cond);
                paramIndex++;
            }
        }

        if (pageable.isPaged()) {
            sql.append(" LIMIT :limit OFFSET :offset");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), Record.class);
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        
        query.setParameter("nodeIds", nodeIds);
        countQuery.setParameter("nodeIds", nodeIds);

        if (status != null && !status.isEmpty()) {
            query.setParameter("status", status);
            countQuery.setParameter("status", status);
        }

        paramIndex = 0;
        for (String key : searchParams.keySet()) {
            if (key.startsWith("op_") || key.endsWith("_max")) continue;
            String op = searchParams.getOrDefault("op_" + key, "EQ");
            String val = searchParams.get(key);
            String safeKey = key.replaceAll("[^a-zA-Z0-9_]", "_");
            
            if ("EQ".equals(op)) {
                String strVal = "{\"" + safeKey + "\": \"" + val.replace("\"", "\\\"") + "\"}";
                String strValLower = "{\"" + safeKey.toLowerCase() + "\": \"" + val.replace("\"", "\\\"") + "\"}";
                query.setParameter("searchValStr" + paramIndex, strVal);
                countQuery.setParameter("searchValStr" + paramIndex, strVal);
                query.setParameter("searchValStrLower" + paramIndex, strValLower);
                countQuery.setParameter("searchValStrLower" + paramIndex, strValLower);
                if (val.matches("-?(0|[1-9]\\d*)(\\.\\d+)?")) {
                    String numVal = "{\"" + safeKey + "\": " + val + "}";
                    String numValLower = "{\"" + safeKey.toLowerCase() + "\": " + val + "}";
                    query.setParameter("searchValNum" + paramIndex, numVal);
                    countQuery.setParameter("searchValNum" + paramIndex, numVal);
                    query.setParameter("searchValNumLower" + paramIndex, numValLower);
                    countQuery.setParameter("searchValNumLower" + paramIndex, numValLower);
                } else if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                    String numVal = "{\"" + safeKey + "\": " + val.toLowerCase() + "}";
                    String numValLower = "{\"" + safeKey.toLowerCase() + "\": " + val.toLowerCase() + "}";
                    query.setParameter("searchValNum" + paramIndex, numVal);
                    countQuery.setParameter("searchValNum" + paramIndex, numVal);
                    query.setParameter("searchValNumLower" + paramIndex, numValLower);
                    countQuery.setParameter("searchValNumLower" + paramIndex, numValLower);
                } else {
                    String numVal = "{\"" + safeKey + "\": null}";
                    String numValLower = "{\"" + safeKey.toLowerCase() + "\": null}";
                    query.setParameter("searchValNum" + paramIndex, numVal);
                    countQuery.setParameter("searchValNum" + paramIndex, numVal);
                    query.setParameter("searchValNumLower" + paramIndex, numValLower);
                    countQuery.setParameter("searchValNumLower" + paramIndex, numValLower);
                }
            } else if ("BETWEEN".equals(op)) {
                String maxVal = searchParams.get(key + "_max");
                double vMin = Double.parseDouble(val);
                double vMax = Double.parseDouble(maxVal != null && !maxVal.isEmpty() ? maxVal : val);
                query.setParameter("searchValMin" + paramIndex, vMin);
                countQuery.setParameter("searchValMin" + paramIndex, vMin);
                query.setParameter("searchValMax" + paramIndex, vMax);
                countQuery.setParameter("searchValMax" + paramIndex, vMax);
            } else {
                double sVal = Double.parseDouble(val);
                query.setParameter("searchVal" + paramIndex, sVal);
                countQuery.setParameter("searchVal" + paramIndex, sVal);
            }
            paramIndex++;
        }

        long total = ((Number) countQuery.getSingleResult()).longValue();

        if (pageable.isPaged()) {
            query.setParameter("limit", pageable.getPageSize());
            query.setParameter("offset", pageable.getOffset());
        }

        List<Record> results = query.getResultList();
        return new PageImpl<>(results, pageable, total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Record> findDynamicRecordsByDomain(UUID domainId, Map<String, String> searchParams, Pageable pageable) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.* FROM record r " +
            "JOIN classification_node n ON r.node_id = n.id " +
            "WHERE n.domain_id = :domainId " +
            "AND r.status NOT IN ('REJECTED', 'MISMATCHED') "
        );
        StringBuilder countSql = new StringBuilder(
            "SELECT COUNT(*) FROM record r " +
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
                String cond = " AND (CAST(r.data AS jsonb) @> CAST(:searchValStr" + paramIndex + " AS jsonb) " +
                              " OR CAST(r.data AS jsonb) @> CAST(:searchValStrLower" + paramIndex + " AS jsonb) " +
                              " OR CAST(r.data AS jsonb) @> CAST(:searchValNum" + paramIndex + " AS jsonb) " +
                              " OR CAST(r.data AS jsonb) @> CAST(:searchValNumLower" + paramIndex + " AS jsonb)) ";
                sql.append(cond);
                countSql.append(cond);
                paramIndex++;
            } else if ("BETWEEN".equals(op)) {
                String cond = " AND ( (NULLIF(r.data->>'" + safeKey + "', '') ~ '^[0-9]+(\\.[0-9]+)?$' " +
                              " AND CAST(NULLIF(r.data->>'" + safeKey + "', '') AS NUMERIC) BETWEEN :searchValMin" + paramIndex + " AND :searchValMax" + paramIndex + ") " +
                              " OR (NULLIF(r.data->>'" + safeKey.toLowerCase() + "', '') ~ '^[0-9]+(\\.[0-9]+)?$' " +
                              " AND CAST(NULLIF(r.data->>'" + safeKey.toLowerCase() + "', '') AS NUMERIC) BETWEEN :searchValMin" + paramIndex + " AND :searchValMax" + paramIndex + ") ) ";
                sql.append(cond);
                countSql.append(cond);
                paramIndex++;
            } else {
                String sqlOp = switch (op) {
                    case "GT" -> ">";
                    case "LT" -> "<";
                    case "GTE" -> ">=";
                    case "LTE" -> "<=";
                    default -> "=";
                };
                String cond = " AND ( (NULLIF(r.data->>'" + safeKey + "', '') ~ '^[0-9]+(\\.[0-9]+)?$' " +
                              " AND CAST(NULLIF(r.data->>'" + safeKey + "', '') AS NUMERIC) " + sqlOp + " :searchVal" + paramIndex + ") " +
                              " OR (NULLIF(r.data->>'" + safeKey.toLowerCase() + "', '') ~ '^[0-9]+(\\.[0-9]+)?$' " +
                              " AND CAST(NULLIF(r.data->>'" + safeKey.toLowerCase() + "', '') AS NUMERIC) " + sqlOp + " :searchVal" + paramIndex + ") ) ";
                sql.append(cond);
                countSql.append(cond);
                paramIndex++;
            }
        }

        if (pageable.isPaged()) {
            sql.append(" LIMIT :limit OFFSET :offset");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), Record.class);
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        
        query.setParameter("domainId", domainId);
        countQuery.setParameter("domainId", domainId);

        paramIndex = 0;
        for (String key : searchParams.keySet()) {
            if (key.startsWith("op_") || key.endsWith("_max")) continue;
            String op = searchParams.getOrDefault("op_" + key, "EQ");
            String val = searchParams.get(key);
            String safeKey = key.replaceAll("[^a-zA-Z0-9_]", "_");
            
            if ("EQ".equals(op)) {
                String strVal = "{\"" + safeKey + "\": \"" + val.replace("\"", "\\\"") + "\"}";
                String strValLower = "{\"" + safeKey.toLowerCase() + "\": \"" + val.replace("\"", "\\\"") + "\"}";
                query.setParameter("searchValStr" + paramIndex, strVal);
                countQuery.setParameter("searchValStr" + paramIndex, strVal);
                query.setParameter("searchValStrLower" + paramIndex, strValLower);
                countQuery.setParameter("searchValStrLower" + paramIndex, strValLower);
                if (val.matches("-?(0|[1-9]\\d*)(\\.\\d+)?")) {
                    String numVal = "{\"" + safeKey + "\": " + val + "}";
                    String numValLower = "{\"" + safeKey.toLowerCase() + "\": " + val + "}";
                    query.setParameter("searchValNum" + paramIndex, numVal);
                    countQuery.setParameter("searchValNum" + paramIndex, numVal);
                    query.setParameter("searchValNumLower" + paramIndex, numValLower);
                    countQuery.setParameter("searchValNumLower" + paramIndex, numValLower);
                } else if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                    String numVal = "{\"" + safeKey + "\": " + val.toLowerCase() + "}";
                    String numValLower = "{\"" + safeKey.toLowerCase() + "\": " + val.toLowerCase() + "}";
                    query.setParameter("searchValNum" + paramIndex, numVal);
                    countQuery.setParameter("searchValNum" + paramIndex, numVal);
                    query.setParameter("searchValNumLower" + paramIndex, numValLower);
                    countQuery.setParameter("searchValNumLower" + paramIndex, numValLower);
                } else {
                    String numVal = "{\"" + safeKey + "\": null}";
                    String numValLower = "{\"" + safeKey.toLowerCase() + "\": null}";
                    query.setParameter("searchValNum" + paramIndex, numVal);
                    countQuery.setParameter("searchValNum" + paramIndex, numVal);
                    query.setParameter("searchValNumLower" + paramIndex, numValLower);
                    countQuery.setParameter("searchValNumLower" + paramIndex, numValLower);
                }
            } else if ("BETWEEN".equals(op)) {
                String maxVal = searchParams.get(key + "_max");
                double vMin = Double.parseDouble(val);
                double vMax = Double.parseDouble(maxVal != null && !maxVal.isEmpty() ? maxVal : val);
                query.setParameter("searchValMin" + paramIndex, vMin);
                countQuery.setParameter("searchValMin" + paramIndex, vMin);
                query.setParameter("searchValMax" + paramIndex, vMax);
                countQuery.setParameter("searchValMax" + paramIndex, vMax);
            } else {
                double sVal = Double.parseDouble(val);
                query.setParameter("searchVal" + paramIndex, sVal);
                countQuery.setParameter("searchVal" + paramIndex, sVal);
            }
            paramIndex++;
        }

        long total = ((Number) countQuery.getSingleResult()).longValue();

        if (pageable.isPaged()) {
            query.setParameter("limit", pageable.getPageSize());
            query.setParameter("offset", pageable.getOffset());
        }

        List<Record> results = query.getResultList();
        return new PageImpl<>(results, pageable, total);
    }
}
