package com.kafkaTest.entity;

import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "counter")
@DynamicUpdate
public class Counter implements Serializable {

    public Counter(String topic) {
        this.topic = topic;
    }

    public Counter() {}

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    Integer id;

    @Column(unique = true)
    String topic;

    Integer counter = 0;

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    public Integer getCounter() {
        return counter;
    }
}
