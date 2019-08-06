package com.code.user.controller;

import com.code.user.feign.OrderClient;
import com.code.user.util.MicroUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class HelloController {

    @Autowired
    private OrderClient orderClient;

    @Value("${crawler.test}")
    private String crawler;

    @GetMapping("/hello")
    public String hello(@RequestParam("name") String name) {
        System.out.println("入参：" + name);
        log.info("current:{}", MicroUserUtil.getCurrentUser());
        return "hello " + name + "=====" + orderClient.order(name);
    }


    @GetMapping("/crawler")
    public String crawler(){
        return this.crawler;
    }



}
