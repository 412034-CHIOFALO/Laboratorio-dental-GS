package com.gys.ms_auth.config;

import com.gys.ms_auth.filter.LoginRateLimitInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registra el rate limiter de login.
     * Solo intercepta POST /api/auth/login (el interceptor filtra internamente).
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRateLimitInterceptor())
                .addPathPatterns("/api/auth/login");
    }

    @Bean
    public LoginRateLimitInterceptor loginRateLimitInterceptor() {
        return new LoginRateLimitInterceptor();
    }
}
