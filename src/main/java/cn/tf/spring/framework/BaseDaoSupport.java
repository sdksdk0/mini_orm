package cn.tf.spring.framework;

import cn.tf.spring.framework.util.GenericsUtils;
import cn.tf.spring.framework.util.Page;
import com.sun.istack.internal.logging.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class BaseDaoSupport<T extends Serializable, P extends Serializable>{

    private Logger log = Logger.getLogger(BaseDaoSupport.class);

    private String tableName = "";

    private JdbcTemplate jdbcTemplateWrite;
    private JdbcTemplate jdbcTemplateReadOnly;

    private DataSource dataSourceReadOnly;
    private DataSource dataSourceWrite;

    private EntityOperation<T> op;

    @SuppressWarnings("unchecked")
    protected BaseDaoSupport(){
        try{
            Class<T> entityClass = GenericsUtils.getSuperClassGenricType(getClass(), 0);
            op = new EntityOperation<T>(entityClass,this.getPKColumn());
            this.setTableName(op.tableName);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    protected String getTableName() {
        return tableName;
    }

    protected DataSource getDataSourceReadOnly() {
        return dataSourceReadOnly;
    }

    protected DataSource getDataSourceWrite() {
        return dataSourceWrite;
    }
    protected abstract String getPKColumn();
    /**
     * 动态切换表名
     */
    protected void setTableName(String tableName) {
        if(StringUtils.isEmpty(tableName)){
            this.tableName = op.tableName;
        }else{
            this.tableName = tableName;
        }
    }

    protected void setDataSourceWrite(DataSource dataSourceWrite) {
        this.dataSourceWrite = dataSourceWrite;
        jdbcTemplateWrite = new JdbcTemplate(dataSourceWrite);
    }

    protected void setDataSourceReadOnly(DataSource dataSourceReadOnly) {
        this.dataSourceReadOnly = dataSourceReadOnly;
        jdbcTemplateReadOnly = new JdbcTemplate(dataSourceReadOnly);
    }

    private JdbcTemplate jdbcTemplateReadOnly() {
        return this.jdbcTemplateReadOnly;
    }

    private JdbcTemplate jdbcTemplateWrite() {
        return this.jdbcTemplateWrite;
    }


    /**
     * 还原默认表名
     */
    protected void restoreTableName(){
        this.setTableName(op.tableName);
    }

    /**
     * 将对象解析为Map
     * @param entity
     * @return
     */
    protected Map<String,Object> parse(T entity){
        return op.parse(entity);
    }

    /**
     * 插入一条记录
     * @param entity
     * @return
     */
    public boolean insert(T entity) throws Exception{
        return this.doInsert(parse(entity));
    }


    /**
     * 使用SQL语句更新对象.<br>
     * 例如：以下代码将更新id="0002"的name值更新为“张三”到数据库
     * <pre>
     * 		<code>
     * String name = "张三";
     * String id = "0002";
     * String sql = "UPDATE SET name = ? WHERE id = ?";
     * // 更新对象
     * service.update(sql,name,id)
     * </code>
     * </pre>
     *
     * @param sql 更新sql语句
     * @param args 参数对象
     *
     * @return 更新记录数
     */
    protected int update(String sql,Object... args) throws Exception{
        return jdbcTemplateWrite().update(sql, args);
    }

    /**
     * 使用SQL语句更新对象.<br>
     * 例如：以下代码将更新id="0002"的name值更新为“张三”到数据库
     * <pre>
     * 		<code>
     * Map<String,Object> map = new HashMap();
     * map.put("name","张三");
     * map.put("id","0002");
     * String sql = "UPDATE SET name = :name WHERE id = :id";
     * // 更新对象
     * service.update(sql,map)
     * </code>
     * </pre>
     *
     * @param sql 更新sql语句
     * @param paramMap 参数对象
     *
     * @return 更新记录数
     */
    protected int update(String sql,Map<String,?> paramMap) throws Exception{
        return jdbcTemplateWrite().update(sql, paramMap);
    }
    /**
     * 批量保存对象.<br>
     * 例如：以下代码将对象保存到数据库
     * <pre>
     * 		<code>
     * List&lt;Role&gt; list = new ArrayList&lt;Role&gt;();
     * for (int i = 1; i &lt; 8; i++) {
     * 	Role role = new Role();
     * 	role.setId(i);
     * 	role.setRolename(&quot;管理quot; + i);
     * 	role.setPrivilegesFlag(&quot;1,2,3&quot;);
     * 	list.add(role);
     * }
     * service.insertAll(list);
     * </code>
     * </pre>
     *
     * @param list 待保存的对象List
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public int insertAll(List<T> list) throws Exception {
        int count = 0 ,len = list.size(),step = 50000;
        Map<String, PropertyMapping> pm = op.mappings;
        int maxPage = (len % step == 0) ? (len / step) : (len / step + 1);
        for (int i = 1; i <= maxPage; i ++) {
            Page<T> page = pagination(list, i, step);
            String sql = "insert into " + getTableName() + "(" + op.allColumn + ") values ";// (" + valstr.toString() + ")";
            StringBuffer valstr = new StringBuffer();
            Object[] values = new Object[pm.size() * page.getRows().size()];
            for (int j = 0; j < page.getRows().size(); j ++) {
                if(j > 0 && j < page.getRows().size()){ valstr.append(","); }
                valstr.append("(");
                int k = 0;
                for (PropertyMapping p : pm.values()) {
                    values[(j * pm.size()) + k] = p.getter.invoke(page.getRows().get(j));
                    if(k > 0 && k < pm.size()){ valstr.append(","); }
                    valstr.append("?");
                    k ++;
                }
                valstr.append(")");
            }
            int result = jdbcTemplateWrite().update(sql + valstr.toString(), values);
            count += result;
        }

        return count;
    }


    protected boolean replaceOne(T entity) throws Exception{
        return this.doReplace(parse(entity));
    }


    protected int replaceAll(List<T> list) throws Exception {
        int count = 0 ,len = list.size(),step = 50000;
        Map<String, PropertyMapping> pm = op.mappings;
        int maxPage = (len % step == 0) ? (len / step) : (len / step + 1);
        for (int i = 1; i <= maxPage; i ++) {
            Page<T> page = pagination(list, i, step);
            String sql = "replace into " + getTableName() + "(" + op.allColumn + ") values ";// (" + valstr.toString() + ")";
            StringBuffer valstr = new StringBuffer();
            Object[] values = new Object[pm.size() * page.getRows().size()];
            for (int j = 0; j < page.getRows().size(); j ++) {
                if(j > 0 && j < page.getRows().size()){ valstr.append(","); }
                valstr.append("(");
                int k = 0;
                for (PropertyMapping p : pm.values()) {
                    values[(j * pm.size()) + k] = p.getter.invoke(page.getRows().get(j));
                    if(k > 0 && k < pm.size()){ valstr.append(","); }
                    valstr.append("?");
                    k ++;
                }
                valstr.append(")");
            }
            int result = jdbcTemplateWrite().update(sql + valstr.toString(), values);
            count += result;
        }
        return count;
    }


    /**
     * 删除对象.<br>
     * 例如：以下删除entity对应的记录
     * <pre>
     * 		<code>
     * service.deleteAll(entityList);
     * </code>
     * </pre>
     *
     * @param list 待删除的实体对象列表
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public int deleteAll(List<T> list) throws Exception {
        String pkName = op.pkField.getName();
        int count = 0 ,len = list.size(),step = 1000;
        Map<String, PropertyMapping> pm = op.mappings;
        int maxPage = (len % step == 0) ? (len / step) : (len / step + 1);
        for (int i = 1; i <= maxPage; i ++) {
            StringBuffer valstr = new StringBuffer();
            Page<T> page = pagination(list, i, step);
            Object[] values = new Object[page.getRows().size()];

            for (int j = 0; j < page.getRows().size(); j ++) {
                if(j > 0 && j < page.getRows().size()){ valstr.append(","); }
                values[j] = pm.get(pkName).getter.invoke(page.getRows().get(j));
                valstr.append("?");
            }

            String sql = "delete from " + getTableName() + " where " + pkName + " in (" + valstr.toString() + ")";
            int result = jdbcTemplateWrite().update(sql, values);
            count += result;
        }
        return count;
    }



    /**
     * 查询满足条件的记录数，使用hql.<br>
     * 例如：查询User里满足条件?name like "%ca%" 的记录数
     *
     * <pre>
     * 		<code>
     * long count = service.getCount(&quot;from User where name like ?&quot;, &quot;%ca%&quot;);
     * </code>
     * </pre>
     *
     * @param queryRule
     * @return 满足条件的记录数
     */
    protected long getCount(QueryRule queryRule) throws Exception {
        QueryRuleSqlBuilder bulider = new QueryRuleSqlBuilder(queryRule);
        Object [] values = bulider.getValues();
        String ws = removeFirstAnd(bulider.getWhereSql());
        String whereSql = ("".equals(ws) ? ws : (" where " + ws));
        String countSql = "select count(1) from " + getTableName() + whereSql;
        return (Long) this.jdbcTemplateReadOnly().queryForMap(countSql, values).get("count(1)");
    }

    /**
     * 根据某个属性值倒序获得第一个最大值
     * @param propertyName
     * @return
     */
    protected T getMax(String propertyName) throws Exception{
        QueryRule queryRule = QueryRule.getInstance();
        queryRule.addDescOrder(propertyName);
        Page<T> result = this.select(queryRule,1,1);
        if(null == result.getRows() || 0 == result.getRows().size()){
            return null;
        }else{
            return result.getRows().get(0);
        }
    }

    /**
     * 查询函数，使用查询规
     * 例如以下代码查询条件为匹配的数据
     *
     * <pre>
     *		<code>
     * QueryRule queryRule = QueryRule.getInstance();
     * queryRule.addLike(&quot;username&quot;, user.getUsername());
     * queryRule.addLike(&quot;monicker&quot;, user.getMonicker());
     * queryRule.addBetween(&quot;id&quot;, lowerId, upperId);
     * queryRule.addDescOrder(&quot;id&quot;);
     * queryRule.addAscOrder(&quot;username&quot;);
     * list = userService.select(User.class, queryRule);
     * </code>
     * </pre>
     *
     * @param queryRule 查询规则
     * @return 查询出的结果List
     */
    public List<T> select(QueryRule queryRule) throws Exception{
        QueryRuleSqlBuilder bulider = new QueryRuleSqlBuilder(queryRule);
        String ws = removeFirstAnd(bulider.getWhereSql());
        String whereSql = ("".equals(ws) ? ws : (" where " + ws));
        String sql = "select " + op.allColumn + " from " + getTableName() + whereSql;
        Object [] values = bulider.getValues();
        String orderSql = bulider.getOrderSql();
        orderSql = (StringUtils.isEmpty(orderSql) ? " " : (" order by " + orderSql));
        sql += orderSql;
        return (List<T>) this.jdbcTemplateReadOnly().query(sql, this.op.rowMapper, values);
    }

    /**
     * 根据SQL语句执行查询，参数为Map
     * @param sql 语句
     * @param pamam 为Map，key为属性名，value为属性值
     * @return 符合条件的所有对象
     */
    protected List<Map<String,Object>> selectBySql(String sql,Map<String,?> pamam) throws Exception{
        return this.jdbcTemplateReadOnly().queryForList(sql,pamam);
    }

    /**
     * 根据SQL语句查询符合条件的唯一对象，没符合条件的记录返回null.<br>
     * @param sql 语句
     * @param pamam 为Map，key为属性名，value为属性值
     * @return 符合条件的唯一对象，没符合条件的记录返回null.
     */
    protected Map<String,Object> selectUniqueBySql(String sql,Map<String,?> pamam) throws Exception{
        List<Map<String,Object>> list = selectBySql(sql,pamam);
        if (list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            throw new IllegalStateException("findUnique return " + list.size() + " record(s).");
        }
    }

    /**
     * 根据SQL语句执行查询，参数为Object数组对象
     * @param sql 查询语句
     * @param args 为Object数组
     * @return 符合条件的所有对象
     */
    public List<Map<String,Object>> selectBySql(String sql,Object... args) throws Exception{
        return this.jdbcTemplateReadOnly().queryForList(sql,args);
    }



    /**
     * 根据SQL语句执行查询，参数为List对象
     * @param sql 查询语句
     * @param list<Object>对象
     * @return 符合条件的所有对象
     */
    protected List<Map<String,Object>> selectBySql(String sql,List<Object> list) throws Exception{
        return this.jdbcTemplateReadOnly().queryForList(sql,list.toArray());
    }

    /**
     * 根据SQL语句查询符合条件的唯一对象，没符合条件的记录返回null.<br>
     * @param sql 查询语句
     * @param listParam 属性值List
     * @return 符合条件的唯一对象，没符合条件的记录返回null.
     */
    protected Map<String,Object> selectUniqueBySql(String sql,List<Object> listParam) throws Exception{
        List<Map<String,Object>> listMap = selectBySql(sql, listParam);
        if (listMap.size() == 0) {
            return null;
        } else if (listMap.size() == 1) {
            return listMap.get(0);
        } else {
            throw new IllegalStateException("findUnique return " + listMap.size() + " record(s).");
        }
    }

    /**
     * 分页查询函数，使用查询规则<br>
     * 例如以下代码查询条件为匹配的数据
     *
     * <pre>
     *		<code>
     * QueryRule queryRule = QueryRule.getInstance();
     * queryRule.addLike(&quot;username&quot;, user.getUsername());
     * queryRule.addLike(&quot;monicker&quot;, user.getMonicker());
     * queryRule.addBetween(&quot;id&quot;, lowerId, upperId);
     * queryRule.addDescOrder(&quot;id&quot;);
     * queryRule.addAscOrder(&quot;username&quot;);
     * page = userService.select(queryRule, pageNo, pageSize);
     * </code>
     * </pre>
     *
     * @param queryRule 查询规则
     * @param pageNo 页号,从1开始
     * @param pageSize  每页的记录条数
     * @return 查询出的结果Page
     */
    public Page<T> select(QueryRule queryRule,final int pageNo, final int pageSize) throws Exception{
        QueryRuleSqlBuilder bulider = new QueryRuleSqlBuilder(queryRule);
        Object [] values = bulider.getValues();
        String ws = removeFirstAnd(bulider.getWhereSql());
        String whereSql = ("".equals(ws) ? ws : (" where " + ws));
        String countSql = "select count(1) from " + getTableName() + whereSql;
        long count = (Long) this.jdbcTemplateReadOnly().queryForMap(countSql, values).get("count(1)");
        if (count == 0) {
            return new Page<T>();
        }
        long start = (pageNo - 1) * pageSize;
        // 有数据的情况下，继续查询
        String orderSql = bulider.getOrderSql();
        orderSql = (StringUtils.isEmpty(orderSql) ? " " : (" order by " + orderSql));
        String sql = "select " + op.allColumn +" from " + getTableName() + whereSql + orderSql + " limit " + start + "," + pageSize;
        List<T> list = (List<T>) this.jdbcTemplateReadOnly().query(sql, this.op.rowMapper, values);
        return new Page<T>(start, count, pageSize, list);
    }


    /**
     * 分页查询特殊SQL语句
     * @param sql 语句
     * @param param  查询条件
     * @param pageNo	页码
     * @param pageSize	每页内容
     * @return
     */
    protected Page<Map<String,Object>> selectBySqlToPage(String sql, Map<String,?> param, final int pageNo, final int pageSize) throws Exception {
        String countSql = "select count(1) from (" + sql + ") a";
        long count = (Long) this.jdbcTemplateReadOnly().queryForMap(countSql,param).get("count(1)");

        if (count == 0) {
            return new Page<Map<String,Object>>();
        }
        long start = (pageNo - 1) * pageSize;
        // 有数据的情况下，继续查询
        sql = sql + " limit " + start + "," + pageSize;
        List<Map<String,Object>> list = (List<Map<String,Object>>) this.jdbcTemplateReadOnly().queryForList(sql, param);
        return new Page<Map<String,Object>>(start, count, pageSize, list);
    }


    /**
     * 分页查询特殊SQL语句
     * @param sql 语句
     * @param param  查询条件
     * @param pageNo	页码
     * @param pageSize	每页内容
     * @return
     */
    public Page<Map<String,Object>> selectBySqlToPage(String sql, Object [] param, final int pageNo, final int pageSize) throws Exception {
        String countSql = "select count(1) from (" + sql + ") a";

        long count = (Long) this.jdbcTemplateReadOnly().queryForMap(countSql,param).get("count(1)");
//		long count = this.jdbcTemplateReadOnly().queryForLong(countSql, param);
        if (count == 0) {
            return new Page<Map<String,Object>>();
        }
        long start = (pageNo - 1) * pageSize;
        sql = sql + " limit " + start + "," + pageSize;
        List<Map<String,Object>> list = (List<Map<String,Object>>) this.jdbcTemplateReadOnly().queryForList(sql, param);
        return new Page<Map<String,Object>>(start, count, pageSize, list);
    }

    /**
     * 根据<属性名和属属性值Map查询符合条件的唯一对象，没符合条件的记录返回null.<br>
     * 例如，如下语句查找sex=1,age=18的所有记录：
     *
     * <pre>
     *     <code>
     * Map properties = new HashMap();
     * properties.put(&quot;sex&quot;, &quot;1&quot;);
     * properties.put(&quot;age&quot;, 18);
     * User user = service.selectUnique(properties);
     * </code>
     * </pre>
     *
     * @param properties 属性值Map，key为属性名，value为属性值
     * @return 符合条件的唯一对象，没符合条件的记录返回null.
     */
    protected T selectUnique(Map<String, Object> properties) throws Exception {
        QueryRule queryRule = QueryRule.getInstance();
        for (String key : properties.keySet()) {
            queryRule.andEqual(key, properties.get(key));
        }
        return selectUnique(queryRule);
    }

    /**
     * 根据查询规则查询符合条件的唯一象，没符合条件的记录返回null.<br>
     * <pre>
     *     <code>
     * QueryRule queryRule = QueryRule.getInstance();
     * queryRule.addLike(&quot;username&quot;, user.getUsername());
     * queryRule.addLike(&quot;monicker&quot;, user.getMonicker());
     * queryRule.addBetween(&quot;id&quot;, lowerId, upperId);
     * User user = service.selectUnique(queryRule);
     * </code>
     * </pre>
     *
     * @param queryRule  查询规则
     * @return 符合条件的唯一对象，没符合条件的记录返回null.
     */
    protected T selectUnique(QueryRule queryRule) throws Exception {
        List<T> list = select(queryRule);
        if (list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            throw new IllegalStateException("findUnique return " + list.size() + " record(s).");
        }
    }


    /**
     * 根据当前list进行相应的分页返回
     * @param objList
     * @param pageNo
     * @param pageSize
     * @return Page
     */
    protected Page<T> pagination(List<T> objList, int pageNo, int pageSize) throws Exception {
        List<T> objectArray = new ArrayList<T>(0);
        int startIndex = (pageNo - 1) * pageSize;
        int endIndex = pageNo * pageSize;
        if(endIndex >= objList.size()){
            endIndex = objList.size();
        }
        for (int i = startIndex; i < endIndex; i++) {
            objectArray.add(objList.get(i));
        }
        return new Page<T>(startIndex, objList.size(), pageSize, objectArray);
    }

    private String removeFirstAnd(String sql){
        if(StringUtils.isEmpty(sql)){return sql;}
        return sql.trim().toLowerCase().replaceAll("^\\s*and", "") + " ";
    }

    private EntityOperation<T> getOp(){
        return this.op;
    }



    /**
     * 分页查询支持，支持简单的sql查询分页（复杂的查询，请自行编写对应的方法）
     * @param <T>
     *
     * @param sql
     * @param rowMapper
     * @param args
     * @param pageNo
     * @param pageSize
     * @return
     */
    private <T> Page simplePageQuery(String sql, RowMapper<T> rowMapper, Map<String, ?> args, long pageNo, long pageSize) {
        long start = (pageNo - 1) * pageSize;
        return simplePageQueryByStart(sql,rowMapper,args,start,pageSize);
    }

    /**
     *
     * @param sql
     * @param rowMapper
     * @param args
     * @param start
     * @param pageSize
     * @return
     */
    private <T> Page simplePageQueryByStart(String sql, RowMapper<T> rowMapper, Map<String, ?> args, long start, long pageSize) {
        // 首先查询总数
        String countSql = "select count(*) " + removeSelect(removeOrders(sql));

        long count = (Long) this.jdbcTemplateReadOnly().queryForMap(countSql,args).get("count(1)");
        if (count == 0) {
            return new Page();
        }
        // 有数据的情况下，继续查询
        sql = sql + " limit " + start + "," + pageSize;
        List<T> list = this.jdbcTemplateReadOnly().query(sql, rowMapper, args);
        return new Page(start, count, (int)pageSize, list);
    }

    protected long queryCount(String sql,Map<String, ?> args){
        String countSql = "select count(1) " + removeSelect(removeOrders(sql));

        return (Long)this.jdbcTemplateReadOnly().queryForMap(countSql, args).get("count(1)");
    }

    protected <T> List<T> simpleListQueryByStart(String sql, RowMapper<T> rowMapper,
                                                 Map<String, ?> args, long start, long pageSize) {

        sql = sql + " limit " + start + "," + pageSize;
        List<T> list = this.jdbcTemplateReadOnly().query(sql, rowMapper, args);
        if(list == null){
            return new ArrayList<T>();
        }
        return list;
    }

    /**
     * 分页查询支持，支持简单的sql查询分页（复杂的查询，请自行编写对应的方法）
     *
     * @param sql
     * @param rm
     * @param args
     * @param pageNo
     * @param pageSize
     * @return
     */
    private Page simplePageQueryNotT(String sql, RowMapper rm, Map<String, ?> args, long pageNo, long pageSize) {
        // 首先查询总数
        String countSql = "select count(*) " + removeSelect(removeOrders(sql));
        long count = (Long)this.jdbcTemplateReadOnly().queryForMap(countSql, args).get("count(1)");
        if (count == 0) {
            return new Page();
        }
        // 有数据的情况下，继续查询
        long start = (pageNo - 1) * pageSize;
        sql = sql + " limit " + start + "," + pageSize;
        List list = this.jdbcTemplateReadOnly().query(sql, rm, args);
        return new Page(start, count, (int)pageSize, list);
    }

    /**
     * 去掉order
     *
     * @param sql
     * @return
     */
    private String removeOrders(String sql) {
        Pattern p = Pattern.compile("order\\s*by[\\w|\\W|\\s|\\S]*", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 去掉select
     *
     * @param sql
     * @return
     */
    private String removeSelect(String sql) {
        int beginPos = sql.toLowerCase().indexOf("from");
        return sql.substring(beginPos);
    }


    private long getMaxId(String table, String column) {
        String sql = "SELECT max(" + column + ") FROM " + table + " ";
        long maxId = (Long)this.jdbcTemplateReadOnly().queryForMap(sql).get("max(" + column + ")");
        return maxId;
    }

    /**
     * 生成简单对象UPDATE语句，简化sql拼接
     * @param tableName
     * @param pkName
     * @param pkValue
     * @param params
     * @return
     */
    private String makeSimpleUpdateSql(String tableName, String pkName, Object pkValue, Map<String, Object> params){
        if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
            return "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("update ").append(tableName).append(" set ");
        //添加参数
        Set<String> set = params.keySet();
        int index = 0;
        for (String key : set) {
//			 sb.append(key).append(" = :").append(key);
            sb.append(key).append(" = ?");
            if(index != set.size() - 1){
                sb.append(",");
            }
            index++;
        }
//		sb.append(" where ").append(pkName).append(" = :").append(pkName) ;
        sb.append(" where ").append(pkName).append(" = ?");
        params.put("where_" + pkName,params.get(pkName));

        return sb.toString();
    }


    /**
     * 生成简单对象UPDATE语句，简化sql拼接
     * @param pkName
     * @param pkValue
     * @param params
     * @return
     */
    private String makeSimpleUpdateSql(String pkName, Object pkValue, Map<String, Object> params){
        if(StringUtils.isEmpty(getTableName()) || params == null || params.isEmpty()){
            return "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("update ").append(getTableName()).append(" set ");
        //添加参数
        Set<String> set = params.keySet();
        int index = 0;
        for (String key : set) {
            sb.append(key).append(" = :").append(key);
            if(index != set.size() - 1){
                sb.append(",");
            }
            index++;
        }
        sb.append(" where ").append(pkName).append(" = :").append(pkName) ;

        return sb.toString();
    }



    /**
     * 生成对象INSERT语句，简化sql拼接
     * @param tableName
     * @param params
     * @return
     */
    private String makeSimpleReplaceSql(String tableName, Map<String, Object> params){
        if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("replace into ").append(tableName);

        StringBuffer sbKey = new StringBuffer();
        StringBuffer sbValue = new StringBuffer();

        sbKey.append("(");
        sbValue.append("(");
        //添加参数
        Set<String> set = params.keySet();
        int index = 0;
        for (String key : set) {
            sbKey.append(key);
            sbValue.append(" :").append(key);
            if(index != set.size() - 1){
                sbKey.append(",");
                sbValue.append(",");
            }
            index++;
        }
        sbKey.append(")");
        sbValue.append(")");

        sb.append(sbKey).append("VALUES").append(sbValue);

        return sb.toString();
    }

    /**
     * 生成对象INSERT语句，简化sql拼接
     * @param tableName
     * @param params
     * @return
     */
    private String makeSimpleReplaceSql(String tableName, Map<String, Object> params,List<Object> values){
        if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("replace into ").append(tableName);

        StringBuffer sbKey = new StringBuffer();
        StringBuffer sbValue = new StringBuffer();

        sbKey.append("(");
        sbValue.append("(");
        //添加参数
        Set<String> set = params.keySet();
        int index = 0;
        for (String key : set) {
            sbKey.append(key);
            sbValue.append(" ?");
            if(index != set.size() - 1){
                sbKey.append(",");
                sbValue.append(",");
            }
            index++;
            values.add(params.get(key));
        }
        sbKey.append(")");
        sbValue.append(")");

        sb.append(sbKey).append("VALUES").append(sbValue);

        return sb.toString();
    }



    /**
     * 生成对象INSERT语句，简化sql拼接
     * @param tableName
     * @param params
     * @return
     */
    private String makeSimpleInsertSql(String tableName, Map<String, Object> params){
        if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("insert into ").append(tableName);

        StringBuffer sbKey = new StringBuffer();
        StringBuffer sbValue = new StringBuffer();

        sbKey.append("(");
        sbValue.append("(");
        //添加参数
        Set<String> set = params.keySet();
        int index = 0;
        for (String key : set) {
            sbKey.append(key);
//			sbValue.append(" :").append(key);
            sbValue.append(" ?");
            if(index != set.size() - 1){
                sbKey.append(",");
                sbValue.append(",");
            }
            index++;
        }
        sbKey.append(")");
        sbValue.append(")");

        sb.append(sbKey).append("VALUES").append(sbValue);

        return sb.toString();
    }

    /**
     * 生成对象INSERT语句，简化sql拼接
     * @param tableName
     * @param params
     * @return
     */
    private String makeSimpleInsertSql(String tableName, Map<String, Object> params,List<Object> values){
        if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("insert into ").append(tableName);

        StringBuffer sbKey = new StringBuffer();
        StringBuffer sbValue = new StringBuffer();

        sbKey.append("(");
        sbValue.append("(");
        //添加参数
        Set<String> set = params.keySet();
        int index = 0;
        for (String key : set) {
            sbKey.append(key);
            sbValue.append(" ?");
            if(index != set.size() - 1){
                sbKey.append(",");
                sbValue.append(",");
            }
            index++;
            values.add(params.get(key));
        }
        sbKey.append(")");
        sbValue.append(")");

        sb.append(sbKey).append("VALUES").append(sbValue);

        return sb.toString();
    }


    private Serializable doInsertRuturnKey(Map<String,Object> params){
        final List<Object> values = new ArrayList<Object>();
        final String sql = makeSimpleInsertSql(getTableName(),params,values);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceWrite());
        try {

            jdbcTemplate.update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(

                        Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                    for (int i = 0; i < values.size(); i++) {
                        ps.setObject(i+1, values.get(i)==null?null:values.get(i));

                    }
                    return ps;
                }

            }, keyHolder);
        } catch (DataAccessException e) {
            System.out.println(e);
        }



        if (keyHolder == null) { return ""; }


        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null || keys.size() == 0 || keys.values().size() == 0) {
            return "";
        }
        Object key = keys.values().toArray()[0];
        if (key == null || !(key instanceof Serializable)) {
            return "";
        }
        if (key instanceof Number) {
            //Long k = (Long) key;
            Class clazz = key.getClass();
//			return clazz.cast(key);
            return (clazz == int.class || clazz == Integer.class) ? ((Number) key).intValue() : ((Number)key).longValue();


        } else if (key instanceof String) {
            return (String) key;
        } else {
            return (Serializable) key;
        }


    }


    /**
     * 生成默认的对象UPDATE语句，简化sql拼接
     * @param pkValue
     * @param params
     * @return
     */
    private String makeDefaultSimpleUpdateSql(Object pkValue, Map<String, Object> params){
        return this.makeSimpleUpdateSql(getTableName(), getPKColumn(), pkValue, params);
    }

    /**
     * 生成默认的对象INSERT语句，简化sql拼接
     * @param params
     * @return
     */
    private String makeDefaultSimpleInsertSql(Map<String, Object> params){
        return this.makeSimpleInsertSql(this.getTableName(), params);
    }

    /**
     * 获取一个实例对象
     * @param tableName
     * @param pkName
     * @param pkValue
     * @param rm
     * @return
     */
    private Object doLoad(String tableName, String pkName, Object pkValue, RowMapper rm){
        StringBuffer sb = new StringBuffer();
        sb.append("select * from ").append(tableName).append(" where ").append(pkName).append(" = ?");
        List<Object> list = this.jdbcTemplateReadOnly().query(sb.toString(), rm, pkValue);
        if(list == null || list.isEmpty()){
            return null;
        }
        return list.get(0);
    }


    /**
     * 删除实例对象，返回删除记录数
     * @param tableName
     * @param pkName
     * @param pkValue
     * @return
     */
    private int doDelete(String tableName, String pkName, Object pkValue) {
        StringBuffer sb = new StringBuffer();
        sb.append("delete from ").append(tableName).append(" where ").append(pkName).append(" = ?");
        int ret = this.jdbcTemplateWrite().update(sb.toString(), pkValue);
        return ret;
    }


    /**
     * 更新实例对象，返回删除记录数
     * @param tableName
     * @param pkName
     * @param pkValue
     * @param params
     * @return
     */
    private int doUpdate(String tableName, String pkName, Object pkValue, Map<String, Object> params){
        params.put(pkName, pkValue);
        String sql = this.makeSimpleUpdateSql(tableName, pkName, pkValue, params);
        int ret = this.jdbcTemplateWrite().update(sql, params.values().toArray());
        return ret;
    }

    /**
     * 更新实例对象，返回删除记录数
     * @param pkName
     * @param pkValue
     * @param params
     * @return
     */
    private int doUpdate( String pkName, Object pkValue, Map<String, Object> params){
        params.put(pkName, pkValue);
        String sql = this.makeSimpleUpdateSql( pkName, pkValue, params);
        int ret = this.jdbcTemplateWrite().update(sql, params.values().toArray());
        return ret;
    }



    private boolean doReplace(Map<String, Object> params) {
        String sql = this.makeSimpleReplaceSql(this.getTableName(), params);
        int ret = this.jdbcTemplateWrite().update(sql, params.values().toArray());
        return ret > 0;
    }

    private boolean doReplace(String tableName, Map<String, Object> params){
        String sql = this.makeSimpleReplaceSql(tableName, params);
        int ret = this.jdbcTemplateWrite().update(sql, params.values().toArray());
        return ret > 0;
    }


    /**
     * 插入
     * @param tableName
     * @param params
     * @return
     */
    private boolean doInsert(String tableName, Map<String, Object> params){
        String sql = this.makeSimpleInsertSql(tableName, params);
        int ret = this.jdbcTemplateWrite().update(sql, params.values().toArray());
        return ret > 0;
    }

    /**
     * 插入
     * @param params
     * @return
     */
    private boolean doInsert(Map<String, Object> params) {
        String sql = this.makeSimpleInsertSql(this.getTableName(), params);
        int ret = this.jdbcTemplateWrite().update(sql, params.values().toArray());
        return ret > 0;
    }


}
