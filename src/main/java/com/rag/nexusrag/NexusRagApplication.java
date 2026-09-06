package com.rag.nexusrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NexusRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusRagApplication.class, args);
    }

}
