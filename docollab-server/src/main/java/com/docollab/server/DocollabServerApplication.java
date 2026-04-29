package com.docollab.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocollabServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocollabServerApplication.class, args);
        System.out.println("DOCollab Cloud Relay Server is ONLINE and listening for WebSockets.");
    }
}