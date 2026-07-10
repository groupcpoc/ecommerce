package com.ecommerce.authservice.service;

import com.ecommerce.authservice.exception.UserAlreadyExistsException;
import com.ecommerce.authservice.model.LoginRequest;
import com.ecommerce.authservice.model.RegisterRequest;
import com.ecommerce.authservice.model.TokenResponse;
import lombok.RequiredArgsConstructor;
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
public class UserService {

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

    public UserService(Keycloak keycloak) {
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
            System.out.println("===================================USER ID=======" + userId + "===" + r.getUserId());
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



//    public void createUser(RegisterRequest r, String requestType) {
//
//        String token = adminToken();
//
//        // ✅ Step 1: Validate input
//        if (r.email == null || r.email.isBlank()) {
//            throw new IllegalArgumentException("Email cannot be empty");
//        }
//
//        if (r.password == null || r.password.length() < 6) {
//            throw new IllegalArgumentException("Password must be at least 6 characters");
//        }
//
//        // ✅ Step 2: Check if user already exists
//        if (userExists(r.email, token)) {
//            throw new UserAlreadyExistsException(
//                    "User already exists with email: " + r.email
//            );
//        }
//
//        String createUserUrl = url + "/admin/realms/" + realm + "/users";
//
//        Map<String, Object> user = new HashMap<>();
//        user.put("username", r.username);
//        user.put("email", r.email);
//        user.put("enabled", true);
//        user.put("emailVerified", true);
//        user.put("requiredActions", List.of("VERIFY_EMAIL"));
////        user.put("requiredActions", List.of("VERIFY_EMAIL","UPDATE_PASSWORD"));
//
//        Map<String, Object> credential = new HashMap<>();
//        credential.put("type", "password");
//        credential.put("value", r.password);
//        credential.put("temporary", false);
//
//        user.put("credentials", List.of(credential));
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(token);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(user, headers);
//
//        try {
//            // ✅ Step 3: Call Keycloak API
//            ResponseEntity<String> response =
//                    rt.postForEntity(createUserUrl, entity, String.class);
//
//            if (!response.getStatusCode().is2xxSuccessful()) {
//                throw new RuntimeException("Failed to create user: " + response.getBody());
//            }
//
//            // ✅ Step 4: Extract userId safely
//            String location = response.getHeaders().getFirst("Location");
//
//            if (location == null) {
//                throw new RuntimeException("User created but Location header missing");
//            }
//
//            String userId = location.substring(location.lastIndexOf("/") + 1);
//            /*---*/
//
//            // NEW STEP: Set Credential via Keycloak API (BEST PRACTICE)
//            String passwordUrl =
//                    url + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";
//
//            Map<String, Object> passwordPayload = new HashMap<>();
//            passwordPayload.put("type", "password");
//            passwordPayload.put("value", r.password);
//            passwordPayload.put("temporary", false);
//
//            HttpEntity<Map<String, Object>> passwordEntity =
//                    new HttpEntity<>(passwordPayload, headers);
//            rt.exchange(passwordUrl, HttpMethod.PUT, passwordEntity, Void.class);
//            /*-----*/
////modify
////            String actionEmailUrl =
////                    url + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";
////
////            List<String> actions = List.of("VERIFY_EMAIL", "UPDATE_PASSWORD");
////
////            HttpEntity<List<String>> emailEntity = new HttpEntity<>(actions, headers);
////
////            rt.exchange(actionEmailUrl, HttpMethod.PUT, emailEntity, String.class);
//////modify end
//            // ✅ Step 5: Assign role
//            String roleName = requestType.equalsIgnoreCase("admin") ? "admin" : "user";
//
//            String roleUrl = url + "/admin/realms/" + realm + "/roles/" + roleName;
//
//            HttpEntity<Void> roleEntity = new HttpEntity<>(headers);
//
//            ResponseEntity<Map> roleResponse =
//                    rt.exchange(roleUrl, HttpMethod.GET, roleEntity, Map.class);
//
//            Map role = roleResponse.getBody();
//
//            String assignRoleUrl =
//                    url + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
//
//            List<Map> roles = new ArrayList<>();
//            roles.add(role);
//
//            HttpEntity<List<Map>> assignEntity =
//                    new HttpEntity<>(roles, headers);
//            System.out.println("<<<<<<<<<<<<<<<<<<<<<<<USER REGISTERED SUCCESSFULLY>>>>>>>>>>>>>>>>>>>>>");
//            rt.exchange(assignRoleUrl, HttpMethod.POST, assignEntity, Void.class);
//
//        } catch (HttpClientErrorException e) {
//
//            // ✅ Handle 409 conflict (duplicate user from Keycloak side)
//            if (e.getStatusCode().value() == 409) {
//                throw new UserAlreadyExistsException(
//                        "User already exists (Keycloak): " + r.email
//                );
//            }
//
//            throw new RuntimeException("Keycloak error: " + e.getResponseBodyAsString());
//        }
//    }`


//    public void createUser(RegisterRequest r, String requestType) {
//       String token = adminToken();
//

    /// / Validate input
//        if (r.email == null || r.email.isBlank()) {
//            throw new IllegalArgumentException("Email cannot be empty");
//        }
//
//        if (r.password == null || r.password.length() < 6) {
//            throw new IllegalArgumentException("Password must be at least 6 characters");
//        }
//
//        //if user already exists
//        if (userExists(r.email, token)) {
//            throw new UserAlreadyExistsException(
//                    "User already exists with email: " + r.email
//            );
//        }
//
//        String createUserUrl = url + "/admin/realms/" + realm + "/users";
//
//        Map<String, Object> user = new HashMap<>();
//        user.put("username", r.email);
//        user.put("email", r.email);
//        user.put("enabled", true);
//
//        Map<String, Object> credential = new HashMap<>();
//        credential.put("type", "password");
//        credential.put("value", r.password);
//        credential.put("temporary", false);
//
//        user.put("credentials", List.of(credential));
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(token);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(user, headers);
//
//        ResponseEntity<String> response = rt.postForEntity(createUserUrl, entity, String.class);
//
//        String location = response.getHeaders().getFirst("Location");
//        String userId = location.substring(location.lastIndexOf("/") + 1);
//
//        String roleName = requestType.equalsIgnoreCase("admin") ? "admin" : "user";
//
//        String roleUrl = url + "/admin/realms/" + realm + "/roles/" + roleName;
//        HttpEntity<Void> roleEntity = new HttpEntity<>(headers);
//
//        ResponseEntity<Map> roleResponse =
//                rt.exchange(roleUrl, HttpMethod.GET, roleEntity, Map.class);
//
//        Map role = roleResponse.getBody();
//        String assignRoleUrl = url + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
//
//        List<Map> roles = new ArrayList<>();
//        roles.add(role);
//
//        HttpEntity<List<Map>> assignEntity = new HttpEntity<>(roles, headers);
//
//        rt.exchange(assignRoleUrl, HttpMethod.POST, assignEntity, Void.class);
//    }

//    public TokenResponse login(LoginRequest r) {
//
//        String tokenUrl = url + "/realms/" + realm + "/protocol/openid-connect/token";
//        System.out.println("<<<<<<<<<<<<<<<<<<<<TOKEN URL>>>>>>>>>>>>>>>" + tokenUrl);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//        body.add("grant_type", "password");
//        body.add("client-id", client);

    /// /        body.add("client-secret", clientsecret);
//        body.add("username", r.email);
//        body.add("password", r.password);
//
//        HttpEntity<MultiValueMap<String, String>> entity =
//                new HttpEntity<>(body, headers);
//
//        try {
//            ResponseEntity<TokenResponse> response =
//                    rt.exchange(tokenUrl, HttpMethod.POST, entity, TokenResponse.class);
//            System.out.println("RESPONSE" + response.getBody());
//            return response.getBody();
//
//        } catch (HttpClientErrorException e) {
//            throw new RuntimeException("Login failed: " + e.getStatusCode() + "======" + e.getResponseBodyAsString());
//        }
//    }

//    public TokenResponse login(LoginRequest r) {
//        String u = url + "/realms/" + realm + "/protocol/openid-connect/token";
//        MultiValueMap<String, String> b = new LinkedMultiValueMap<>();
//        b.add("client-id", client);
//        b.add("username", r.email);
//        b.add("password", r.password);
//        b.add("grant_type", "password");
//        return restTemplate.postForObject(u, b, TokenResponse.class);
//    }

//    public RefreshRequest refresh(String refresh_token) {
//
//        String tokenUrl = url + "/realms/" + realm + "/protocol/openid-connect/token";
//        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//        body.add("client_id", client);
//        body.add("client_secret", clientsecret);
//        body.add("grant_type", "refresh_token");
//        body.add("refresh_token", refresh_token);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        HttpEntity<MultiValueMap<String, String>> request =
//                new HttpEntity<>(body, headers);
//        return rt.postForObject(tokenUrl, request, RefreshRequest.class);
//    }
//    public void initializeKeycloak() {
//
//        RealmConfig config = loadRealmJson();
//
//        createRealm(config);
//
//        createClient(config);
//
//        createRoles(config);
//
//        createUsers(config);
//    }
//
//    private void createRealm(RealmConfig config) {
//
//        try {
//
//            keycloak.realm(config.getRealm())
//                    .toRepresentation();
//
//        } catch (Exception ex) {
//
//            RealmRepresentation realm =
//                    new RealmRepresentation();
//
//            realm.setRealm(config.getRealm());
//            realm.setEnabled(true);
//
//            keycloak.realms().create(realm);
//        }
//    }
//    private void createClient(RealmConfig config) {
//        ClientRepresentation client =
//                new ClientRepresentation();
//
//        client.setClientId(
//                config.getClientId());
//
//        client.setSecret(
//                config.getClientSecret());
//
//        client.setEnabled(true);
//
//        client.setDirectAccessGrantsEnabled(true);
//
//        realm.clients().create(client);
//    }
//    private void createRoles(RealmConfig config) {
//        realm.roles().create(role);
//    }
//    private void createUsers(RealmConfig config) {
//        UserRepresentation user =
//                new UserRepresentation();
//
//        user.setUsername("admin");
//
//        user.setEmail("admin@test.com");
//
//        user.setEnabled(true);
//    }
//
//
//    private void createUser(
//            UserConfig userConfig,
//            String realmName) {
//
//        RealmResource realm =
//                keycloak.realm(realmName);
//
//        UserRepresentation user =
//                new UserRepresentation();
//
//        user.setUsername(
//                userConfig.getUsername());
//
//        user.setEmail(
//                userConfig.getEmail());
//
//        user.setEnabled(true);
//
//        user.setEmailVerified(
//                userConfig.isEmailVerified());
//
//        Response response =
//                realm.users().create(user);
//
//        String userId =
//                CreatedResponseUtil
//                        .getCreatedId(response);
//
//        setPassword(
//                realm,
//                userId,
//                userConfig.getPassword());
//
//        assignRoles(
//                realm,
//                userId,
//                userConfig.getRoles());
//    }
//    private void setPassword(
//            RealmResource realm,
//            String userId,
//            String password) {
//
//        CredentialRepresentation credential =
//                new CredentialRepresentation();
//
//        credential.setType(
//                CredentialRepresentation.PASSWORD);
//
//        credential.setValue(password);
//
//        credential.setTemporary(false);
//
//        realm.users()
//                .get(userId)
//                .resetPassword(credential);
//    }
//
//    private void assignRoles(
//            RealmResource realm,
//            String userId,
//            List<String> roles) {
//
//        List<RoleRepresentation> roleList =
//                new ArrayList<>();
//
//        for(String roleName : roles) {
//
//            roleList.add(
//                    realm.roles()
//                            .get(roleName)
//                            .toRepresentation());
//        }
//
//        realm.users()
//                .get(userId)
//                .roles()
//                .realmLevel()
//                .add(roleList);
//    }
//
//    private RealmConfig loadRealmConfig()
//            throws Exception {
//
//        InputStream is =
//                new ClassPathResource(
//                        "keycloak/realm-config.json")
//                        .getInputStream();
//
//        return objectMapper.readValue(
//                is,
//                RealmConfig.class);
//    }

