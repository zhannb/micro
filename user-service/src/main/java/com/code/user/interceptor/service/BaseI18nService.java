package com.code.user.interceptor.service;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.TableInfoHelper;
import com.code.user.interceptor.annotation.I18nField;
import com.code.user.interceptor.domain.BaseI18nDomain;
import com.code.user.interceptor.domain.BaseI18nMetaData;
import com.code.user.interceptor.exception.SqlProcessInterceptorException;
import com.code.user.interceptor.plugin.I18nSqlProcessInterceptor;
import com.code.user.interceptor.util.ReflectionUtil;
import com.code.user.interceptor.util.SqlExecutionUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * create by liuliang
 * on 2019-08-08  10:56
 */
@Component
public class BaseI18nService {

    private static final Logger log = LoggerFactory.getLogger(BaseI18nService.class);
    private static final String ID_CONSTANT = "id";
    private static final String language = "'zh'";//默认语言是中文
    private DataSource dataSource;
    public static ConcurrentMap<Class<?>, Reflector> i18nDomainMethodCache = new ConcurrentHashMap();

    public BaseI18nService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T extends BaseI18nDomain> T convertOneByLocale(T entity) {
        if (entity == null) {
            return entity;
        } else {
            Locale locale = LocaleContextHolder.getLocale();

            try {
                Connection connection = this.dataSource.getConnection();
                Throwable var4 = null;

                BaseI18nDomain var13;
                try {
                    Class clazz = entity.getClass();
                    TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
                    if (tableInfo == null) {
                        throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
                    }

                    List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
                    Long baseTableId = (Long) ReflectionUtil.getMethodValue(entity, "id");
                    List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
                    StringBuilder sb = new StringBuilder("SELECT");
                    if (baseTableId == null) {
                        log.info("id必传!根据id和language匹配做翻译!");
                        return entity;
                    }

                    tableFieldInfoList.forEach((f) -> {
                        if (i18nFieldNameList.contains(f.getProperty())) {
                            sb.append(" base." + f.getColumn() + " AS base_" + f.getProperty() + ",i18n." + f.getColumn() + " AS " + f.getProperty() + ",");
                        } else if (f.getColumn().equals(f.getProperty())) {
                            sb.append(" base." + f.getColumn() + ",");
                        } else {
                            sb.append(" base." + f.getColumn() + " AS " + f.getProperty() + ",");
                        }

                    });
                    sb.append("base.id FROM ").append(tableInfo.getTableName() + " base ").append("LEFT JOIN ").append(tableInfo.getTableName() + "_i18n i18n ON base.id = i18n.id WHERE base.id = ? ").append("AND i18n.language = ?;");
                    List<Object> parameterList = new ArrayList();
                    parameterList.add(baseTableId);
                    parameterList.add(locale.toString());
                    List<Object> resultList = SqlExecutionUtil.executeForListWithManyParameters(connection, sb.toString(), parameterList, clazz, tableFieldInfoList, entity, i18nFieldNameList);
                    if (resultList.size() > 1) {
                        throw new SqlProcessInterceptorException("数据有问题,一个id和language至多匹配一条记录!");
                    }

                    if (resultList.size() != 1) {
                        log.info("未找到匹配的记录,将返回原entity");
                        return entity;
                    }

                    BeanUtils.copyProperties(entity, resultList.get(0), (String[])i18nFieldNameList.toArray(new String[0]));
                    var13 = (BaseI18nDomain)resultList.get(0);
                } catch (Throwable var24) {
                    var4 = var24;
                    throw var24;
                } finally {
                    if (connection != null) {
                        if (var4 != null) {
                            try {
                                connection.close();
                            } catch (Throwable var23) {
                                var4.addSuppressed(var23);
                            }
                        } else {
                            connection.close();
                        }
                    }

                }

                return (T) var13;
            } catch (SQLException var26) {
                var26.printStackTrace();
                return entity;
            }
        }
    }

    public <T extends BaseI18nDomain> List<T> convertListByLocale(List<T> entityList) {
        return CollectionUtils.isEmpty(entityList) ? entityList : (List)entityList.stream().map((e) -> {
            return this.convertOneByLocale(e);
        }).collect(Collectors.toList());
    }

