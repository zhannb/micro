package com.code.user.util;

import com.code.user.entity.MicroUser;
import com.code.user.feign.AuthFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * create by liuliang
 * on 2019-08-05  19:31
 */
@Component
public class MicroUserUtil {


    private static AuthFeign authFeign;

    @Autowired
    public MicroUserUtil(AuthFeign authFeign){
        MicroUserUtil.authFeign = authFeign;
    }

    /**
     * 获取当前用户信息
     * @return
     */
    public  static MicroUser getCurrentUser(){

        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication().getPrincipal();

        MicroUser o = (MicroUser)SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if(!ObjectUtils.isEmpty(o)){
            return o;
        }
        return null;
    }
}
