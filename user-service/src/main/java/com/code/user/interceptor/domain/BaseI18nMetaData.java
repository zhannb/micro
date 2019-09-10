package com.code.user.interceptor.domain;

import lombok.*;

import java.beans.ConstructorProperties;

/**
 * create by liuliang
 * on 2019-08-08  11:26
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseI18nMetaData {
    private String field;
    private String language;
    private String value;
}
