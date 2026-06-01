package com.vendorhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class VendorHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(VendorHubApplication.class, args);
        System.out.println("\n🔥 ====================================");
        System.out.println("   VendorHub Food Delivery API Started!");
        System.out.println("   API  → http://localhost:8080/api");
        System.out.println("   DB   → MongoDB (vendorhub)");
        System.out.println("======================================\n");
    }
}
