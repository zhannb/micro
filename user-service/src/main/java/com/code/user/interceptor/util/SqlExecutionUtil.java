package com.code.user.interceptor.util;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.code.user.interceptor.domain.BaseI18nDomain;
import com.code.user.interceptor.exception.SqlProcessInterceptorException;
import com.code.user.interceptor.service.BaseI18nService;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * create by liuliang
 * on 2019-08-08  11:13
 */
public class SqlExecutionUtil {
    public SqlExecutionUtil() {
    }

    public static List<Long> executeForIdsWithParameters(Connection connection, List<Object> valueList, String sql) {
        List<Long> idList = new ArrayList();
        if (CollectionUtils.isEmpty(valueList)) {
            return executeForIdsWithoutParameters(connection, sql);
        } else {
            try {
                PreparedStatement psm = connection.prepareStatement(sql);
                Throwable var5 = null;

                try {
                    for(int i = 0; i < valueList.size(); ++i) {
                        psm.setObject(i + 1, valueList.get(i));
                    }

                    ResultSet resultSet = psm.executeQuery();

                    while(resultSet.next()) {
                        idList.add(resultSet.getLong(1));
                    }
                } catch (Throwable var15) {
                    var5 = var15;
                    throw var15;
                } finally {
                    if (psm != null) {
                        if (var5 != null) {
                            try {
                                psm.close();
                            } catch (Throwable var14) {
                                var5.addSuppressed(var14);
                            }
                        } else {
                            psm.close();
                        }
                    }

                }
            } catch (SQLException var17) {
                var17.printStackTrace();
            }

            return idList;
        }
    }

