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
public class MemberDao extends BaseDaoSupport<Member,Long> {

    @Autowired
    private void setDataSource(DataSource dataSource) {
        super.setReadonlyDataSource(dataSource);
        super.setWriteDataSource(dataSource);
    }

    public MemberDao() throws Exception {
    }
    public List<Member> selectAll() {
        QueryRule queryRule =  QueryRule.getInstance();
        queryRule.getRuleList();
        return super.select(queryRule);
    }

    public List<Member> selectListByName(String name) {
        QueryRule queryRule = QueryRule.getInstance();
        queryRule.andEqual("name", name);
        return super.select(queryRule);
    }

    public List<Member> selectListBetween(String type,int min,int max) {
        QueryRule queryRule = QueryRule.getInstance();
        queryRule.andBetween(type,min,max);
        return super.select(queryRule);
    }

}
