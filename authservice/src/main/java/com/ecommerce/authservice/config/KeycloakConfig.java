package com.ecommerce.authservice.config;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@KeycloakConfiguration
public class KeycloakConfig {

    private final String serverUrl = "http://localhost:8080";
    private final String realm = "master";
    private final String clientId = "admin-cli";
    private final String username = "admin";
    private final String password = "admin321";

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .grantType("password")
                .build();
    }
}