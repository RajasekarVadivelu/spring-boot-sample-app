package com.springboot.sample.app.rest.controller;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {

    private final Logger LOG = LoggerFactory.getLogger(HelloWorld.class);

    @Observed(name="HelloWorld", contextualName = "REST", lowCardinalityKeyValues = {"rest-call","helloWorld"})
    @RequestMapping("/")
    @ResponseBody
    ResponseEntity<String> home() {
        LOG.info("Hello world!");
        MDC.put("Greeting", "Hello");
        return ResponseEntity.ok("Hello World!");
    }
}
