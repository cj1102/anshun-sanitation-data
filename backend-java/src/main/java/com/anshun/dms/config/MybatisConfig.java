package com.anshun.dms.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.anshun.dms.mapper")
public class MybatisConfig { }
