package cn.tf.spring.demo.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


@Entity
@Table(name="t_member")
@Data
public class Member implements Serializable {
    @Id private Long id;
    private String name;
    private String addr;
    private Integer age;

}
