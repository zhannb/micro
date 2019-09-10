package com.code.user.interceptor.plugin;


import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisDefaultParameterHandler;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import com.code.user.interceptor.annotation.I18nField;
import com.code.user.interceptor.domain.BaseI18nDomain;
import com.code.user.interceptor.domain.BaseI18nMetaData;
import com.code.user.interceptor.exception.SqlProcessInterceptorException;
import com.code.user.interceptor.service.BaseI18nService;
import com.code.user.interceptor.util.MapUtil;
import com.code.user.interceptor.util.ReflectionUtil;
import com.code.user.interceptor.util.SqlExecutionUtil;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * create by liuliang
 * on 2019-08-08  10:51
 */

@Intercepts({@Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
), @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
)})
public class I18nSqlProcessInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(I18nSqlProcessInterceptor.class);


    private static final String ID_CONSTANT = "id";
    private static final String LANGUAGE_CONSTANT = "language";
    private static final String VALUE_CONSTANT = "value";
    private static final String DELIMITER_DOT = ".";
    private String dialectType;
    private String dialectClazz;
    private boolean overflowCurrent = false;
    private static final EnumMap<SqlCommandType, String[]> supportedOperationMap = new EnumMap(SqlCommandType.class);

    public I18nSqlProcessInterceptor() {
    }

    public Object intercept(Invocation invocation) throws Throwable {
        Object parameter = invocation.getArgs()[1];
        MappedStatement ms = (MappedStatement)invocation.getArgs()[0];
        String baseMethodStr = ms.getId().substring(ms.getId().lastIndexOf(".") + 1);
        Class parameterClass = parameter != null ? parameter.getClass() : null;
        Executor executor = (Executor)invocation.getTarget();
        Connection connection = executor.getTransaction().getConnection();
        Class domainClass = null;
        TableInfo tableInfo = new TableInfo();

        try {
            domainClass = ReflectionUtil.extractModelClass(Class.forName(ms.getId().substring(0, ms.getId().lastIndexOf("."))));
            Iterator iterator = BaseI18nService.i18nDomainMethodCache.keySet().iterator();

            while(iterator.hasNext()) {
                Class<?> next = (Class)iterator.next();
                if (domainClass.toString().equals(next.toString())) {
                    domainClass = next;
                    break;
                }
            }

            if (domainClass != null) {
                tableInfo = TableInfoHelper.getTableInfo(domainClass);
            }
        } catch (ClassNotFoundException var26) {
            log.warn("Mapper: {}, 未使用mybatis-plus的方式注入Mapper,找不到entity类", ms.getId());
        }

        if (domainClass == null) {
            return invocation.proceed();
        } else {
            Object baseResult = null;
            List i18nFieldList;
            Object ew;
            BoundSql boundSql;
            String baseSql;
            String baseTableName;
            Object entity;
            String deleteSql;
            List idList;
            List<ParameterMapping> parameterMappingList;
            List<ParameterMapping> parametersStrList;
            switch(ms.getSqlCommandType()) {
                case INSERT:
                    baseResult = invocation.proceed();
                    if (this.methodSupported(SqlCommandType.INSERT, baseMethodStr) && BaseI18nDomain.class.isAssignableFrom(domainClass)) {
                        List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(parameterClass, I18nField.class);
                        if (BaseI18nDomain.class.isAssignableFrom(parameterClass) && CollectionUtils.isNotEmpty(i18nFieldNameList)) {
                            baseSql = tableInfo.getTableName();
                            Map<String, Map<String, String>> metaDataMap = this.constructMetaDataMap(invocation.getArgs()[1], i18nFieldNameList);
                            this.execInsertOrUpdateList(metaDataMap, baseSql, connection, (Long)ReflectionUtil.getMethodValue(parameter, "id"));
                        }
                    }
                    break;
                case UPDATE:
                    boundSql = ms.getSqlSource().getBoundSql(parameter);
                    if (this.methodSupported(SqlCommandType.UPDATE, baseMethodStr) && BaseI18nDomain.class.isAssignableFrom(domainClass)) {
                        if (Map.class.isAssignableFrom(boundSql.getParameterObject().getClass()) && Map.class.isAssignableFrom(parameterClass)) {
                            MapperMethod.ParamMap parameterMap = (MapperMethod.ParamMap)boundSql.getParameterObject();
                            if (parameterMap.containsKey("ew")) {
                                Object entityWrapper = parameterMap.get("ew");
                                if (entityWrapper == null) {
                                    return invocation.proceed();
                                }

                                entity = ((QueryWrapper)entityWrapper).getEntity();
                                Map<String, String> paramValuePairs = ((QueryWrapper)entityWrapper).getParamNameValuePairs();
                                List<Object> valueList = new ArrayList();
//                                if (entity != null) {
//                                    parametersStrList = boundSql.getParameterMappings();
//                                    List<String> parametersStrList = (List)parametersStrList.stream().map((s) -> {
//                                        return s.getProperty();
//                                    }).collect(Collectors.toList());
//                                    valueList.addAll((Collection)((List)parametersStrList.stream().filter((s) -> {
//                                        return s.contains("ew.entity");
//                                    }).map((s) -> {
//                                        return s.substring(s.lastIndexOf(".") + 1);
//                                    }).collect(Collectors.toList())).stream().map((p) -> {
//                                        return ReflectionUtil.getMethodValue(entity, p);
//                                    }).collect(Collectors.toList()));
//                                }

                                if (paramValuePairs.size() > 0) {
                                    Map<String, String> sortedMap = paramValuePairs;
                                    valueList.addAll((Collection)sortedMap.values().stream().collect(Collectors.toList()));
                                }

                                String conditionStr = this.getSqlFromBaseSql(tableInfo, boundSql.getSql(), SqlCommandType.UPDATE, (List)null, (List)null, (String)null);
                                StringBuilder selectIdSb = (new StringBuilder("SELECT id FROM ")).append(tableInfo.getTableName()).append(" ").append(conditionStr).append(";");
                                idList = SqlExecutionUtil.executeForIdsWithParameters(connection, valueList, selectIdSb.toString());
                                if (CollectionUtils.isNotEmpty(idList)) {
                                    Object baseEntity = parameterMap.get("et");
                                    Class baseEntityClass = baseEntity.getClass();
                                    idList.forEach((idx) -> {
                                        List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(baseEntityClass, I18nField.class);
                                        Map<String, Map<String, String>> metaDataMap = this.constructMetaDataMap(baseEntity, i18nFieldNameList);
//                                        this.execInsertOrUpdateList(metaDataMap, tableInfo.getTableName(), connection, idx);
                                    });
                                }
                            } else if (parameterMap.containsKey("et") && BaseI18nDomain.class.isAssignableFrom(parameterMap.get("et").getClass())) {
                                baseTableName = tableInfo.getTableName();
                                Long baseTableId = (Long)ReflectionUtil.getMethodValue(parameterMap.get("et"), "id");
                                if (baseTableId != null) {
                                    List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(parameterMap.get("et").getClass(), I18nField.class);
                                    Map<String, Map<String, String>> metaDataMap = this.constructMetaDataMap(parameterMap.get("et"), i18nFieldNameList);
                                    this.execInsertOrUpdateList(metaDataMap, baseTableName, connection, baseTableId);
                                } else {
                                    log.info("Can not find the value of id of parameter 'entity'!");
                                }
                            }
                        } else {
                            log.info("Parameter'class: " + parameterClass.getName() + ",i18n interceptor is not supported for this api now!");
                        }
                    }

                    baseResult = invocation.proceed();
                    break;
                case DELETE:
//                    if (this.methodSupported(SqlCommandType.DELETE, baseMethodStr) && BaseI18nDomain.class.isAssignableFrom(domainClass)) {
//                        if (Map.class.isAssignableFrom(parameterClass)) {
//                            boundSql = ms.getSqlSource().getBoundSql(parameter);
//                            HashMap parameterMap = (HashMap)boundSql.getParameterObject();
//                            i18nFieldList = boundSql.getParameterMappings();
//                            entity = new ArrayList();
//                            if (parameterMap.containsKey("ew")) {
//                                Object entityWrapper = parameterMap.get("ew");
//                                if (entityWrapper == null) {
//                                    return invocation.proceed();
//                                }
//
//                                ew = ((QueryWrapper)entityWrapper).getEntity();
//                                parameterMappingList = (List)i18nFieldList.stream().map((s) -> {
//                                    return s.getProperty().substring(s.getProperty().lastIndexOf(".") + 1);
//                                }).collect(Collectors.toList());
//                                entity = (List)parameterMappingList.stream().map((p) -> {
//                                    return ReflectionUtil.getMethodValue(ew, p);
//                                }).collect(Collectors.toList());
//                            } else if (parameterMap.containsKey("cm")) {
//                                HashMap<String, Object> entity = (HashMap)parameterMap.get("cm");
//                                idList = (List)i18nFieldList.stream().map((s) -> {
//                                    return s.getProperty().substring(s.getProperty().indexOf("[") + 1, s.getProperty().indexOf("]"));
//                                }).collect(Collectors.toList());
//                                entity = (List)idList.stream().map((p) -> {
//                                    return entity.get(p);
//                                }).collect(Collectors.toList());
//                            }
//
//                            deleteSql = this.getSqlFromBaseSql(tableInfo, boundSql.getSql(), SqlCommandType.DELETE, (List)null, (List)null, (String)null);
//                            SqlExecutionUtil.executeForNoResultWithManyParameters(connection, deleteSql, (List)entity);
//                        } else if (Long.class.isAssignableFrom(parameterClass)) {
//                            boundSql = ms.getSqlSource().getBoundSql(parameter);
//                            Long id = (Long)boundSql.getParameterObject();
//                            baseTableName = this.getSqlFromBaseSql(tableInfo, boundSql.getSql(), SqlCommandType.DELETE, (List)null, (List)null, (String)null);
//                            SqlExecutionUtil.executeForNoResultWithParameterId(connection, baseTableName, id);
//                        } else {
//                            log.info("parameter'class: " + parameterClass.getName() + ",i18n interceptor is not supported for this api now!");
//                        }
//                    }
//
//                    baseResult = invocation.proceed();
                    break;
                case SELECT:
                    if (this.methodSupported(SqlCommandType.SELECT, baseMethodStr) && BaseI18nDomain.class.isAssignableFrom(domainClass)) {
                        boundSql = ms.getSqlSource().getBoundSql(parameter);
                        baseSql = boundSql.getSql();
                        i18nFieldList = ReflectionUtil.getSpecificAnnotationFieldNameList(domainClass, I18nField.class);
                        List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
                        if (Long.class.isAssignableFrom(parameterClass)) {
                            deleteSql = this.getSqlFromBaseSql(tableInfo, baseSql, SqlCommandType.SELECT, tableFieldInfoList, i18nFieldList, (String)null);
                            List<Object> parameterList = new ArrayList();
                            parameterList.add(parameter);
                            parameterList.add(LocaleContextHolder.getLocale().toString());
                            parameterMappingList = SqlExecutionUtil.executeForListWithManyParameters(connection, deleteSql, parameterList, domainClass, tableFieldInfoList, (BaseI18nDomain)null, i18nFieldList);
                            if (CollectionUtils.isNotEmpty(parameterMappingList)) {
                                return parameterMappingList;
                            }
                        } else if (Map.class.isAssignableFrom(parameterClass)) {
                            MapperMethod.ParamMap parameterMap = (MapperMethod.ParamMap)boundSql.getParameterObject();
                            ew = parameterMap.get("ew");
                            if (ew == null) {
                                log.info("I18n interceptor not support select without Entitywrapper temporarily!");
                                return invocation.proceed();
                            }

                            List valueList;
//                            List idList;
//                            if (domainClass.isAssignableFrom(ew.getClass())) {
//                                parameterMappingList = boundSql.getParameterMappings();
//                                parametersStrList = (List)parameterMappingList.stream().map((s) -> {
//                                    return s.getProperty();
//                                }).collect(Collectors.toList());
//                                String sqlWhere = baseSql.substring(baseSql.indexOf("WHERE"));
//                                List<String> actualParameterList = (List)parametersStrList.stream().filter((s) -> {
//                                    return s.contains("ew");
//                                }).map((s) -> {
//                                    return s.substring(s.lastIndexOf(".") + 1);
//                                }).collect(Collectors.toList());
//                                valueList = (List)actualParameterList.stream().map((p) -> {
//                                    return ReflectionUtil.getMethodValue(ew, p);
//                                }).collect(Collectors.toList());
//                                String selectSql = this.getSqlFromBaseSql(tableInfo, baseSql, SqlCommandType.SELECT, tableFieldInfoList, i18nFieldList, sqlWhere);
//                                Map<Long, Map<String, Object>> map = SqlExecutionUtil.executeForMapWithManyParameters(connection, selectSql, valueList, tableFieldInfoList, i18nFieldList);
//                                idList = this.convertBaseMap2List(domainClass, map, (List)null, i18nFieldList);
//                                if (CollectionUtils.isNotEmpty(idList)) {
//                                    return idList;
//                                }
//                            } else
                            if (QueryWrapper.class.isAssignableFrom(ew.getClass())) {
                                QueryWrapper entityWrapper = (QueryWrapper)ew;
                                Object et = entityWrapper.getEntity();
                                Map paramNameValuePairs = entityWrapper.getParamNameValuePairs();
                                RowBounds pageParameter = (RowBounds)invocation.getArgs()[2];
                                if (pageParameter != null && pageParameter != RowBounds.DEFAULT) {
                                    DbType dbType = StringUtils.isNotEmpty(this.dialectType) ? DbType.getDbType(this.dialectType) : JdbcUtils.getDbType(connection.getMetaData().getURL());
                                    String originalSql="";
                                    if (pageParameter instanceof IPage) {
                                        IPage page = (IPage)pageParameter;
                                        boolean orderBy = true;
                                        if (page.isSearchCount()) {
//                                            SqlInfo sqlInfo = SqlUtils.(this.sqlParser, baseSql);
//                                            orderBy = sqlInfo.isOrderBy();
//                                            this.queryTotal(this.overflowCurrent, sqlInfo.getSql(), ms, boundSql, page, connection);
//                                            if (page.getTotal() <= 0) {
//                                                return invocation.proceed();
//                                            }
                                        }
//
//                                        String buildSql = SqlUtils.(baseSql, page, orderBy);
//                                        originalSql = DialectFactory.buildPaginationSql(page, buildSql, dbType, this.dialectClazz);
                                    } else {
//                                        originalSql = DialectFactory.buildPaginationSql(pageParameter, baseSql, dbType, this.dialectClazz);
                                    }

                                    List<Object> valueList1 = this.getValueListByBindingParameters(et, paramNameValuePairs, boundSql);
                                    idList = SqlExecutionUtil.executeForIdsWithParameters(connection, valueList1, originalSql);
                                    List<Object> resultList = this.executeQueryBindingParameters(tableInfo, connection, domainClass, valueList1, baseSql, idList, tableFieldInfoList, i18nFieldList);
                                    if (CollectionUtils.isNotEmpty(resultList)) {
                                        return resultList;
                                    }
                                } else {
                                    valueList = this.getValueListByBindingParameters(et, paramNameValuePairs, boundSql);
                                    List<Object> resultList = this.executeQueryBindingParameters(tableInfo, connection, domainClass, valueList, baseSql, (List)null, tableFieldInfoList, i18nFieldList);
                                    if (CollectionUtils.isNotEmpty(resultList)) {
                                        return resultList;
                                    }
                                }
                            }
                        } else {
                            log.info("Parameter'class: " + parameterClass.getName() + ",i18n interceptor is not supported for this api now!");
                        }
                    }

                    baseResult = invocation.proceed();
                    break;
                default:
                    throw new SqlProcessInterceptorException("非CRUD操作,请检查!");
            }

            return baseResult;
        }
    }

    private List<Object> getValueListByBindingParameters(Object et, Map paramNameValuePairs, BoundSql boundSql) {
        List<Object> valueList = new ArrayList();
        if (et != null) {
            List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
            List<String> parametersStrList = (List)parameterMappingList.stream().map((s) -> {
                return s.getProperty();
            }).collect(Collectors.toList());
            List<String> actualParameterList = (List)parametersStrList.stream().filter((s) -> {
                return s.contains("ew.entity");
            }).map((s) -> {
                return s.substring(s.lastIndexOf(".") + 1);
            }).collect(Collectors.toList());
            valueList.addAll((Collection)actualParameterList.stream().map((p) -> {
                return ReflectionUtil.getMethodValue(et, p);
            }).collect(Collectors.toList()));
        }

        if (paramNameValuePairs.size() > 0) {
            Map<String, String> sortedMap = paramNameValuePairs;
            valueList.addAll((Collection)sortedMap.values().stream().collect(Collectors.toList()));
        }

        return valueList;
    }

    private List<Object> executeQueryBindingParameters(TableInfo tableInfo, Connection connection, Class domainClass, List<Object> valueList, String baseSql, List<Long> idList, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList) {
        String sqlWhere = baseSql.substring(baseSql.indexOf("WHERE"));
        String selectSql = this.getSqlFromBaseSql(tableInfo, baseSql, SqlCommandType.SELECT, tableFieldInfoList, i18nFieldList, sqlWhere);
        Map<Long, Map<String, Object>> map = SqlExecutionUtil.executeForMapWithManyParameters(connection, selectSql, valueList, tableFieldInfoList, i18nFieldList);
        List<Object> resultList = this.convertBaseMap2List(domainClass, map, idList, i18nFieldList);
        return resultList;
    }

    private String getSqlFromBaseSql(TableInfo tableInfo, String baseSql, SqlCommandType sqlCommandType, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList, String sqlWhere) {
        StringBuilder sb = new StringBuilder();
        switch(sqlCommandType) {
            case UPDATE:
                sb.append(baseSql.substring(baseSql.indexOf("WHERE")));
                break;
            case DELETE:
                int offset = baseSql.indexOf("FROM", 0);
                int firstBlankIndex = baseSql.indexOf(" ", offset);
                int secondBlankIndex = baseSql.indexOf(" ", firstBlankIndex + 1);
                String tableName = baseSql.substring(firstBlankIndex, secondBlankIndex).trim();
                sb.append(baseSql.substring(0, firstBlankIndex)).append(" ").append(tableName + "_i18n").append(" ").append("WHERE id IN (SELECT id FROM ").append(tableName).append(baseSql.substring(secondBlankIndex)).append(");");
                break;
            case SELECT:
                baseSql = baseSql.replaceAll("`", "");
                if (StringUtils.isEmpty(sqlWhere)) {
                    Iterator var8 = tableFieldInfoList.iterator();

                    while(var8.hasNext()) {
                        TableFieldInfo tableFieldInfo = (TableFieldInfo)var8.next();
                        if (i18nFieldList.contains(tableFieldInfo.getProperty())) {
                            baseSql = this.replaceColumnWithTableAlias(baseSql, tableFieldInfo.getColumn().replaceAll("`", ""), "i18n", "base." + tableFieldInfo.getColumn() + " AS base_" + tableFieldInfo.getProperty() + ",");
                        } else {
                            baseSql = this.replaceColumnWithTableAlias(baseSql, tableFieldInfo.getColumn().replaceAll("`", ""), "base", "");
                        }
                    }

                    baseSql = this.replaceColumnWithTableAlias(baseSql, "id", "base", "");
                    sb.append(baseSql.substring(0, baseSql.indexOf("WHERE"))).append("base LEFT JOIN ").append(tableInfo.getTableName()).append("_i18n i18n ON base.id = i18n.id ").append(this.replaceColumnWithTableAlias(baseSql.substring(baseSql.indexOf("WHERE")), "id", "base", "")).append(" AND i18n.language = ?;");
                } else {
                    String sqlHeader = baseSql.substring(0, baseSql.indexOf("FROM"));

                    TableFieldInfo tableFieldInfo;
                    for(Iterator var13 = tableFieldInfoList.iterator(); var13.hasNext(); sqlWhere = this.replaceColumnWithTableAlias(sqlWhere.replaceAll("`", ""), tableFieldInfo.getColumn().replaceAll("`", ""), "base", "")) {
                        tableFieldInfo = (TableFieldInfo)var13.next();
                        if (i18nFieldList.contains(tableFieldInfo.getProperty())) {
                            sqlHeader = this.replaceColumnWithTableAlias(sqlHeader, tableFieldInfo.getColumn().replaceAll("`", ""), "i18n", "base." + tableFieldInfo.getColumn() + " AS base_" + tableFieldInfo.getProperty() + ",");
                        } else {
                            sqlHeader = this.replaceColumnWithTableAlias(sqlHeader, tableFieldInfo.getColumn().replaceAll("`", ""), "base", "");
                        }
                    }

                    sqlWhere = this.replaceColumnWithTableAlias(sqlWhere, "id", "base", "");
                    if (!sqlWhere.contains("WHERE")) {
                        sqlWhere = sqlWhere.replaceFirst("AND", "WHERE");
                    }

                    sqlHeader = this.replaceColumnWithTableAlias(sqlHeader, "id", "base", "");
                    sb.append(sqlHeader).append(",i18n.language FROM ").append(tableInfo.getTableName()).append(" base LEFT JOIN ").append(tableInfo.getTableName()).append("_i18n i18n  ON base.id = i18n.id ").append(sqlWhere).append(";");
                }
                break;
            default:
                throw new SqlProcessInterceptorException("目前只支持DELETE!");
        }

        return sb.toString();
    }

    private String replaceColumnWithTableAlias(String sqlStr, String column, String tableAlias, String additionStr) {
        if (sqlStr.indexOf(column) != -1) {
            String regex = "\\b" + column + "\\b";
            sqlStr = sqlStr.replaceFirst(regex, additionStr + tableAlias + "." + column);
        }

        return sqlStr;
    }

    public void execInsertOrUpdateList(Map<String, Map<String, String>> metaDataMap, String baseTableName, Connection connection, Long baseTableId) {
        metaDataMap.forEach((k, v) -> {
            this.execInsertOrUpdateOne(k, v, baseTableName, connection, baseTableId);
        });
    }

    private void execInsertOrUpdateOne(String language, Map<String, String> fieldValueMap, String baseTableName, Connection connection, Long baseTableId) {
        String sqlOrigin = "INSERT INTO ${tableName}_i18n(id,language${fieldList}) VALUES(?,?${valueList}) ON DUPLICATE KEY UPDATE ${updateList};";
        StringBuilder fieldListSb = new StringBuilder();
        StringBuilder valueListSb = new StringBuilder();
        StringBuilder updateListSb = new StringBuilder();
        Map<String, String> sqlParamMap = new HashMap();
        sqlParamMap.put("tableName", baseTableName);
        sqlParamMap.put("fieldList", "");
        sqlParamMap.put("valueList", "");
        sqlParamMap.put("updateList", "");
        fieldValueMap.forEach((k1, v1) -> {
            if (StringUtils.containsUpperCase(k1)) {
                k1 = StringUtils.camelToUnderline(k1);
            }

            fieldListSb.append("," + k1);
            valueListSb.append(",'" + v1 + "'");
            updateListSb.append("," + k1 + "='" + v1 + "'");
        });
        sqlParamMap.put("fieldList", fieldListSb.toString());
        sqlParamMap.put("valueList", valueListSb.toString());
        sqlParamMap.put("updateList", updateListSb.toString().substring(1));
        String sql = StrSubstitutor.replace(sqlOrigin, sqlParamMap);

        try {
            PreparedStatement psm = connection.prepareStatement(sql);
            Throwable var13 = null;

            try {
                psm.setLong(1, baseTableId);
                psm.setString(2, language);
                psm.execute();
            } catch (Throwable var23) {
                var13 = var23;
                throw var23;
            } finally {
                if (psm != null) {
                    if (var13 != null) {
                        try {
                            psm.close();
                        } catch (Throwable var22) {
                            var13.addSuppressed(var22);
                        }
                    } else {
                        psm.close();
                    }
                }

            }
        } catch (SQLException var25) {
            var25.printStackTrace();
        }

    }

    private Map<String, Map<String, String>> constructMetaDataMap(Object parameter, List<String> i18nFieldNameList) {
        Map<String, List<Map<String, String>>> metaData = ((BaseI18nDomain)parameter).getI18n();
        List<BaseI18nMetaData> baseI18nMetaDataList = new ArrayList();
        Map<String, Map<String, String>> metaDataMap = new HashMap();
        if (metaData != null) {
            metaData.forEach((k, v) -> {
                v.forEach((p) -> {
                    if (i18nFieldNameList.contains(k)) {
                        baseI18nMetaDataList.add(BaseI18nMetaData.builder().field(k).language((String)p.get("language")).value((String)p.get("value")).build());
                    }

                });
            });
            Iterator var6 = baseI18nMetaDataList.iterator();

            while(var6.hasNext()) {
                BaseI18nMetaData baseI18nMetaData = (BaseI18nMetaData)var6.next();
                String language = baseI18nMetaData.getLanguage();
                Map subMap = new HashMap();
                if (metaDataMap.keySet().contains(language)) {
                    subMap = (Map)metaDataMap.get(language);
                }

                ((Map)subMap).put(baseI18nMetaData.getField(), baseI18nMetaData.getValue());
                metaDataMap.put(language, subMap);
            }
        }

        return metaDataMap;
    }

    public List convertBaseMap2List(Class domainClass, Map<Long, Map<String, Object>> map, List<Long> idList, List<String> i18nFieldList) {
        List<Object> resultList = new ArrayList();
        if (CollectionUtils.isEmpty(idList)) {
            map.forEach((id, subMap) -> {
                this.getResultListFromMap(domainClass, id, subMap, resultList, i18nFieldList);
            });
        } else {
            idList.forEach((i) -> {
                Map<String, Object> subMap = (Map)map.get(i);
                if (subMap != null) {
                    this.getResultListFromMap(domainClass, i, subMap, resultList, i18nFieldList);
                }

            });
        }

        return resultList;
    }

    private void getResultListFromMap(Class domainClass, Long id, Map<String, Object> subMap, List<Object> resultList, List<String> i18nFieldList) {
        try {
            Object result = domainClass.newInstance();
            if (null == BaseI18nService.i18nDomainMethodCache.get(domainClass)) {
                throw new SqlProcessInterceptorException(domainClass.getName() + "尚未初始化,请检查!");
            }

            Invoker idSetMethodInvoker = ((Reflector)BaseI18nService.i18nDomainMethodCache.get(domainClass)).getSetInvoker("id");
            Object[] param = new Object[]{id};
            idSetMethodInvoker.invoke(result, param);
            subMap.forEach((fieldName, filedValue) -> {
                Invoker setMethodInvoker = ((Reflector)BaseI18nService.i18nDomainMethodCache.get(domainClass)).getSetInvoker(fieldName);

                try {
                    if (setMethodInvoker instanceof MethodInvoker) {
                        ReflectionUtil.specificProcessInvoker((MethodInvoker)setMethodInvoker, filedValue, fieldName, result, (BaseI18nDomain)null, i18nFieldList);
                    } else {
                        Object[] paramField = new Object[]{filedValue};
                        setMethodInvoker.invoke(result, paramField);
                    }
                } catch (IllegalAccessException var7) {
                    var7.printStackTrace();
                } catch (InvocationTargetException var8) {
                    var8.printStackTrace();
                }

            });
            resultList.add(result);
        } catch (InstantiationException var9) {
            var9.printStackTrace();
        } catch (IllegalAccessException var10) {
            var10.printStackTrace();
        } catch (InvocationTargetException var11) {
            var11.printStackTrace();
        }

    }

    private Boolean methodSupported(SqlCommandType sqlCommandType, String methodStr) {
        String[] methodArray = (String[])supportedOperationMap.get(sqlCommandType);
        return methodArray == null ? false : Arrays.asList(methodArray).contains(methodStr);
    }

    protected void queryTotal(boolean overflowCurrent, String sql, MappedStatement mappedStatement, BoundSql boundSql, IPage page, Connection connection) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            Throwable var8 = null;

            try {
                DefaultParameterHandler parameterHandler = new MybatisDefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), boundSql);
                parameterHandler.setParameters(statement);
                int total = 0;
                ResultSet resultSet = statement.executeQuery();
                Throwable var12 = null;

                try {
                    if (resultSet.next()) {
                        total = resultSet.getInt(1);
                    }
                } catch (Throwable var37) {
                    var12 = var37;
                    throw var37;
                } finally {
                    if (resultSet != null) {
                        if (var12 != null) {
                            try {
                                resultSet.close();
                            } catch (Throwable var36) {
                                var12.addSuppressed(var36);
                            }
                        } else {
                            resultSet.close();
                        }
                    }

                }

                page.setTotal(total);
                Long pages = page.getPages();
                if (overflowCurrent && page.getCurrent() > pages) {
                    page =  new Page<>(1, page.getSize());
                    page.setTotal(total);
                }
            } catch (Throwable var39) {
                var8 = var39;
                throw var39;
            } finally {
                if (statement != null) {
                    if (var8 != null) {
                        try {
                            statement.close();
                        } catch (Throwable var35) {
                            var8.addSuppressed(var35);
                        }
                    } else {
                        statement.close();
                    }
                }

            }
        } catch (Exception var41) {
            log.error("Error: Method queryTotal execution error !", var41);
        }

    }

    public Object plugin(Object target) {
        return target instanceof Executor ? Plugin.wrap(target, this) : target;
    }

    public void setProperties(Properties properties) {
        String dialectType = properties.getProperty("dialectType");
        String dialectClazz = properties.getProperty("dialectClazz");
        if (StringUtils.isNotEmpty(dialectType)) {
            this.dialectType = dialectType;
        }

        if (StringUtils.isNotEmpty(dialectClazz)) {
            this.dialectClazz = dialectClazz;
        }

    }

    public void setDialectType(String dialectType) {
        this.dialectType = dialectType;
    }

    public void setDialectClazz(String dialectClazz) {
        this.dialectClazz = dialectClazz;
    }

    public void setOverflowCurrent(boolean overflowCurrent) {
        this.overflowCurrent = overflowCurrent;
    }



    static {
        supportedOperationMap.put(SqlCommandType.INSERT, new String[]{SqlMethod.INSERT_ONE.getMethod()});
        supportedOperationMap.put(SqlCommandType.UPDATE, new String[]{SqlMethod.UPDATE.getMethod(), SqlMethod.UPDATE_BY_ID.getMethod()});
        supportedOperationMap.put(SqlCommandType.DELETE, new String[]{SqlMethod.DELETE.getMethod(), SqlMethod.DELETE_BY_ID.getMethod(), SqlMethod.DELETE_BY_MAP.getMethod()});
        supportedOperationMap.put(SqlCommandType.SELECT, new String[]{SqlMethod.SELECT_BY_ID.getMethod(), SqlMethod.SELECT_LIST.getMethod(), SqlMethod.SELECT_ONE.getMethod(), SqlMethod.SELECT_MAPS.getMethod(), SqlMethod.SELECT_PAGE.getMethod(), SqlMethod.SELECT_MAPS_PAGE.getMethod()});
    }
}
