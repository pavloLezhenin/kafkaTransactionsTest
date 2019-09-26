package com.kafkaTest.services;

import com.kafkaTest.entity.Counter;

public interface ICounterService {
    Counter getCounter(String topic);
    void saveCounter(Counter counter);
}
