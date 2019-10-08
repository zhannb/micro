package com.code.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@RestController
public class OrderController {

    @GetMapping("/order")
    public String order(@RequestParam("id") String id) throws InterruptedException {

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = requestAttributes == null ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
        Object attribute = request.getAttribute("OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE");
        String token = attribute == null ? null : attribute.toString();
        System.out.println(Thread.currentThread().getName()+" 进入... "+ token);
//        Thread.sleep(2000);


        System.out.println(Thread.currentThread().getName()+" 出去... ");
        return "订单id：" + id;
    }
}
