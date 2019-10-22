package com.code.user.feign;

//import com.code.user.config.EurekaFeignServiceFailure;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "crawler-order")
public interface OrderClient {

    @GetMapping("/order")
    String order(@RequestParam("id") String id);

    @PutMapping("/test/lcn")
    Boolean testLcn();

}
