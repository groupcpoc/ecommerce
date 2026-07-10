package com.ecommerce.authservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RealmConfig {

    private String realm;

    private ClientConfig client;

    private List<String> roles;

    private List<UserConfig> users;

    public RealmConfig() {
    }

    public RealmConfig(String realm, ClientConfig client, List<String> roles, List<UserConfig> users) {
        this.realm = realm;
        this.client = client;
        this.roles = roles;
        this.users = users;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public ClientConfig getClient() {
        return client;
    }

    public void setClient(ClientConfig client) {
        this.client = client;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<UserConfig> getUsers() {
        return users;
    }

    public void setUsers(List<UserConfig> users) {
        this.users = users;
    }
}