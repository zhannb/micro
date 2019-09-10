package com.code.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.code.user.interceptor.annotation.I18nField;
import com.code.user.interceptor.domain.BaseI18nDomain;
import lombok.Data;

/**
 * create by liuliang
 * on 2019-08-08  13:29
 */
@TableName("micro_test")
@Data
public class MicroTest extends BaseI18nDomain {

    private Long id;

    private String name;

    @I18nField
    private String desc;

    private Integer age;

}
