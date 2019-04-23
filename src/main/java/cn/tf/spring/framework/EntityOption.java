package cn.tf.spring.framework;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EntityOption<T> {

    private Class<?> clazz = null;

    private String allColumns;

    private String tableName;

    private String pk;

    private final RowMapper<T> rowMapper;

    private final Map<String, Field> paramNamemapping;


    public EntityOption(Class<?> clazz) throws Exception {
        this.clazz = clazz;
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new Exception(clazz + "必须使用Entity注解标注");
        }

        if (!clazz.isAnnotationPresent(Table.class)) {
            throw new Exception(clazz + "必须使用Table注解标注");
        }

        Table table = clazz.getAnnotation(Table.class);

        this.tableName = table.name();
        if ("".equals(tableName.trim())) {
            throw new Exception("table name 为设置");
        }

        paramNamemapping = new HashMap<>(32);

        createAllColumns(clazz);


        this.rowMapper = createRowMapper();
    }

    private void createAllColumns(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        Field[] fields = clazz.getDeclaredFields();
        int index = 0;
        for (Field field : fields) {
            String paramName = field.getName();
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (!"".equals(column.name().trim())) {
                    paramName = column.name();

                }
                paramNamemapping.put(paramName, field);

            } else {
                paramNamemapping.put(field.getName(), field);
            }
            if (index != 0) {
                sb.append(",");
            }
            sb.append(paramName).append(" AS \"").append(field.getName()).append("\"");
            index ++;
        }
        this.allColumns = sb.toString();


    }

    private RowMapper<T> createRowMapper() {
        return new RowMapper<T>() {
            @Override
            public T mapRow(ResultSet resultSet, int i) throws SQLException {
                try {
                    T t = (T) clazz.newInstance();
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int count = metaData.getColumnCount();
                    for (int j = 1; j <= count; j++) {
                        Object value = resultSet.getObject(j);
                        String columnName = metaData.getColumnName(j);

                        setValue(t, columnName, value);
                    }
                    return t;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    public String getAllColumns() {
        return allColumns;
    }

    public String getTableName() {
        return tableName;
    }

    public String getPk() {
        return pk;
    }

    private void setValue(T t, String columnName, Object value) throws Exception {
        Field field = this.paramNamemapping.get(columnName);
        if (field == null) {
            throw new Exception("columeName ：" + columnName + " is not exists");
        }
        field.setAccessible(true);
        field.set(t, value);
    }

    public RowMapper<T> getRowMapper() {
        return this.rowMapper;
    }
}
