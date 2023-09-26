package com.demo.business.configure;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Oneice
 * @since 2023/1/16 15:32
 */
@Setter
@Getter
@Primary
@ConfigurationProperties(prefix = "demo.mq")
public class MqProperties {

    private List<ConnectConfig> connect;


    @Data
    public static class ConnectConfig{
        private String host;
        private int port=5672;
        private String userName;
        private String password;
        private String virtualHost;
        private int channelCacheSize = 100;
        private Boolean publisherConfirms = true;
        private Boolean publisherReturns = true;

        private List<BindConfig> bind;

        private List<ExchangeConfig> exchange;

        private List<QueueConfig> queue;

    }

    @Data
    static class QueueConfig{
        private String name;
        private boolean durable=true;
        private boolean exclusive;
        private boolean autoDelete;
        private String arguments;
    }

    @Data
    public static class ExchangeConfig{
        private String name;
        private String type;
        private boolean durable=true;
        private boolean autoDelete;
        private String arguments;

    }

    @Data
    public static class BindConfig{
        private String routeKey;
        private String exchange;
        private String queue;

    }
}
