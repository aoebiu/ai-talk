package info.mengnan.aitalk.repository.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * SQL日志拦截器
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
})
public class SqlLogInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            return invocation.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long sqlCost = endTime - startTime;

            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            Object parameter = null;
            if (invocation.getArgs().length > 1) {
                parameter = invocation.getArgs()[1];
            }

            BoundSql boundSql = mappedStatement.getBoundSql(parameter);
            Configuration configuration = mappedStatement.getConfiguration();

            String sql = formatSql(boundSql, parameter, configuration);

            log.info("SQL耗时: {}ms | {}", sqlCost, sql);
        }
    }

    /**
     * 格式化SQL,替换占位符为真实参数值
     */
    private String formatSql(BoundSql boundSql, Object parameterObject, Configuration configuration) {
        String sql = boundSql.getSql();

        // 去除多余空白字符
        sql = sql.replaceAll("[\\s]+", " ");

        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

        if (parameterMappings != null && !parameterMappings.isEmpty()) {
            MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);

            for (ParameterMapping parameterMapping : parameterMappings) {
                // 跳过OUT类型的参数(存储过程输出参数)
                String mode = parameterMapping.getMode().name();
                if ("OUT".equals(mode)) {
                    continue;
                }

                Object value;
                String propertyName = parameterMapping.getProperty();

                if (boundSql.hasAdditionalParameter(propertyName)) {
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    value = metaObject == null ? null : metaObject.getValue(propertyName);
                }

                String paramValueStr = getParameterValue(value);
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(paramValueStr));
            }
        }

        return sql;
    }

    /**
     * 获取参数值的字符串表示
     */
    private String getParameterValue(Object obj) {
        if (obj == null) {
            return "NULL";
        }

        if (obj instanceof String) {
            return "'" + obj + "'";
        }

        if (obj instanceof Date) {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "'" + formatter.format(obj) + "'";
        }

        return obj.toString();
    }
}