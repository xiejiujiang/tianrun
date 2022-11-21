package com.example.tianrun.service.impl;

import com.example.tianrun.mapper.orderMapper;
import com.example.tianrun.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.example.tianrun.service.impl.TestServiceImpl.class);

    @Autowired
    private orderMapper orderMapper;


    @Override
    public void testService(String str) {
        LOGGER.error("--------------------------- str == " + str + " ---------------------------");
    }
}
