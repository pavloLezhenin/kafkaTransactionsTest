package com.kafkaTest.services;

import com.kafkaTest.entity.Counter;
import com.kafkaTest.jpa.CounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CounterService implements ICounterService {

    private CounterRepository counterRepository;

    @Autowired
    public CounterService(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Override
    public Counter getCounter(String topic) {
        return counterRepository.findByTopic(topic)
                .orElse(new Counter(topic));
    }

    @Override
    public void saveCounter(Counter counter) {
        counterRepository.save(counter);
    }
}
