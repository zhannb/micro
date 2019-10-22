package com.code.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * create by liuliang
 * on 2019-10-19  17:00
 */
@Data
@TableName("order_test")
public class OrderTest {

    private Long id;

    private String name;

    private int age;
}
