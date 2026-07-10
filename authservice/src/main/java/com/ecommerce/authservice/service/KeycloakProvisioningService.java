package com.ecommerce.authservice.service;

import com.ecommerce.authservice.model.RealmConfig;
import com.ecommerce.authservice.model.UserConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class KeycloakProvisioningService {

    private final Keycloak keycloak;

    public KeycloakProvisioningService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public void provision() throws Exception {

        RealmConfig config = readJson();

        createRealm(config);

        createClient(config);

        createRoles(config);

        createUsers(config);

        assignRoles(config);

        System.out.println("Keycloak bootstrap completed.");
    }
    private RealmConfig readJson() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        Resource resource =
                new ClassPathResource("realm-config.json");

        return mapper.readValue(
                resource.getInputStream(),
                RealmConfig.class);
    }

    private void createRealm(RealmConfig config) {

        try {

            keycloak.realm(config.getRealm())
                    .toRepresentation();

            return;

        } catch (Exception ex) {

        }

        RealmRepresentation realm =
                new RealmRepresentation();

        realm.setRealm(config.getRealm());

        realm.setEnabled(true);

        keycloak.realms()
                .create(realm);
    }

    private void createClient(RealmConfig config) {

        RealmResource realm =
                keycloak.realm(config.getRealm());

        String clientId =
                config.getClient().getClientId();

        boolean exists =
                realm.clients()
                        .findByClientId(clientId)
                        .size() > 0;

        if (exists) {
            return;
        }

        ClientRepresentation client =
                new ClientRepresentation();

        client.setClientId(clientId);

        client.setEnabled(true);

        client.setServiceAccountsEnabled(true);

        client.setDirectAccessGrantsEnabled(true);

        client.setSecret(
                config.getClient().getClientSecret());

        realm.clients().create(client);
    }
    private void createRoles(RealmConfig config) {

        RealmResource realm =
                keycloak.realm(config.getRealm());

        for (String role : config.getRoles()) {

            try {

                realm.roles()
                        .get(role)
                        .toRepresentation();

            } catch (Exception ex) {

                RoleRepresentation rep =
                        new RoleRepresentation();

                rep.setName(role);

                realm.roles().create(rep);
            }
        }
    }
    private void createUsers(RealmConfig config) {

        RealmResource realm =
                keycloak.realm(config.getRealm());

        for (UserConfig user : config.getUsers()) {

            if (!realm.users()
                    .search(user.getUsername())
                    .isEmpty()) {

                continue;
            }

            UserRepresentation u =
                    new UserRepresentation();

            u.setUsername(user.getUsername());

            u.setEmail(user.getEmail());

            u.setEnabled(true);

            Response response =
                    realm.users().create(u);

            String id =
                    CreatedResponseUtil
                            .getCreatedId(response);

            CredentialRepresentation credential =
                    new CredentialRepresentation();

            credential.setType(
                    CredentialRepresentation.PASSWORD);

            credential.setTemporary(false);

            credential.setValue(
                    user.getPassword());

            realm.users()
                    .get(id)
                    .resetPassword(credential);
        }
    }
    private void assignRoles(RealmConfig config) {

        RealmResource realm =
                keycloak.realm(config.getRealm());

        for (UserConfig user : config.getUsers()) {

            UserRepresentation ur =
                    realm.users()
                            .search(user.getUsername())
                            .get(0);

            UserResource userResource =
                    realm.users()
                            .get(ur.getId());

            List<RoleRepresentation> roles =
                    new ArrayList<>();

            for (String role : user.getRoles()) {

                roles.add(
                        realm.roles()
                                .get(role)
                                .toRepresentation()
                );
            }

            userResource.roles()
                    .realmLevel()
                    .add(roles);
        }
    }
}
