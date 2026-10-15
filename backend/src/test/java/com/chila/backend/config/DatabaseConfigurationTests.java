package com.chila.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DatabaseConfigurationTests {

    @Test
    void convertsNeonUriAndExtractsEncodedCredentials() {
        DatabaseConfiguration.ConnectionSettings settings = DatabaseConfiguration.parse(
                "postgresql://neon_user:pa%40ss%3Aword@ep-example.neon.tech:5432/neondb?sslmode=require",
                "local_user", "local_password", null, null);

        assertEquals("jdbc:postgresql://ep-example.neon.tech:5432/neondb?sslmode=require", settings.jdbcUrl());
        assertEquals("neon_user", settings.username());
        assertEquals("pa@ss:word", settings.password());
    }

    @Test
    void explicitSpringCredentialsOverrideCredentialsFromUrl() {
        DatabaseConfiguration.ConnectionSettings settings = DatabaseConfiguration.parse(
                "postgres://url_user:url_password@ep-example.neon.tech/neondb?sslmode=require",
                "local_user", "local_password", "explicit_user", "explicit_password");

        assertEquals("jdbc:postgresql://ep-example.neon.tech/neondb?sslmode=require", settings.jdbcUrl());
        assertEquals("explicit_user", settings.username());
        assertEquals("explicit_password", settings.password());
    }

    @Test
    void keepsJdbcUrlAndUsesLocalCredentialsWhenNoUrlCredentialsExist() {
        DatabaseConfiguration.ConnectionSettings settings = DatabaseConfiguration.parse(
                "jdbc:postgresql://localhost:5433/chila", "chila", "local_password", null, null);

        assertEquals("jdbc:postgresql://localhost:5433/chila", settings.jdbcUrl());
        assertEquals("chila", settings.username());
        assertEquals("local_password", settings.password());
    }
}
