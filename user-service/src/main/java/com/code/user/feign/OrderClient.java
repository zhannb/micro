package com.code.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-server")
public interface OrderClient {

    @GetMapping("/order")
    String order(@RequestParam("id") String id);
}
