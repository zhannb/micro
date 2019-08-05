package com.code.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * create by liuliang
 * on 2019-08-05  19:18
 */
@Configuration
public class FeignConfig implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        //添加token
//        requestTemplate.header(headerNames"Access-Token", request.getHeader("Access-Token"));

        Object attribute = request.getAttribute("OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE");
        String token = attribute == null ? null : attribute.toString();
        if(!StringUtils.isEmpty(token)){
            requestTemplate.header("Authorization", "Bearer " + token);
        }

    }
}
