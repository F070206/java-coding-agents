package com.javacodingagent.infrastructure.config;
public record ExternalServiceProperties(String mysqlUrl, String redisHost, String rocketMqEndpoints, String milvusUri) { }
