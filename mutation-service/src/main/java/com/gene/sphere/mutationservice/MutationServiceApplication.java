package com.gene.sphere.mutationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableCaching
@EnableJpaRepositories(basePackages = "com.gene.sphere.mutationservice.repository")
@EntityScan(basePackages = "com.gene.sphere.mutationservice.model")
public class MutationServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MutationServiceApplication.class, args);
        System.out.println("🧬 Mutation Service is running!");
    }
}
