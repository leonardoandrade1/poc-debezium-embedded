package com.leonardoandrade1.poc_debeizum_embedded.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DebeziumConnectorConfig {

    @Value("${debezium.source.host}")
    private String sourceHost;

    @Value("${debezium.source.port}")
    private int sourcePort;

    @Value("${debezium.source.user}")
    private String sourceUser;

    @Value("${debezium.source.password}")
    private String sourcePassword;

    @Value("${debezium.source.database}")
    private String sourceDatabase;

    @Value("${debezium.source.table.include}")
    private String tableIncludeList;

    @Value("${debezium.offset.jdbc.url}")
    private String offsetJdbcUrl;

    @Value("${debezium.offset.jdbc.user}")
    private String offsetJdbcUser;

    @Value("${debezium.offset.jdbc.password}")
    private String offsetJdbcPassword;

    @Bean
    public io.debezium.config.Configuration debeziumMySqlConnector() {
        Map<String, String> configMap = new HashMap<>();

        // Connector identity
        configMap.put("name", "cdc-mysql-product-connector");
        configMap.put("connector.class", "io.debezium.connector.mysql.MySqlConnector");

        // MySQL source connection
        configMap.put("database.hostname", sourceHost);
        configMap.put("database.port", String.valueOf(sourcePort));
        configMap.put("database.user", sourceUser);
        configMap.put("database.password", sourcePassword);
        configMap.put("database.server.id", "42");

        // Topic prefix for CDC events
        configMap.put("topic.prefix", "cdc-dbz");

        // Tables to capture
        configMap.put("database.include.list", sourceDatabase);
        configMap.put("table.include.list", tableIncludeList);

        // Data handling: serialize decimals as strings to avoid Base64 encoding in JSON
        configMap.put("decimal.handling.mode", "string");

        // Snapshot: initial snapshot when no offset exists
        configMap.put("snapshot.mode", "initial");

        // Signal
        configMap.put("signal.enabled", "true");
        configMap.put("signal.data.collection", sourceDatabase +
                ".debezium_signal");

        // Offset storage in destination MySQL via JDBC
        configMap.put("offset.storage", "io.debezium.storage.jdbc.offset.JdbcOffsetBackingStore");
        configMap.put("offset.storage.jdbc.offset.table.name", "debezium_offset_storage");
        configMap.put("offset.storage.jdbc.url", offsetJdbcUrl);
        configMap.put("offset.storage.jdbc.user", offsetJdbcUser);
        configMap.put("offset.storage.jdbc.password", offsetJdbcPassword);
        configMap.put("offset.flush.interval.ms", "5000");

        // Schema history storage in destination MySQL via JDBC
        configMap.put("schema.history.internal", "io.debezium.storage.jdbc.history.JdbcSchemaHistory");
        configMap.put("schema.history.internal.jdbc.url", offsetJdbcUrl);
        configMap.put("schema.history.internal.jdbc.user", offsetJdbcUser);
        configMap.put("schema.history.internal.jdbc.password", offsetJdbcPassword);
        configMap.put("schema.history.internal.jdbc.schema.history.table.name", "debezium_schema_history");

        // Error handling
        configMap.put("errors.log.include.messages", "true");

        return io.debezium.config.Configuration.from(configMap);
    }
}
