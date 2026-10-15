package com.chila.backend.config;

import java.net.URI;
import java.net.URISyntaxException;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
public class DatabaseConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(Environment environment) {
        String configuredUrl = environment.getRequiredProperty("spring.datasource.url");
        String fallbackUsername = environment.getProperty("spring.datasource.username", "chila");
        String fallbackPassword = environment.getProperty("spring.datasource.password", "");
        String explicitUsername = environment.getProperty("SPRING_DATASOURCE_USERNAME");
        String explicitPassword = environment.getProperty("SPRING_DATASOURCE_PASSWORD");
        ConnectionSettings connection = parse(configuredUrl, fallbackUsername, fallbackPassword,
                explicitUsername, explicitPassword);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(connection.jdbcUrl());
        dataSource.setUsername(connection.username());
        dataSource.setPassword(connection.password());
        return dataSource;
    }

    static ConnectionSettings parse(
            String configuredUrl,
            String fallbackUsername,
            String fallbackPassword,
            String explicitUsername,
            String explicitPassword) {
        String uriValue = configuredUrl.startsWith("jdbc:")
                ? configuredUrl.substring("jdbc:".length())
                : configuredUrl;
        try {
            URI uri = new URI(uriValue);
            if (!("postgresql".equalsIgnoreCase(uri.getScheme()) || "postgres".equalsIgnoreCase(uri.getScheme()))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("POSTGRESQL_DB_URL must be a PostgreSQL connection URL.");
            }

            String authority = uri.getRawAuthority();
            int userInfoEnd = authority.lastIndexOf('@');
            if (userInfoEnd >= 0) {
                authority = authority.substring(userInfoEnd + 1);
            }
            String jdbcUrl = "jdbc:postgresql://" + authority
                    + (uri.getRawPath() == null ? "" : uri.getRawPath())
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

            String uriUsername = null;
            String uriPassword = null;
            if (uri.getUserInfo() != null) {
                int separator = uri.getUserInfo().indexOf(':');
                uriUsername = separator < 0 ? uri.getUserInfo() : uri.getUserInfo().substring(0, separator);
                uriPassword = separator < 0 ? "" : uri.getUserInfo().substring(separator + 1);
            }

            return new ConnectionSettings(jdbcUrl,
                    preferred(explicitUsername, preferred(uriUsername, fallbackUsername)),
                    preferred(explicitPassword, preferred(uriPassword, fallbackPassword)));
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("POSTGRESQL_DB_URL is not a valid PostgreSQL connection URL.");
        }
    }

    private static String preferred(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    record ConnectionSettings(String jdbcUrl, String username, String password) {
    }
}
