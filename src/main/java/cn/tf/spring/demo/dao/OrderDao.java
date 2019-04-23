package cn.tf.spring.demo.dao;


import cn.tf.spring.demo.entity.Order;
import cn.tf.spring.framework.BaseDaoSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;


@Repository
public class OrderDao extends BaseDaoSupport<Order, Long> {

    @Autowired
    private void setDataSource(DataSource dataSource) {
        super.setReadonlyDataSource(dataSource);
        super.setWriteDataSource(dataSource);
    }

    protected OrderDao() throws Exception {
    }
}
