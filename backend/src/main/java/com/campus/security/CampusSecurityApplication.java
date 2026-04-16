package com.campus.security;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.campus.security.mapper")
public class CampusSecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampusSecurityApplication.class, args);
	}

}
