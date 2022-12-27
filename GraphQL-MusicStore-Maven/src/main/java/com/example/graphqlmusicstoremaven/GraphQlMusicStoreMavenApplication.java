package com.example.graphqlmusicstoremaven;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackageClasses = {
                com.example.graphqlmusicstoremaven.graphql.ApplicationSpecificSpringComponentScanMarker.class,
                com.example.graphqlmusicstoremaven.configuration.ApplicationSpecificSpringComponentScanMarker.class
        })
public class GraphQlMusicStoreMavenApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphQlMusicStoreMavenApplication.class, args);
    }

}