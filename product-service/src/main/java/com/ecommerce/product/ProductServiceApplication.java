package com.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        System.out.println("JVM Timezone = " + TimeZone.getDefault().getID());
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}