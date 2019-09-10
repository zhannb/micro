package com.code.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.code.user.entity.MicroTest;
import com.code.user.entity.MicroUser;
import com.code.user.interceptor.service.BaseI18nService;
import com.code.user.mapper.MicroTestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * create by liuliang
 * on 2019-08-08  13:31
 */
@Service
public class MicroTestService  extends ServiceImpl<MicroTestMapper, MicroTest> {

    @Autowired
    private BaseI18nService baseI18nService;

    public MicroTest getTest(){
        MicroTest microTest = baseI18nService.selectOneBaseTableInfoWithI18n(1L, MicroTest.class);
        return microTest;
    }


}
