package com.ecommerce.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/user").permitAll()
                        .requestMatchers("api/auth/admin").permitAll()
                        .requestMatchers("/api/auth/admin/register").hasRole("admin")
                        .requestMatchers("/api/auth/user").hasRole("admin")
                        .requestMatchers("/api/auth/{id}").hasRole("admin")
                        .requestMatchers("/api/auth/user/register").hasRole("user")
                        .requestMatchers("/api/auth/users/*/suspend").hasRole("admin")
                        .requestMatchers("/api/auth/login").authenticated()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtConverter() {
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realm = jwt.getClaim("realm_access");
            if (realm == null) return List.of();
            List<String> roles = (List<String>) realm.get("roles");
            return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList());
        });
        return c;
    }
}
