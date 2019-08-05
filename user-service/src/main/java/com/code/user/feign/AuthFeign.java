package com.code.user.feign;

import com.code.user.entity.MicroUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * create by liuliang
 * on 2019-08-05  19:32
 */
@FeignClient(name = "auth-server")
public interface AuthFeign {
    @GetMapping("/current/user")
    MicroUser getCurrentUser();
}
