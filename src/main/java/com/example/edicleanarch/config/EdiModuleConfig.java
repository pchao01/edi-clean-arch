package com.example.edicleanarch.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.lis.ediprocessor.edi")
@MapperScan(basePackages = {
        "com.example.edicleanarch.railinc.adapter.out",
        "com.example.edicleanarch.edi315.adapter.out"
})
public class EdiModuleConfig {
}
