package com.classification.domain_system.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcDynamicExecutionService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void executeUpsert(String configJson, String payloadJson) throws Exception {
        JsonNode config = objectMapper.readTree(configJson);
        String url = config.get("url").asText();
        String user = config.get("user").asText();
        String password = config.get("password").asText();
        String table = config.get("table").asText();

        Map<String, Object> data = objectMapper.readValue(payloadJson, new TypeReference<>() {});
        if (data == null || data.isEmpty()) {
            log.warn("Payload is empty, skipping JDBC execution");
            return;
        }

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet pkRs = metaData.getPrimaryKeys(null, null, table);
            List<String> pkColumns = new ArrayList<>();
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }

            String dbProductName = metaData.getDatabaseProductName().toLowerCase();
            List<String> columns = new ArrayList<>(data.keySet());
            List<Object> values = new ArrayList<>();
            for (String col : columns) {
                values.add(data.get(col));
            }

            String sql;
            if (dbProductName.contains("mysql") || dbProductName.contains("mariadb")) {
                sql = buildMySqlUpsert(table, columns);
            } else if (dbProductName.contains("postgresql")) {
                sql = buildPostgresUpsert(table, columns, pkColumns);
            } else if (dbProductName.contains("oracle")) {
                sql = buildOracleMerge(table, columns, pkColumns);
            } else {
                throw new UnsupportedOperationException("Upsert not supported for database: " + dbProductName);
            }

            log.info("Executing JDBC Upsert: {}", sql);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (dbProductName.contains("mysql") || dbProductName.contains("mariadb")) {
                    int idx = 1;
                    for (Object val : values) {
                        pstmt.setObject(idx++, val);
                    }
                } else if (dbProductName.contains("postgresql")) {
                    int idx = 1;
                    for (Object val : values) {
                        pstmt.setObject(idx++, val);
                    }
                } else if (dbProductName.contains("oracle")) {
                    int idx = 1;
                    for (Object val : values) {
                        pstmt.setObject(idx++, val);
                    }
                }

                int affectedRows = pstmt.executeUpdate();
                log.info("JDBC Upsert completed. Affected rows: {}", affectedRows);
            }
        }
    }

    private String buildMySqlUpsert(String table, List<String> columns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(table).append(" (");
        
        StringBuilder values = new StringBuilder("VALUES (");
        StringBuilder update = new StringBuilder("ON DUPLICATE KEY UPDATE ");

        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            sql.append(col);
            values.append("?");
            update.append(col).append("=VALUES(").append(col).append(")");

            if (i < columns.size() - 1) {
                sql.append(", ");
                values.append(", ");
                update.append(", ");
            }
        }
        sql.append(") ");
        values.append(") ");

        return sql.append(values).append(update).toString();
    }

    private String buildPostgresUpsert(String table, List<String> columns, List<String> pkColumns) {
        if (pkColumns.isEmpty()) {
            throw new IllegalStateException("Primary key is required for PostgreSQL upsert on table: " + table);
        }

        StringBuilder sql = new StringBuilder("INSERT INTO \"");
        sql.append(table).append("\" (");

        StringBuilder values = new StringBuilder("VALUES (");
        StringBuilder conflict = new StringBuilder("ON CONFLICT (");
        
        for (int i = 0; i < pkColumns.size(); i++) {
            conflict.append("\"").append(pkColumns.get(i)).append("\"");
            if (i < pkColumns.size() - 1) conflict.append(", ");
        }
        conflict.append(") DO UPDATE SET ");

        boolean firstUpdate = true;
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            sql.append("\"").append(col).append("\"");
            values.append("?");
            
            if (!pkColumns.contains(col)) {
                if (!firstUpdate) conflict.append(", ");
                conflict.append("\"").append(col).append("\" = EXCLUDED.\"").append(col).append("\"");
                firstUpdate = false;
            }

            if (i < columns.size() - 1) {
                sql.append(", ");
                values.append(", ");
            }
        }
        
        if (firstUpdate) {
            conflict = new StringBuilder("ON CONFLICT (");
            for (int i = 0; i < pkColumns.size(); i++) {
                conflict.append("\"").append(pkColumns.get(i)).append("\"");
                if (i < pkColumns.size() - 1) conflict.append(", ");
            }
            conflict.append(") DO NOTHING");
        }

        sql.append(") ");
        values.append(") ");

        return sql.append(values).append(conflict).toString();
    }

    private String buildOracleMerge(String table, List<String> columns, List<String> pkColumns) {
        if (pkColumns.isEmpty()) {
            throw new IllegalStateException("Primary key is required for Oracle merge on table: " + table);
        }

        StringBuilder sql = new StringBuilder("MERGE INTO ");
        sql.append(table).append(" t USING (SELECT ");
        
        for (int i = 0; i < columns.size(); i++) {
            sql.append("? AS ").append(columns.get(i));
            if (i < columns.size() - 1) sql.append(", ");
        }
        sql.append(" FROM DUAL) s ON (");

        for (int i = 0; i < pkColumns.size(); i++) {
            String pk = pkColumns.get(i);
            sql.append("t.").append(pk).append(" = s.").append(pk);
            if (i < pkColumns.size() - 1) sql.append(" AND ");
        }
        sql.append(") ");

        List<String> nonPkColumns = new ArrayList<>();
        for (String col : columns) {
            if (!pkColumns.contains(col)) {
                nonPkColumns.add(col);
            }
        }

        if (!nonPkColumns.isEmpty()) {
            sql.append("WHEN MATCHED THEN UPDATE SET ");
            for (int i = 0; i < nonPkColumns.size(); i++) {
                String col = nonPkColumns.get(i);
                sql.append("t.").append(col).append(" = s.").append(col);
                if (i < nonPkColumns.size() - 1) sql.append(", ");
            }
            sql.append(" ");
        }

        sql.append("WHEN NOT MATCHED THEN INSERT (");
        for (int i = 0; i < columns.size(); i++) {
            sql.append(columns.get(i));
            if (i < columns.size() - 1) sql.append(", ");
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            sql.append("s.").append(columns.get(i));
            if (i < columns.size() - 1) sql.append(", ");
        }
        sql.append(")");

        return sql.toString();
    }
}
