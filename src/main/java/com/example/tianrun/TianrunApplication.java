package com.example.tianrun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(value = "com.example.tianrun.mapper")
public class TianrunApplication {

    public static void main(String[] args) {
        SpringApplication.run(TianrunApplication.class, args);
    }

}
