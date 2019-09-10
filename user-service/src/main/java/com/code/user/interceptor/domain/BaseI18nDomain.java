package com.code.user.interceptor.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * create by liuliang
 * on 2019-08-08  10:58
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseI18nDomain {
    @TableField(
            exist = false
    )
    private Map<String, List<Map<String, String>>> i18n;

}
