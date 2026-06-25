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
                        // Product service
                         .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/products").hasRole("CUSTOMER")
                         .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/products/*").hasRole("CUSTOMER")
                         .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/products/search").hasRole("CUSTOMER")
                         .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/products/categories").hasRole("CUSTOMER")
                         .pathMatchers(org.springframework.http.HttpMethod.POST, "/api/products").hasRole("ADMIN")
                         .pathMatchers(org.springframework.http.HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                         .pathMatchers(org.springframework.http.HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                         .pathMatchers("/api/products/**").denyAll()
                        // Notification service
                         .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/notifications").hasRole("ADMIN")
                         .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/notifications/**").hasRole("ADMIN")
                         .pathMatchers("/api/notifications/**").denyAll()
                        // Payment service — specific rules
                        .pathMatchers(org.springframework.http.HttpMethod.POST, "/api/payments/*/refund")
                        .hasRole("ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/payments/me").hasRole("CUSTOMER")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/payments").hasRole("ADMIN")
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/payments/**").hasRole("CUSTOMER")
                        .pathMatchers("/api/payments/**").denyAll()

                        .pathMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
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
        List<String> roles = jwt.getClaimAsStringList("roles");
        System.out.println("Extracted roles claim: " + roles);

        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
