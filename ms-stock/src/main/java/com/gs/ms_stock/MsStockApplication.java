package com.gs.ms_stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync   // para la auditoría fire-and-forget (AuditoriaClient)
public class MsStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsStockApplication.class, args);
    }
}
