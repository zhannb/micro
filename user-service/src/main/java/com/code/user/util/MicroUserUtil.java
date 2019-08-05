package com.code.user.util;

import com.code.user.entity.MicroUser;
import com.code.user.feign.AuthFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

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
        MicroUser o = authFeign.getCurrentUser();
        if(!ObjectUtils.isEmpty(o)){
            return o;
        }
        return null;
    }
}
