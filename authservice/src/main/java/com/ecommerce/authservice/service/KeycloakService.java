package com.ecommerce.authservice.service;

import com.ecommerce.authservice.exception.UserAlreadyExistsException;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    private final Keycloak keycloak;
    @Autowired
    private RestTemplate rt;
    @Value("${keycloak.auth-server-url}")
    private String url;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client_id}")
    private String client;
    @Value("${keycloak.client_secret}")
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
        body.add("password", "admin321");
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

        if (r.getEmail() == null || r.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (r.getPassword() == null || r.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (userExists(r.getEmail(), token)) {
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + r.getEmail());
        }

        String createUserUrl =
                url + "/admin/realms/" + realm + "/users";

        Map<String, Object> user = new HashMap<>();
        user.put("username", r.username); // Login using email
        user.put("email", r.getEmail());
        user.put("enabled", true);
        user.put("emailVerified", true);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {

            // Create User
            ResponseEntity<String> response =
                    rt.postForEntity(
                            createUserUrl,
                            new HttpEntity<>(user, headers),
                            String.class);

            String location =
                    response.getHeaders().getFirst("Location");

            if (location == null) {
                throw new RuntimeException(
                        "User created but Location header missing");
            }

            String userId =
                    location.substring(location.lastIndexOf("/") + 1);
            r.setUserId(userId);
            System.out.println("===================================USER ID======="+userId+"==="+r.getUserId());
            // Save Password Permanently
            String passwordUrl =
                    url + "/admin/realms/" + realm +
                            "/users/" + userId +
                            "/reset-password";

            Map<String, Object> passwordPayload =
                    new HashMap<>();

            passwordPayload.put("type", "password");
            passwordPayload.put("value", r.getPassword());
            passwordPayload.put("temporary", false);

            rt.exchange(
                    passwordUrl,
                    HttpMethod.PUT,
                    new HttpEntity<>(passwordPayload, headers),
                    Void.class);

            // Assign Realm Role
            String roleName =
                    requestType.equalsIgnoreCase("admin")
                            ? "admin"
                            : "user";

            String roleUrl =
                    url + "/admin/realms/" + realm +
                            "/roles/" + roleName;

            ResponseEntity<Map> roleResponse =
                    rt.exchange(
                            roleUrl,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            Map.class);

            Map role = roleResponse.getBody();

            String assignRoleUrl =
                    url + "/admin/realms/" + realm +
                            "/users/" + userId +
                            "/role-mappings/realm";

            rt.exchange(
                    assignRoleUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(role), headers),
                    Void.class);

            System.out.println(
                    "USER REGISTERED SUCCESSFULLY");

        } catch (HttpClientErrorException e) {

            if (e.getStatusCode().value() == 409) {
                throw new UserAlreadyExistsException(
                        "User already exists (Keycloak): "
                                + r.getEmail());
            }

            throw new RuntimeException(
                    "Keycloak error: "
                            + e.getResponseBodyAsString());
        }
    }
    public TokenResponse login(LoginRequest request) {

        String tokenUrl =
                url + "/realms/" + realm +
                        "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("grant_type", "password");
        form.add("client_id", client);

        // Add only if client is confidential
        form.add("client_secret", clientsecret);

        form.add("username", request.getEmail());
        form.add("password", request.getPassword());

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(form, headers);

        ResponseEntity<TokenResponse> response =
                rt.postForEntity(
                        tokenUrl,
                        entity,
                        TokenResponse.class);

        return response.getBody();
    }
    public TokenResponse refresh(String refresh_token) {

        System.out.println("Client Id = " + client);
        System.out.println("Realm = " + realm);
        System.out.println("Refresh Token = " + refresh_token);

        String tokenUrl =
                url + "/realms/" + realm +
                        "/protocol/openid-connect/token";
        System.out.println("=================================TOKEN URL" + tokenUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("grant_type", "refresh_token");
        form.add("client_id", client);
        form.add("client_secret", clientsecret);
        form.add("refresh_token", refresh_token);

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(form, headers);

        try {

            ResponseEntity<TokenResponse> response =
                    restTemplate.exchange(
                            tokenUrl,
                            HttpMethod.POST,
                            entity,
                            TokenResponse.class);

            return response.getBody();

        } catch (HttpClientErrorException e) {

            throw new RuntimeException(
                    "Refresh Token Failed : "
                            + e.getResponseBodyAsString());
        }
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

    private boolean userExists(String email, String token) {

        String searchUrl = url + "/admin/realms/" + realm + "/users?username=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = rt.exchange(
                searchUrl,
                HttpMethod.GET,
                entity,
                List.class
        );

        List users = response.getBody();

        return users != null && !users.isEmpty();
    }
}
