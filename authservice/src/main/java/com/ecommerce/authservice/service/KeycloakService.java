package com.ecommerce.authservice.service;

import com.ecommerce.authservice.model.LoginRequest;
import com.ecommerce.authservice.model.RegisterRequest;
import com.ecommerce.authservice.model.TokenResponse;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    private final Keycloak keycloak;
    @Autowired
    private RestTemplate rt;
    @Value("${keycloak.url}")
    private String url;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client-id}")
    private String client;
    @Value("${keycloak.client-secret}")
    private String clientsecret;
    @Value("${keycloak.admin.username}")
    private String user;
    @Value("${keycloak.admin.password}")
    private String pass;
    @Autowired
    private RestTemplate restTemplate;

    public KeycloakService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    private String adminToken() {

        String tokenUrl = "http://localhost:8080/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "admin-cli");
        body.add("username", "admin");
        body.add("password", "admin123");
        body.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(tokenUrl, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    public void createUser(RegisterRequest r, String requestType) {
        String token = adminToken();

        String createUserUrl = url + "/admin/realms/" + realm + "/users";

        Map<String, Object> user = new HashMap<>();
        user.put("username", r.email);
        user.put("email", r.email);
        user.put("enabled", true);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", r.password);
        credential.put("temporary", false);

        user.put("credentials", List.of(credential));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(user, headers);

        ResponseEntity<Void> response = rt.postForEntity(createUserUrl, entity, Void.class);

        String location = response.getHeaders().getFirst("Location");
        String userId = location.substring(location.lastIndexOf("/") + 1);

        String roleName = requestType.equalsIgnoreCase("admin") ? "admin" : "user";

        String roleUrl = url + "/admin/realms/" + realm + "/roles/" + roleName;
        HttpEntity<Void> roleEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> roleResponse =
                rt.exchange(roleUrl, HttpMethod.GET, roleEntity, Map.class);

        Map role = roleResponse.getBody();
        String assignRoleUrl = url + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

        List<Map> roles = new ArrayList<>();
        roles.add(role);

        HttpEntity<List<Map>> assignEntity = new HttpEntity<>(roles, headers);

        rt.exchange(assignRoleUrl, HttpMethod.POST, assignEntity, Void.class);
    }


    public TokenResponse login(LoginRequest r) {
        String u = url + "/realms/" + realm + "/protocol/openid-connect/token";
        MultiValueMap<String, String> b = new LinkedMultiValueMap<>();
        b.add("client_id", client);
        b.add("username", r.email);
        b.add("password", r.password);
        b.add("grant_type", "password");
        return rt.postForObject(u, b, TokenResponse.class);
    }

    public TokenResponse refresh(String refresh_token) {

        String tokenUrl = url + "/realms/" + realm + "/protocol/openid-connect/token";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", client);
        body.add("client_secret", clientsecret);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refresh_token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);
        return rt.postForObject(tokenUrl, request, TokenResponse.class);
    }

    public void disableUser(String userId) {
        UserResource userResource = keycloak.realm(realm)
                .users()
                .get(userId);

        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(false);
        userResource.update(user);
    }

    public void revokeUserSessions(String userId) {
        keycloak.realm(realm)
                .users()
                .get(userId);


    }
}