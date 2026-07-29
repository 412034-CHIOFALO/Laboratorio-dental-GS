package com.gs.ms_finanzas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableAsync   // para la auditoría fire-and-forget (AuditoriaClient)
public class MsFinanzasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsFinanzasApplication.class, args);
    }
}
