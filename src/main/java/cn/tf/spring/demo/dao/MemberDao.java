package cn.tf.spring.demo.dao;


import cn.tf.spring.demo.entity.Member;
import cn.tf.spring.framework.BaseDaoSupport;
import cn.tf.spring.framework.QueryRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.List;


@Repository
public  class MemberDao extends BaseDaoSupport<Member,Long> {

    @Resource(name="dataSource")
    public void setDataSource(DataSource dataSource){
        super.setDataSourceReadOnly(dataSource);
        super.setDataSourceWrite(dataSource);
    }
    public MemberDao() throws Exception {
    }

    @Override
    protected String getPKColumn() {
        return "id";
    }

    public List<Member> selectAll() throws Exception {
        QueryRule queryRule = QueryRule.getInstance();
        return super.select(queryRule);
    }

    public List<Member> selectListByName(String name) throws Exception {
        QueryRule queryRule = QueryRule.getInstance();
        queryRule.andEqual("name", name);
        return super.select(queryRule);
    }

    public List<Member> selectListBetween(String type,int min,int max) throws Exception {
        QueryRule queryRule = QueryRule.getInstance();
        queryRule.andBetween(type,min,max);
        return super.select(queryRule);
    }

    public boolean insert(Member m) throws Exception {
        return super.insert(m);
    }

}
