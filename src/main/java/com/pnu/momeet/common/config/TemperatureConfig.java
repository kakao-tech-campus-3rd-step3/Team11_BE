package com.pnu.momeet.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TemperatureProperties.class)
public class TemperatureConfig {}
