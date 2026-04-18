package com.netbridge.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.netbridge")
public class NetBridgeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetBridgeServerApplication.class, args);
    }
}
