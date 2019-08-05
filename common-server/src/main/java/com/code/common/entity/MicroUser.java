package com.code.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * create by liuliang
 * on 2019-08-05  11:15
 */
@Data
@TableName("micro_user")
public class MicroUser   {


    private Long id;

    private String name;

    private String password;


}
