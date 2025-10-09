package com.pnu.momeet.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "momeet.temperature.bayesian")
public record TemperatureProperties(double priorK) {
}
