package com.springboot.sample.app.rest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {

    private final Logger LOG = LoggerFactory.getLogger(HelloWorld.class);

    @RequestMapping("/")
    String home() {
        LOG.info("Hello world!");
        MDC.put("Greeting", "Hello");
        return "Hello World!";
    }
}
