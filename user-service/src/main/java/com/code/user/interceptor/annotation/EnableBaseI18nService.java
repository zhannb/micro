package com.code.user.interceptor.annotation;

import com.code.user.interceptor.service.BaseI18nService;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * create by liuliang
 * on 2019-08-08  14:07
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({BaseI18nService.class})
public @interface EnableBaseI18nService {
}
