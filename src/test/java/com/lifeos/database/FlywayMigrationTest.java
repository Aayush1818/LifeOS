package com.lifeos.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allEighteenTablesShouldBeCreated() {
        List<String> expectedTables = List.of(
                "users",
                "refresh_tokens",
                "dependents",
                "documents",
                "document_chunks",
                "document_entity_links",
                "insurance_policies",
                "loans",
                "loan_payments",
                "health_appointments",
                "trips",
                "trip_expenses",
                "transactions",
                "budgets",
                "reminders",
                "conversations",
                "chat_messages",
                "message_citations"
        );

        List<String> actualTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';",
                String.class
        );

        assertThat(actualTables).containsAll(expectedTables);
    }

    @Test
    void pgvectorExtensionAndHnswIndexShouldBePresent() {
        Boolean vectorInstalled = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector');", Boolean.class);
        assertThat(vectorInstalled).isTrue();

        Boolean hnswIndexExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_chunks_hnsw');", Boolean.class);
        assertThat(hnswIndexExists).isTrue();

        Boolean tsvIndexExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_chunks_tsv');", Boolean.class);
        assertThat(tsvIndexExists).isTrue();
    }
}
