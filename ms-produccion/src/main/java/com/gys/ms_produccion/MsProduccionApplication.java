package com.gys.ms_produccion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// @EnableFeignClients - habilitarlo cuando se integre con ms-pedidos real
@SpringBootApplication
@EnableDiscoveryClient
public class MsProduccionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsProduccionApplication.class, args);
    }
}
