package com.code.user.controller;

import com.code.user.feign.OrderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Autowired
    private OrderClient orderClient;

    @GetMapping("/hello")
    public String hello(@RequestParam("name") String name) {
        System.out.println("入参：" + name);
        return "hello " + name + "=====" + orderClient.order(name);
    }
}