    public <T extends BaseI18nDomain> Map<String, T> getI18nInfo(Long id, Class<T> clazz) {
        Object resultMap = new HashMap();

        try {
            Connection connection = this.dataSource.getConnection();
            Throwable var5 = null;

            try {
                TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
                List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
                List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
                StringBuilder sb = new StringBuilder("SELECT ");
                tableFieldInfoList.forEach((t) -> {
                    if (i18nFieldNameList.contains(t.getProperty())) {
                        sb.append(t.getColumn() + " AS " + t.getProperty() + ",");
                    }

                });
                sb.append("language FROM ").append(tableInfo.getTableName() + "_i18n").append(" WHERE id =?;");
                resultMap = this.getI18nInfoMap(connection, sb.toString(), id, clazz, i18nFieldNameList);
            } catch (Throwable var18) {
                var5 = var18;
                throw var18;
            } finally {
                if (connection != null) {
                    if (var5 != null) {
                        try {
                            connection.close();
                        } catch (Throwable var17) {
                            var5.addSuppressed(var17);
                        }
                    } else {
                        connection.close();
                    }
                }

            }
        } catch (SQLException var20) {
            var20.printStackTrace();
        }

        return (Map)resultMap;
    }

    private <T extends BaseI18nDomain> Map<String, T> getI18nInfoMap(Connection connection, String sql, Long id, Class clazz, List<String> i18nFieldNameList) {
        HashMap resultMap = new HashMap();

        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var8 = null;

            try {
                psm.setLong(1, id);
                ResultSet resultSet = psm.executeQuery();

                while(resultSet.next()) {
                    Object result = clazz.newInstance();
                    i18nFieldNameList.forEach((t) -> {
                        Invoker setMethodInvoker = ((Reflector)i18nDomainMethodCache.get(clazz)).getSetInvoker(t);

                        try {
                            if (setMethodInvoker instanceof MethodInvoker) {
                                ReflectionUtil.specificProcessInvoker((MethodInvoker)setMethodInvoker, resultSet, t, result, (BaseI18nDomain)null, i18nFieldNameList);
                            } else {
                                Object[] paramField = new Object[]{resultSet.getObject(t)};
                                setMethodInvoker.invoke(result, paramField);
                            }
                        } catch (IllegalAccessException var7) {
                            var7.printStackTrace();
                        } catch (InvocationTargetException varn) {
                            varn.printStackTrace();
                        } catch (SQLException var9) {
                            var9.printStackTrace();
                        }

                    });
                    resultMap.put(resultSet.getString("language"), (BaseI18nDomain)result);
                }
            } catch (Throwable var21) {
                var8 = var21;
                throw var21;
            } finally {
                if (psm != null) {
                    if (var8 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var20) {
                            var8.addSuppressed(var20);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (IllegalAccessException var23) {
            var23.printStackTrace();
        } catch (InstantiationException var24) {
            var24.printStackTrace();
        } catch (SQLException var25) {
            var25.printStackTrace();
        }

        return resultMap;
    }

    public <T extends BaseI18nDomain> List<T> selectListBaseTableInfoWithI18n(List<Long> idList, Class<T> clazz) {
        return (List)idList.stream().map((id) -> {
            return this.selectOneBaseTableInfoWithI18n(id, clazz);
        }).collect(Collectors.toList());
    }

    public <T extends BaseI18nDomain> T selectOneBaseTableInfoWithI18n(Long id, Class<T> clazz) {
        try {
            Connection connection = this.dataSource.getConnection();
            Throwable var4 = null;

            T var15 =null;

            try {
                TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
                if (tableInfo == null) {
                    throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
                }

                Object instance = (T)clazz.newInstance();
                List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
                List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
                StringBuilder sbBase = new StringBuilder("SELECT ");
                tableFieldInfoList.forEach((t) -> {
                    sbBase.append("base."+t.getColumn() + " AS `" + t.getProperty() + "`,");
                    if (i18nFieldNameList.contains(t.getProperty())) {
                        sbBase.append("i18n."+t.getColumn() + " AS base_" + t.getProperty() + ",");
                    }
                });
                sbBase.append("base.id FROM ").append(tableInfo.getTableName()+" base,"+tableInfo.getTableName()+"_i18n i18n" );
                sbBase.append(" WHERE base.id =? and base.id = i18n.id and i18n.language = "+language+";");
                List<Object> parameterList = new ArrayList();
                parameterList.add(id);
                List<Object> resultList = SqlExecutionUtil.executeForListWithManyParameters(connection, sbBase.toString(), parameterList, clazz, tableFieldInfoList, (BaseI18nDomain)null, i18nFieldNameList);
                if (resultList.size() > 1) {
                    throw new RuntimeException("数据有问题,一个id至多匹配一条记录!");
                }

                Map i18n;
                if (resultList.size() != 1) {
                    i18n = null;
                    return (T) i18n;
                }
                instance = (T)resultList.get(0);
                i18n = this.getI18nMap(clazz, id);
                Invoker setMethodInvoker = ((Reflector)i18nDomainMethodCache.get(clazz)).getSetInvoker("i18n");
                Object[] param = new Object[]{i18n};
                setMethodInvoker.invoke(instance, param);
                var15 = (T)instance;
            } catch (Throwable var29) {
                var4 = var29;
                throw var29;
            } finally {
                if (connection != null) {
                    if (var4 != null) {
                        try {
                            connection.close();
                        } catch (Throwable var28) {
                            var4.addSuppressed(var28);
                        }
                    } else {
                        connection.close();
                    }
                }

            }

            return (T) var15;
        } catch (SQLException var31) {
            var31.printStackTrace();
        } catch (InvocationTargetException var32) {
            var32.printStackTrace();
        } catch (IllegalAccessException var33) {
            var33.printStackTrace();
        } catch (InstantiationException var34) {
            var34.printStackTrace();
        }

        return null;
    }

    public <T extends BaseI18nDomain> List<T> selectListTranslatedTableInfoWithI18n(List<Long> idList, Class<T> clazz) {
        return (List)idList.stream().map((id) -> {
            return this.selectOneTranslatedTableInfoWithI18n(id, clazz);
        }).collect(Collectors.toList());
    }

    public <T extends BaseI18nDomain> T selectOneTranslatedTableInfoWithI18n(Long id, Class<T> clazz) {
        try {
            Connection connection = this.dataSource.getConnection();
            Throwable var4 = null;

            Map i18n;
            try {
                TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
                if (tableInfo == null) {
                    throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
                }

                BaseI18nDomain instance = (BaseI18nDomain)clazz.newInstance();
                List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
                List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
                StringBuilder sbBase = new StringBuilder("SELECT ");
                tableFieldInfoList.forEach((t) -> {
                    if (i18nFieldNameList.contains(t.getProperty())) {
                        sbBase.append("base." + t.getColumn() + " AS base_" + t.getProperty() + ",i18n." + t.getColumn() + " AS " + t.getProperty() + ",");
                    } else {
                        sbBase.append("base." + t.getColumn() + " AS " + t.getProperty() + ",");
                    }

                });
                sbBase.append("base.id,i18n.language FROM ").append(tableInfo.getTableName() + " base LEFT JOIN ").append(tableInfo.getTableName() + "_i18n i18n ON base.id = i18n.id").append(" WHERE base.id =?;");
                List<Object> parameterList = new ArrayList();
                parameterList.add(id);
                Map<Long, Map<String, Object>> map = SqlExecutionUtil.executeForMapWithManyParameters(connection, sbBase.toString(), parameterList, tableFieldInfoList, i18nFieldNameList);
                I18nSqlProcessInterceptor i18nSqlProcessInterceptor = new I18nSqlProcessInterceptor();
                List<Object> resultList = i18nSqlProcessInterceptor.convertBaseMap2List(clazz, map, (List)null, i18nFieldNameList);
                if (resultList.size() > 1) {
                    throw new RuntimeException("数据有问题,一个id至多匹配一条记录!");
                }

                if (resultList.size() == 1) {
                    instance = (BaseI18nDomain)resultList.get(0);
                    i18n = this.getI18nMap(clazz, id);
                    Invoker setMethodInvoker = ((Reflector)i18nDomainMethodCache.get(clazz)).getSetInvoker("i18n");
                    Object[] param = new Object[]{i18n};
                    setMethodInvoker.invoke(instance, param);
                    BaseI18nDomain var17 = instance;
                    return  (T) var17;
                }

                i18n = null;
            } catch (Throwable var31) {
                var4 = var31;
                throw var31;
            } finally {
                if (connection != null) {
                    if (var4 != null) {
                        try {
                            connection.close();
                        } catch (Throwable var30) {
                            var4.addSuppressed(var30);
                        }
                    } else {
                        connection.close();
                    }
                }

            }

            return  (T) i18n;
        } catch (SQLException var33) {
            var33.printStackTrace();
        } catch (IllegalAccessException var34) {
            var34.printStackTrace();
        } catch (InstantiationException var35) {
            var35.printStackTrace();
        } catch (InvocationTargetException var36) {
            var36.printStackTrace();
        }

        return null;
    }

    public <T extends BaseI18nDomain> T selectOneTranslatedTableInfoWithI18nByEntity(T entity, Class<T> clazz) {
        try {
            Connection connection = this.dataSource.getConnection();
            Throwable var4 = null;

            Map i18n;
            try {
                TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
                if (tableInfo == null) {
                    throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
                }

                Long baseTableId = (Long)ReflectionUtil.getMethodValue(entity, "id");
                if (baseTableId == null) {
                    log.info("id必传!根据id和language匹配做翻译!");
                    return null;
                }

                T instance = (T) clazz.newInstance();
                List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
                List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
                StringBuilder sbBase = new StringBuilder("SELECT ");
                tableFieldInfoList.forEach((t) -> {
                    if (i18nFieldNameList.contains(t.getProperty())) {
                        sbBase.append("base." + t.getColumn() + " AS base_" + t.getProperty() + ",i18n." + t.getColumn() + " AS " + t.getProperty() + ",");
                    } else {
                        sbBase.append("base." + t.getColumn() + " AS " + t.getProperty() + ",");
                    }

                });
                sbBase.append("base.id,i18n.language FROM ").append(tableInfo.getTableName() + " base LEFT JOIN ").append(tableInfo.getTableName() + "_i18n i18n ON base.id = i18n.id").append(" WHERE base.id =?;");
                List<Object> parameterList = new ArrayList();
                parameterList.add(baseTableId);
                Map<Long, Map<String, Object>> map = SqlExecutionUtil.executeForMapWithManyParameters(connection, sbBase.toString(), parameterList, tableFieldInfoList, i18nFieldNameList);
                I18nSqlProcessInterceptor i18nSqlProcessInterceptor = new I18nSqlProcessInterceptor();
                List<Object> resultList = i18nSqlProcessInterceptor.convertBaseMap2List(clazz, map, (List)null, i18nFieldNameList);
                if (resultList.size() > 1) {
                    throw new RuntimeException("数据有问题,一个id至多匹配一条记录!");
                }

                if (resultList.size() == 1) {
                    instance = (T)resultList.get(0);
                    BeanUtils.copyProperties(entity, instance, (String[])i18nFieldNameList.toArray(new String[0]));
                    i18n = this.getI18nMap(clazz, baseTableId);
                    Invoker setMethodInvoker = ((Reflector)i18nDomainMethodCache.get(clazz)).getSetInvoker("i18n");
                    Object[] param = new Object[]{i18n};
                    setMethodInvoker.invoke(instance, param);
                    BaseI18nDomain var18 = instance;
                    return  (T) var18;
                }

                log.info("未找到匹配的记录,将返回null!");
                i18n = null;
            } catch (Throwable var33) {
                var4 = var33;
                throw var33;
            } finally {
                if (connection != null) {
                    if (var4 != null) {
                        try {
                            connection.close();
                        } catch (Throwable var32) {
                            var4.addSuppressed(var32);
                        }
                    } else {
                        connection.close();
                    }
                }

            }

            return (T) i18n;
        } catch (SQLException var35) {
            var35.printStackTrace();
        } catch (IllegalAccessException var36) {
            var36.printStackTrace();
        } catch (InstantiationException var37) {
            var37.printStackTrace();
        } catch (InvocationTargetException var38) {
            var38.printStackTrace();
        }

        return null;
    }

    public <T extends BaseI18nDomain> List<T> selectListTranslatedTableInfoWithI18nByEntity(List<T> entityList, Class<T> clazz) {
        return (List)entityList.stream().map((entity) -> {
            return this.selectOneTranslatedTableInfoWithI18nByEntity(entity, clazz);
        }).collect(Collectors.toList());
    }

    public Map<String, List<Map<String, String>>> getI18nMap(Class<? extends BaseI18nDomain> clazz, Long id) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
        if (tableInfo == null) {
            throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
        } else {
            List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
            List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
            List<Object> parameterList = new ArrayList();
            parameterList.add(id);

            try {
                Connection connection = this.dataSource.getConnection();
                Throwable var8 = null;

                try {
                    StringBuilder sbI18n = new StringBuilder("SELECT ");
                    tableFieldInfoList.forEach((t) -> {
                        if (i18nFieldNameList.contains(t.getProperty())) {
                            sbI18n.append("`"+t.getColumn() + "` AS `" + t.getProperty() + "`,");
                        }

                    });
                    sbI18n.append("language FROM ").append(tableInfo.getTableName() + "_i18n").append(" WHERE id =?;");

                    try {
                        PreparedStatement psm = connection.prepareStatement(sbI18n.toString());
                        Throwable var11 = null;

                        try {
                            for(int i = 0; i < parameterList.size(); ++i) {
                                psm.setObject(i + 1, parameterList.get(i));
                            }

                            ResultSet resultSet = psm.executeQuery();
                            ArrayList baseI18nMetaDataList = new ArrayList();

                            while(resultSet.next()) {
                                String language = resultSet.getString("language");
                                i18nFieldNameList.forEach((t) -> {
                                    try {
                                        baseI18nMetaDataList.add(BaseI18nMetaData.builder().language(language).field(t).value(resultSet.getObject(t) == null ? null : String.valueOf(resultSet.getObject(t))).build());
                                    } catch (SQLException var5) {
                                        var5.printStackTrace();
                                    }

                                });
                            }

                            Map<String, List<Map<String, String>>> map = this.convertList2Map(baseI18nMetaDataList);
                            Map var15 = map;
                            return var15;
                        } catch (Throwable var43) {
                            var11 = var43;
                            throw var43;
                        } finally {
                            if (psm != null) {
                                if (var11 != null) {
                                    try {
                                        psm.close();
                                    } catch (Throwable var42) {
                                        var11.addSuppressed(var42);
                                    }
                                } else {
                                    psm.close();
                                }
                            }

                        }
                    } catch (SQLException var45) {
                        var45.printStackTrace();
                        return null;
                    }
                } catch (Throwable var46) {
                    var8 = var46;
                    throw var46;
                } finally {
                    if (connection != null) {
                        if (var8 != null) {
                            try {
                                connection.close();
                            } catch (Throwable var41) {
                                var8.addSuppressed(var41);
                            }
                        } else {
                            connection.close();
                        }
                    }

                }
            } catch (SQLException var48) {
                var48.printStackTrace();
                return null;
            }
        }
    }

    private Map convertList2Map(List<BaseI18nMetaData> baseI18nMetaDataList) {
        Map<String, List<HashMap<String, String>>> map = new HashMap();
        Iterator var3 = baseI18nMetaDataList.iterator();

        while(var3.hasNext()) {
            BaseI18nMetaData baseI18nMetaData = (BaseI18nMetaData)var3.next();
            String field = baseI18nMetaData.getField();
            List<HashMap<String, String>> subList;
            if (map.containsKey(field)) {
                subList = (List)map.get(field);
            } else {
                subList = new ArrayList();
                map.put(field, subList);
            }

            if (baseI18nMetaData.getValue() != null) {
                Map elementMap = new HashMap();
                elementMap.put("language", baseI18nMetaData.getLanguage());
                elementMap.put("value", baseI18nMetaData.getValue());
                ((List)subList).add(elementMap);
            }
        }

        return map;
    }

    public void insertOrUpdateI18n(Map<String, List<Map<String, String>>> originDataMap, Class<?> clazz, Long id) {
        List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
        TableName tableName = (TableName)clazz.getAnnotation(TableName.class);
        if (tableName != null && !StringUtils.isEmpty(tableName.value())) {
            try {
                Connection connection = this.dataSource.getConnection();
                Throwable var7 = null;

                try {
                    List<BaseI18nMetaData> baseI18nMetaDataList = new ArrayList();
                    Map<String, Map<String, String>> metaDataMap = new HashMap();
                    if (originDataMap != null) {
                        originDataMap.forEach((k, v) -> {
                            v.forEach((p) -> {
                                if (i18nFieldNameList.contains(k)) {
                                    baseI18nMetaDataList.add(BaseI18nMetaData.builder().field(k).language((String)p.get("language")).value((String)p.get("value")).build());
                                }

                            });
                        });
                        baseI18nMetaDataList.forEach((b) -> {
                            String language = b.getLanguage();
                            Map map = metaDataMap.keySet().contains(language) ? (Map)metaDataMap.get(language) : new HashMap();
                            ((Map)map).put(b.getField(), b.getValue());
                            metaDataMap.put(language, map);
                        });
                    }

                    I18nSqlProcessInterceptor i18nSqlProcessInterceptor = new I18nSqlProcessInterceptor();
                    i18nSqlProcessInterceptor.execInsertOrUpdateList(metaDataMap, tableName.value(), connection, id);
                } catch (Throwable var19) {
                    var7 = var19;
                    throw var19;
                } finally {
                    if (connection != null) {
                        if (var7 != null) {
                            try {
                                connection.close();
                            } catch (Throwable var18) {
                                var7.addSuppressed(var18);
                            }
                        } else {
                            connection.close();
                        }
                    }

                }
            } catch (SQLException var21) {
                var21.printStackTrace();
            }

        }
    }
}
