package com.code.user.interceptor.annotation;

import com.baomidou.mybatisplus.autoconfigure.SpringBootVFS;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.code.user.interceptor.domain.BaseI18nDomain;
import com.code.user.interceptor.service.BaseI18nService;
import com.code.user.interceptor.util.ReflectionUtil;
import org.apache.ibatis.io.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * create by liuliang
 * on 2019-08-08  14:08
 */

public class I18nDomainScannerRegistrar implements ImportBeanDefinitionRegistrar {
    private static final Logger log = LoggerFactory.getLogger(I18nDomainScannerRegistrar.class);

    public I18nDomainScannerRegistrar() {
    }

    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes annotationAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(I18nDomainScan.class.getName()));
        List<String> basePackages = new ArrayList();
        String[] var5 = annotationAttrs.getStringArray("value");
        int var6 = var5.length;

        int var7;
        String pkg;
        for(var7 = 0; var7 < var6; ++var7) {
            pkg = var5[var7];
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        var5 = annotationAttrs.getStringArray("basePackages");
        var6 = var5.length;

        for(var7 = 0; var7 < var6; ++var7) {
            pkg = var5[var7];
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        Class[] var9 = annotationAttrs.getClassArray("basePackageClasses");
        var6 = var9.length;

        for(var7 = 0; var7 < var6; ++var7) {
            Class<?> clazz = var9[var7];
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (CollectionUtils.isNotEmpty(basePackages)) {
            VFS.addImplClass(SpringBootVFS.class);
            BaseI18nService.i18nDomainMethodCache = ReflectionUtil.getReflectorsFromPackage(basePackages, BaseI18nDomain.class);
        } else {
            log.info("I18nDomainScan is not configured,if not use i18n interceptor,that's ok!");
        }

    }
}
