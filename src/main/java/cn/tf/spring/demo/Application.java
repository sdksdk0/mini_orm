package cn.tf.spring.demo;


import cn.tf.spring.demo.dao.MemberDao;
import cn.tf.spring.demo.entity.Member;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

@ContextConfiguration(locations = "classpath:application.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class Application {

    @Autowired
    private MemberDao memberDao;

    @Test
    public void test() throws Exception {
        List<Member>  list =  memberDao.selectListByName("小明");
        System.out.println(Arrays.toString(list.toArray()));

        List<Member>  list1 =  memberDao.selectAll();
        System.out.println(Arrays.toString(list1.toArray()));

        List<Member>  list2 =  memberDao.selectListBetween("age",20,25);
        System.out.println(Arrays.toString(list2.toArray()));

        Member member = new Member();
        member.setAddr("aa");
        member.setAge(30);
        member.setName("王明");
        memberDao.insert(member);


    }
}
