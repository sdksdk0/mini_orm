package cn.tf.spring.framework;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.HashMap;
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

 /*   protected List<T> select(QueryRule queryRule) {
        QueryRuleSqlBuilder builder = new QueryRuleSqlBuilder(queryRule);
        String whereSql = builder.getWhereSql();
        StringBuilder sb = new StringBuilder();
        sb.append("select ").append(entityOption.getAllColumns()).append(" ")
                .append("from ").append(entityOption.getTableName()).append(" ").append(whereSql);
        return readonlyJdbctemplate.query(sb.toString(), builder.getValues(), entityOption.getRowMapper());
    }*/
    public List<T> select(QueryRule queryRule) throws Exception{
        QueryRuleSqlBuilder bulider = new QueryRuleSqlBuilder(queryRule);
        String ws = removeFirstAnd(bulider.getWhereSql());
        String whereSql = ("".equals(ws) ? ws : (" where " + ws));
        String sql = "select " + op.allColumn + " from " + getTableName() + whereSql;
        Object [] values = bulider.getValues();
        String orderSql = bulider.getOrderSql();
        orderSql = (StringUtils.isEmpty(orderSql) ? " " : (" order by " + orderSql));
        sql += orderSql;
        log.debug(sql);
        return (List<T>) readonlyJdbctemplate.query(sql, entityOption.getRowMapper(), values);
    }

    /**
     * 获取全部对象. <br>
     *
     * @return 全部对象
     */
    protected List<T> getAll() throws Exception {
        String sql = "select " + entityOption.getAllColumns() + " from " + entityOption.getTableName();
        return readonlyJdbctemplate.query(sql, entityOption.getRowMapper(), new HashMap<String, Object>());
    }

    private String removeFirstAnd(String sql){
        if(StringUtils.isEmpty(sql)){return sql;}
        return sql.trim().toLowerCase().replaceAll("^\\s*and", "") + " ";
    }

}
