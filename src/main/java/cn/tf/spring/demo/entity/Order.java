package cn.tf.spring.demo.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;


@Entity
@Table(name="t_order")
@Data
public class Order implements Serializable {
    private Long id;

    private Long memberId;
    private String detail;
    private Long createTime;
    private String createTimeFmt;

}
