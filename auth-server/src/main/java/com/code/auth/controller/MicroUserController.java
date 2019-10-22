package com.code.auth.controller;

import com.code.auth.entity.MicroUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * create by liuliang
 * on 2019-08-05  16:44
 */
@RestController
@Slf4j
public class MicroUserController {

    /**
     * 这个接口是框架回掉时使用
     * @param principal
     * @return
     */
    @RequestMapping("/user")
    public Principal principalUser(Principal principal) {
        log.info("principal:{}",principal);
        return principal;
    }

    /**
     * 获取当前用户对象 （用于业务逻辑获取当前用户）
     * @return
     */
    @GetMapping("/current/user")
    public MicroUser getCurrentUser(){
        MicroUser microUser = (MicroUser)SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return microUser;
    }


}