    public static List<Long> executeForIdsWithoutParameters(Connection connection, String sql) {
        ArrayList idList = new ArrayList();

        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var4 = null;

            try {
                ResultSet resultSet = psm.executeQuery();

                while(resultSet.next()) {
                    idList.add(resultSet.getLong(1));
                }
            } catch (Throwable var14) {
                var4 = var14;
                throw var14;
            } finally {
                if (psm != null) {
                    if (var4 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var13) {
                            var4.addSuppressed(var13);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (SQLException var16) {
            var16.printStackTrace();
        }

        return idList;
    }

    public static void executeForNoResultWithoutParameters(Connection connection, String sql) {
        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var3 = null;

            try {
                psm.execute();
            } catch (Throwable var13) {
                var3 = var13;
                throw var13;
            } finally {
                if (psm != null) {
                    if (var3 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var12) {
                            var3.addSuppressed(var12);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (SQLException var15) {
            var15.printStackTrace();
        }

    }

    public static void executeForNoResultWithParameterId(Connection connection, String sql, Long id) {
        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var4 = null;

            try {
                psm.setLong(1, id);
                psm.execute();
            } catch (Throwable var14) {
                var4 = var14;
                throw var14;
            } finally {
                if (psm != null) {
                    if (var4 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var13) {
                            var4.addSuppressed(var13);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (SQLException var16) {
            var16.printStackTrace();
        }

    }

    public static void executeForNoResultWithManyParameters(Connection connection, String sql, List<Object> parameterList) {
        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var4 = null;

            try {
                if (CollectionUtils.isEmpty(parameterList)) {
                    executeForNoResultWithoutParameters(connection, sql);
                } else {
                    for(int i = 0; i < parameterList.size(); ++i) {
                        psm.setObject(i + 1, parameterList.get(i));
                    }

                    psm.execute();
                }
            } catch (Throwable var14) {
                var4 = var14;
                throw var14;
            } finally {
                if (psm != null) {
                    if (var4 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var13) {
                            var4.addSuppressed(var13);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (SQLException var16) {
            var16.printStackTrace();
        }

    }

    public static <T extends BaseI18nDomain> List executeForListWithManyParameters(Connection connection, String sql, List<Object> parameterList, Class domainClass, List<TableFieldInfo> tableFieldInfoList, T entity, List<String> i18nFieldList) {
        ArrayList objectList = new ArrayList();

        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var9 = null;

            try {
                for(int i = 0; i < parameterList.size(); ++i) {
                    psm.setObject(i + 1, parameterList.get(i));
                }

                ResultSet resultSet = psm.executeQuery();

                while(resultSet.next()) {
                    Object result = domainClass.newInstance();
                    if (null == BaseI18nService.i18nDomainMethodCache.get(domainClass)) {
                        throw new SqlProcessInterceptorException(domainClass.getName() + "尚未初始化,请检查!");
                    }

                    tableFieldInfoList.forEach((t) -> {
                        Invoker setMethodInvoker = ((Reflector)BaseI18nService.i18nDomainMethodCache.get(domainClass)).getSetInvoker(t.getProperty());

                        try {
                            if (setMethodInvoker instanceof MethodInvoker) {
                                ReflectionUtil.specificProcessInvoker((MethodInvoker)setMethodInvoker, resultSet, t.getProperty(), result, entity, i18nFieldList);
                            } else {
                                Object[] paramField = new Object[]{ReflectionUtil.isObjectNullOrStringBlank(resultSet.getObject(t.getProperty())) ? resultSet.getObject("base_" + t.getProperty()) : resultSet.getObject(t.getProperty())};
                                setMethodInvoker.invoke(result, paramField);
                            }
                        } catch (IllegalAccessException var8) {
                            var8.printStackTrace();
                        } catch (InvocationTargetException varn) {
                            varn.printStackTrace();
                        } catch (SQLException var10) {
                            var10.printStackTrace();
                        }

                    });
                    Invoker idSetMethodInvoker = ((Reflector)BaseI18nService.i18nDomainMethodCache.get(domainClass)).getSetInvoker("id");
                    Object[] paramId = new Object[]{resultSet.getLong("id")};
                    idSetMethodInvoker.invoke(result, paramId);
                    objectList.add(result);
                }
            } catch (Throwable var25) {
                var9 = var25;
                throw var25;
            } finally {
                if (psm != null) {
                    if (var9 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var24) {
                            var9.addSuppressed(var24);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (SQLException var27) {
            var27.printStackTrace();
        } catch (IllegalAccessException var28) {
            var28.printStackTrace();
        } catch (InstantiationException var29) {
            var29.printStackTrace();
        } catch (InvocationTargetException var30) {
            var30.printStackTrace();
        }

        return objectList;
    }

    public static Map executeForMapWithoutParameters(Connection connection, String sql, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList) {
        LinkedHashMap map = new LinkedHashMap();

        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var6 = null;

            try {
                ResultSet resultSet = psm.executeQuery();
                processResult(map, resultSet, tableFieldInfoList, i18nFieldList);
            } catch (Throwable var16) {
                var6 = var16;
                throw var16;
            } finally {
                if (psm != null) {
                    if (var6 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var15) {
                            var6.addSuppressed(var15);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (SQLException var18) {
            var18.printStackTrace();
        }

        return map;
    }

    public static Map executeForMapWithManyParameters(Connection connection, String sql, List<Object> parameterList, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList) {
        LinkedHashMap map = new LinkedHashMap();

        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var7 = null;

            try {
                for(int i = 0; i < parameterList.size(); ++i) {
                    psm.setObject(i + 1, parameterList.get(i));
                }

                ResultSet resultSet = psm.executeQuery();
                processResult(map, resultSet, tableFieldInfoList, i18nFieldList);
            } catch (Throwable var17) {
                var7 = var17;
                throw var17;
            } finally {
                if (psm != null) {
                    if (var7 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var16) {
                            var7.addSuppressed(var16);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (SQLException var19) {
            var19.printStackTrace();
        }

        return map;
    }

    private static void processResult(Map<Long, Map<String, Object>> map, ResultSet resultSet, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList) {
        while(true) {
            try {
                if (resultSet.next()) {
                    Map<String, Object> subMap = new HashMap();
                    Long id = resultSet.getLong("id");
                    Iterator var6;
                    TableFieldInfo tableFieldInfo;
                    if (LocaleContextHolder.getLocale().toString().equalsIgnoreCase(resultSet.getString("language"))) {
                        var6 = tableFieldInfoList.iterator();

                        while(var6.hasNext()) {
                            tableFieldInfo = (TableFieldInfo)var6.next();
                            subMap.put(tableFieldInfo.getProperty(), resultSet.getObject(tableFieldInfo.getProperty()));
                            if (i18nFieldList.contains(tableFieldInfo.getProperty())) {
                                subMap.put(tableFieldInfo.getProperty(), ReflectionUtil.isObjectNullOrStringBlank(resultSet.getObject(tableFieldInfo.getProperty())) ? resultSet.getObject("base_" + tableFieldInfo.getProperty()) : resultSet.getObject(tableFieldInfo.getProperty()));
                            }
                        }

                        map.put(id, subMap);
                        continue;
                    }

                    if (map.containsKey(id)) {
                        continue;
                    }

                    var6 = tableFieldInfoList.iterator();

                    while(var6.hasNext()) {
                        tableFieldInfo = (TableFieldInfo)var6.next();
                        subMap.put(tableFieldInfo.getProperty(), resultSet.getObject(tableFieldInfo.getProperty()));
                        if (i18nFieldList.contains(tableFieldInfo.getProperty())) {
                            subMap.put(tableFieldInfo.getProperty(), resultSet.getObject("base_" + tableFieldInfo.getProperty()));
                        }
                    }

                    map.put(id, subMap);
                    continue;
                }
            } catch (SQLException var8) {
                var8.printStackTrace();
            }

            return;
        }
    }
}
