package com.code.user.interceptor.util;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.code.user.interceptor.domain.BaseI18nDomain;
import com.code.user.interceptor.enums.MethodPrefixEnum;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * create by liuliang
 * on 2019-08-08  10:59
 */
public class ReflectionUtil {
    private static final Logger log = LoggerFactory.getLogger(ReflectionUtil.class);
    private static final String TYPE_NAME_PREFIX = "class ";

    public ReflectionUtil() {
    }

    public static String getMethodCapitalize(Field field, MethodPrefixEnum methodPrefixEnum, String str) {
        Class<?> fieldType = field.getType();
        Boolean flag = fieldType != null && (Boolean.TYPE.isAssignableFrom(fieldType) || Boolean.class.isAssignableFrom(fieldType));
        if (flag && str.startsWith("is")) {
            String propertyName = str.replaceFirst("is", "");
            if (StringUtils.isEmpty(propertyName)) {
                str = propertyName;
            } else {
                String beforeChar = propertyName.substring(0, 1).toLowerCase();
                String afterChar = propertyName.substring(1, propertyName.length());
                String firstCharToLowerStr = beforeChar + afterChar;
                str = propertyName.equals(firstCharToLowerStr) ? propertyName : firstCharToLowerStr;
            }
        }

        switch(methodPrefixEnum) {
            case GET:
                return StringUtils.concatCapitalize(Boolean.TYPE.equals(fieldType) ? "is" : "get", str);
            case SET:
                return StringUtils.concatCapitalize("set", str);
            default:
                throw new ReflectionException("Only support reflect get/set method!");
        }
    }

    public static String getMethodCapitalizeByFieldName(Class clazz, String fieldName, MethodPrefixEnum methodPrefixEnum, String str) {
        try {
            Field field = clazz.getField(fieldName);
            return getMethodCapitalize(field, methodPrefixEnum, str);
        } catch (NoSuchFieldException var5) {
            var5.printStackTrace();
            throw new ReflectionException("no such field in clazz!");
        }
    }

    public static String methodNameCapitalize(MethodPrefixEnum methodPrefixEnum, String str) {
        return StringUtils.concatCapitalize(methodPrefixEnum.getPrefix(), str);
    }

    public static Object getMethodValue(Class<?> cls, Object entity, String str) {
        Map fieldMaps = getFieldMap(cls);

        try {
            if (ObjectUtils.isEmpty(fieldMaps)) {
                throw new ReflectionException(String.format("Error: NoSuchField in %s for %s.  Cause:", cls.getSimpleName(), str));
            } else {
                Method method = cls.getMethod(getMethodCapitalize((Field)fieldMaps.get(str), MethodPrefixEnum.GET, str));
                return method.invoke(entity);
            }
        } catch (NoSuchMethodException var5) {
            throw new ReflectionException(String.format("Error: NoSuchMethod in %s.  Cause:", cls.getSimpleName()) + var5);
        } catch (IllegalAccessException var6) {
            throw new ReflectionException(String.format("Error: Cannot execute a private method. in %s.  Cause:", cls.getSimpleName()) + var6);
        } catch (InvocationTargetException var7) {
            throw new ReflectionException("Error: InvocationTargetException on getMethodValue.  Cause:" + var7);
        }
    }

    public static Object getMethodValue(Object entity, String str) {
        return null == entity ? null : getMethodValue(entity.getClass(), entity, str);
    }

