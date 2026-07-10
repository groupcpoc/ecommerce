package com.ecommerce.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KeycloakBootstrapServices
        implements ApplicationRunner {

    private final KeycloakProvisioningService service;

    public KeycloakBootstrapServices(KeycloakProvisioningService service) {
        this.service = service;
    }
//    @Override
//    public void run(ApplicationArguments args)
//            throws Exception {
//
//        service.provision();
//    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        service.provision();
    }
}