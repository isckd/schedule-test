package com.example.scheduletest.controller;

import com.example.scheduletest.dto.TestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class TestController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestController(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // make test endpoint
    @GetMapping("/hello/{path}/test")
    public String hello(@PathVariable String path, @RequestParam String query, @RequestParam String query2) {
        restTemplate.getForObject("http://localhost:8080/schedule/api/world/12/test?query=query1&query2=query2", String.class);
        return "hello";
    }

    @GetMapping("/world/{path}/test")
    public String test(@PathVariable String path, @RequestParam String query, @RequestParam String query2) {
        return "test";
    }

    @PostMapping("/body/test")
    public TestDto bodyTest(@RequestBody TestDto dto) {
        return new TestDto(UUID.randomUUID().toString().substring(0, 4), new Random().nextInt(99) + 1);
    }

    @PostMapping("/dynamic/body")
    public Object handleDynamicBody(@RequestParam String method, @RequestParam String uri, @RequestBody Object requestBody) {
        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
        List<Object> responses = new ArrayList<>();

        if(requestBody instanceof List<?>) {
            for(Object o : (List<?>) requestBody) {
                ResponseEntity<Object> response = restTemplate.exchange(uri, httpMethod, new HttpEntity<>(o), Object.class);
                responses.add(response.getBody());
            }
        } else if (requestBody instanceof Map) {
            ResponseEntity<Object> response = restTemplate.exchange(uri, httpMethod, new HttpEntity<>(requestBody), Object.class);
            responses.add(response.getBody());
        }
        return responses;
    }



}