    public static Class getSuperClassGenericType(Class clazz, int index) {
        Type genType = clazz.getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)) {
            log.warn(String.format("Warn: %s's superclass not ParameterizedType", clazz.getSimpleName()));
            return Object.class;
        } else {
            Type[] params = ((ParameterizedType)genType).getActualTypeArguments();
            if (index < params.length && index >= 0) {
                if (!(params[index] instanceof Class)) {
                    log.warn(String.format("Warn: %s not set the actual class on superclass generic parameter", clazz.getSimpleName()));
                    return Object.class;
                } else {
                    return (Class)params[index];
                }
            } else {
                log.warn(String.format("Warn: Index: %s, Size of %s's Parameterized Type: %s .", index, clazz.getSimpleName(), params.length));
                return Object.class;
            }
        }
    }

    public static Map<String, Field> getFieldMap(Class<?> clazz) {
        List<Field> fieldList = getFieldList(clazz);
        Map<String, Field> fieldMap = Collections.emptyMap();
        if (CollectionUtils.isNotEmpty(fieldList)) {
            fieldMap = new LinkedHashMap();
            Iterator var3 = fieldList.iterator();

            while(var3.hasNext()) {
                Field field = (Field)var3.next();
                ((Map)fieldMap).put(field.getName(), field);
            }
        }

        return (Map)fieldMap;
    }

    public static List<Field> getFieldList(Class<?> clazz) {
        if (null == clazz) {
            return null;
        } else {
            List<Field> fieldList = new LinkedList();
            Field[] fields = clazz.getDeclaredFields();
            Field[] var3 = fields;
            int var4 = fields.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Field field = var3[var5];
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                    fieldList.add(field);
                }
            }

            Class<?> superClass = clazz.getSuperclass();
            if (superClass.equals(Object.class)) {
                return fieldList;
            } else {
                return excludeOverrideSuperField(fieldList, getFieldList(superClass));
            }
        }
    }

    public static List<Field> excludeOverrideSuperField(List<Field> fieldList, List<Field> superFieldList) {
        Map<String, Field> fieldMap = new HashMap();
        Iterator var3 = fieldList.iterator();

        Field superField;
        while(var3.hasNext()) {
            superField = (Field)var3.next();
            fieldMap.put(superField.getName(), superField);
        }

        var3 = superFieldList.iterator();

        while(var3.hasNext()) {
            superField = (Field)var3.next();
            if (null == fieldMap.get(superField.getName())) {
                fieldList.add(superField);
            }
        }

        return fieldList;
    }

    public static Type[] getParameterizedTypes(Object object) {
        Type superclassType = object.getClass().getGenericSuperclass();
        return !ParameterizedType.class.isAssignableFrom(superclassType.getClass()) ? null : ((ParameterizedType)superclassType).getActualTypeArguments();
    }

    public static String getClassName(Type type) {
        if (type == null) {
            return "";
        } else {
            String className = type.toString();
            if (className.startsWith("class ")) {
                className = className.substring("class ".length());
            }

            return className;
        }
    }

    public static Class<?> getClass(Type type) throws ClassNotFoundException {
        String className = getClassName(type);
        return className != null && !className.isEmpty() ? Class.forName(className) : null;
    }

    public static Class<?> extractModelClass(Class<?> mapperClass) {
        if (mapperClass == BaseMapper.class) {
            log.warn(" Current Class is BaseMapper ");
            return null;
        } else {
            Type[] types = mapperClass.getGenericInterfaces();
            ParameterizedType target = null;
            Type[] var3 = types;
            int var4 = types.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Type type = var3[var5];
                if (type instanceof ParameterizedType && BaseMapper.class.isAssignableFrom(mapperClass)) {
                    target = (ParameterizedType)type;
                    break;
                }
            }

            return target == null ? null : (Class)target.getActualTypeArguments()[0];
        }
    }

    public static List<String> getSpecificAnnotationFieldNameList(Class clazz, Class specificAnnotationClass) {
        List list = new ArrayList();
        Field[] fields = clazz.getDeclaredFields();
        Field[] var4 = fields;
        int var5 = fields.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Field field = var4[var6];
            if (field.isAnnotationPresent(specificAnnotationClass)) {
                list.add(field.getName());
            }
        }

        return list;
    }

    public static ConcurrentMap<Class<?>, Reflector> getReflectorsFromPackage(List<String> packagePath, Class supClazz) {
        if (CollectionUtils.isEmpty(packagePath)) {
            throw new ReflectionException("No i18n package is found!");
        } else {
            ConcurrentHashMap<Class<?>, Reflector> map = new ConcurrentHashMap();
            packagePath.stream().forEach((p) -> {
                List<Class<?>> classList = PackageScanUtil.getClassFromSuperClass(p, supClazz);
                if (CollectionUtils.isEmpty(classList)) {
                    log.warn("PackagePath: " + p + ",Can not find specific class which needs to be initialized!");
                }

                classList.forEach((clz) -> {
                    Reflector var10000 = (Reflector)map.put(clz, new Reflector(clz));
                });
            });
            return map;
        }
    }

    public static <T extends BaseI18nDomain> void specificProcessInvoker(MethodInvoker setMethodInvoker, Object data, String property, Object result, T entity, List<String> i18nFieldList) {
        try {
            Field methodField = setMethodInvoker.getClass().getDeclaredField("method");
            methodField.setAccessible(true);
            Method method = (Method)methodField.get(setMethodInvoker);
            Class parameterClazz = method.getParameterTypes()[0];
            Object[] paramField;
            if (data instanceof ResultSet) {
                ResultSet resultSet = (ResultSet)data;
                if (parameterClazz == UUID.class) {
                    if (resultSet.getObject(property) == null) {
                        paramField = new Object[]{null};
                    } else {
                        paramField = new Object[]{UUID.fromString((String)resultSet.getObject(property))};
                    }

                    setMethodInvoker.invoke(result, paramField);
                }
//                else if (parameterClazz == DateTime.class) {
//                    if (resultSet.getObject(property) == null) {
//                        paramField = new Object[]{null};
//                    } else {
//                        paramField = new Object[]{new DateTime(resultSet.getObject(property))};
//                    }
//                }
                else if (parameterClazz == ZonedDateTime.class) {
                    if (resultSet.getObject(property) == null) {
                        paramField = new Object[]{null};
                    } else {
                        paramField = new Object[]{ZonedDateTime.ofInstant(resultSet.getTimestamp(property, Calendar.getInstance()).toInstant(), ZoneId.systemDefault())};
                    }
                } else if (i18nFieldList.contains(property)) {
                    paramField = new Object[]{isObjectNullOrStringBlank(resultSet.getObject(property)) ? resultSet.getObject("base_" + property) : resultSet.getObject(property)};
                } else {
                    paramField = new Object[]{resultSet.getObject(property)};
                }
            } else if (parameterClazz == UUID.class) {
                if (data == null) {
                    paramField = new Object[]{null};
                } else {
                    paramField = new Object[]{UUID.fromString(String.valueOf(data))};
                }
            }
//            else if (parameterClazz == DateTime.class) {
//                if (data == null) {
//                    paramField = new Object[]{null};
//                } else {
//                    paramField = new Object[]{new DateTime(data)};
//                }
//            }
            else if (parameterClazz == ZonedDateTime.class) {
                if (data == null) {
                    paramField = new Object[]{null};
                } else {
                    paramField = new Object[]{ZonedDateTime.ofInstant(((Timestamp)data).toInstant(), ZoneId.systemDefault())};
                }
            } else {
                paramField = new Object[]{data};
            }

            try {
                setMethodInvoker.invoke(result, paramField);
            } catch (Exception var12) {
                log.warn("Parameter mismatch,value: {},property: {}", paramField, property);
                if (entity == null) {
                    setMethodInvoker.invoke(result, new Object[]{null});
                } else {
                    Object[] o = new Object[]{getMethodValue(entity, property)};
                    setMethodInvoker.invoke(result, o);
                }
            }
        } catch (NoSuchFieldException var13) {
            var13.printStackTrace();
        } catch (IllegalAccessException var14) {
            var14.printStackTrace();
        } catch (SQLException var15) {
            var15.printStackTrace();
        } catch (InvocationTargetException var16) {
            var16.printStackTrace();
        }

    }

    public static boolean isObjectNullOrStringBlank(Object o) {
        if (o == null) {
            return true;
        } else {
            return o instanceof String ? org.apache.commons.lang.StringUtils.isBlank((String)o) : false;
        }
    }
}
