package cn.tf.spring.framework;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.List;

public class BaseDaoSupport<T extends Serializable, P extends Serializable> {

    private EntityOption<T> entityOption;

    protected BaseDaoSupport() throws Exception {
        Class<?> clazz = GenericUtils.getGenericClass(getClass());
        entityOption = new EntityOption<>(clazz);
    }

    private DataSource readonlyDataSource;

    private DataSource writeDataSource;

    private JdbcTemplate readonlyJdbctemplate;

    private JdbcTemplate writeJdbcTemplate;

    protected void setReadonlyDataSource(DataSource readonlyDataSource) {
        this.readonlyDataSource = readonlyDataSource;
        this.readonlyJdbctemplate = new JdbcTemplate(readonlyDataSource);
    }

    protected void setWriteDataSource(DataSource writeDataSource) {
        this.writeDataSource = writeDataSource;
        this.writeJdbcTemplate = new JdbcTemplate(writeDataSource);
    }

    protected List<T> select(QueryRule queryRule) {
        QueryRuleSqlBuilder builder = new QueryRuleSqlBuilder(queryRule);
        String whereSql = builder.getWhereSql();
        StringBuilder sb = new StringBuilder();
        sb.append("select ").append(entityOption.getAllColumns()).append(" ")
                .append("from ").append(entityOption.getTableName()).append(" ").append(whereSql);
        return readonlyJdbctemplate.query(sb.toString(), builder.getValues(), entityOption.getRowMapper());
    }

}
