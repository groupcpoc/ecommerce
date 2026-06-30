package com.ecommerce.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.ecommerce.apigateway.handler.CustomAccessDeniedHandler;
import com.ecommerce.apigateway.handler.CustomAuthenticationEntryPoint;

import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.core.convert.converter.Converter;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**").permitAll()
                        // Payment service — specific rules
                        .pathMatchers(org.springframework.http.HttpMethod.POST, "/api/payments/*/refund")
                        .hasRole("ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/payments/me").hasRole("CUSTOMER")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/payments").hasRole("ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/payments/**").hasRole("CUSTOMER")
                        .pathMatchers("/api/payments/**").denyAll()

                        // Order service — specific rules
                        .pathMatchers(org.springframework.http.HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/orders/me").hasRole("CUSTOMER")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/orders/assigned").hasRole("DELIVERY_EXECUTIVE")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/orders/*").hasAnyRole("CUSTOMER", "ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.DELETE, "/api/orders/*").hasAnyRole("CUSTOMER", "ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.PUT, "/api/orders/*/status").hasRole("ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.PUT, "/api/orders/*/assign").hasRole("ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.PUT, "/api/orders/*/delivery-status").hasRole("DELIVERY_EXECUTIVE")
                        .pathMatchers("/api/orders/**").denyAll()

                        .pathMatchers("/api/users/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .anyExchange().authenticated())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private Converter<Jwt, Mono<org.springframework.security.authentication.AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        java.util.Set<String> rolesSet = new java.util.HashSet<>();

        // 1. Try extracting from standard "roles" claim
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            rolesSet.addAll(roles);
        }

        // 2. Try extracting from realm_access.roles claim
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                rolesSet.addAll(realmRoles);
            }
        }

        System.out.println("Extracted roles claims: " + rolesSet);

        return rolesSet.stream()
                .map(role -> {
                    String formatted = role.toUpperCase();
                    if (!formatted.startsWith("ROLE_")) {
                        formatted = "ROLE_" + formatted;
                    }
                    return new SimpleGrantedAuthority(formatted);
                })
                .collect(Collectors.toList());
    }
}
