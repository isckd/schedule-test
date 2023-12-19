package com.example.scheduletest.config;

import com.example.scheduletest.common.LoggingRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class Config {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new LoggingRequestInterceptor(applicationName)));
        return restTemplate;
    }


}
