package com.study.english;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.study.english.mapper")
public class EnglishApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishApplication.class, args);
    }

}
