package cn.tf.spring.demo.dao;


import cn.tf.spring.demo.entity.Order;
import cn.tf.spring.framework.BaseDaoSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.sql.DataSource;


@Repository
public class OrderDao extends BaseDaoSupport<Order, Long> {

    @Resource(name="dataSource")
    public void setDataSource(DataSource dataSource){
        super.setDataSourceReadOnly(dataSource);
        super.setDataSourceWrite(dataSource);
    }


    protected OrderDao() throws Exception {
    }

    @Override
    protected String getPKColumn() {
        return "id";
    }
}
