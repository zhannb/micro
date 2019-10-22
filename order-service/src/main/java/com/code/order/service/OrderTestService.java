package com.code.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.code.order.entity.OrderTest;
import com.code.order.mapper.OrderTestMapper;
import com.codingapi.txlcn.tc.annotation.LcnTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * create by liuliang
 * on 2019-10-19  17:01
 */
@Service
@Slf4j
public class OrderTestService extends ServiceImpl<OrderTestMapper,OrderTest> {

    @Autowired
    private OrderTestMapper orderTestMapper;


    @LcnTransaction
    public Boolean testLcn(){
        OrderTest orderTest = orderTestMapper.selectById(1);
        orderTest.setAge(orderTest.getAge()+1);
        orderTestMapper.updateById(orderTest);
//        throw new RuntimeException("测试异常");
        return true;
    }

}
