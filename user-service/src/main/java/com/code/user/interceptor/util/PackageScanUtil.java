package com.code.user.interceptor.util;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.ibatis.io.ResolverUtil;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * create by liuliang
 * on 2019-08-08  11:05
 */
public class PackageScanUtil {
    public PackageScanUtil() {
    }

    public static List<Class<?>> getClassFromSuperClass(String packageName, Class<?> superType) {
        List<Class<?>> list = new ArrayList();
        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil();
        resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
        Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
        if (CollectionUtils.isNotEmpty(mapperSet)) {
            list.addAll(mapperSet);
        }

        return list;
    }

    public static List<Class<?>> getClassWithAnnotation(String packageName, Class<? extends Annotation> annotationType) {
        List<Class<?>> list = new ArrayList();
        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil();
        resolverUtil.findAnnotated(annotationType, new String[]{packageName});
        Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
        if (CollectionUtils.isNotEmpty(mapperSet)) {
            list.addAll(mapperSet);
        }

        return list;
    }

    public static List<Class<?>> getClassImplementClass(String packageName, Class<?> interfaceType) {
        List<Class<?>> list = new ArrayList();
        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil();
        resolverUtil.findImplementations(interfaceType, new String[]{packageName});
        Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
        if (CollectionUtils.isNotEmpty(mapperSet)) {
            list.addAll(mapperSet);
        }

        return list;
    }
}
