package com.kafkaTest.jpa;

import com.kafkaTest.entity.Counter;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface CounterRepository extends CrudRepository<Counter, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Counter> findByTopic(String topic);

    Counter save(Counter counter);
}
