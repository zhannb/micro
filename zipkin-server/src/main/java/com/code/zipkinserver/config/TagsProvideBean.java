package com.code.zipkinserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * create by liuliang
 * on 2019-08-07  13:36
 */
@Configuration
public class TagsProvideBean {

    /**
     * 将MyTagsProvider注入
     *
     * @return
     */
    @Bean
    public MicroTagsProvider myTagsProvider() {
        return new MicroTagsProvider();
    }

}