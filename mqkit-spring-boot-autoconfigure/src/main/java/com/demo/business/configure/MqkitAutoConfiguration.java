package com.demo.business.configure;

import com.alibaba.fastjson.JSON;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zbm
 * @since 2023年01月31日16:55:08
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({MqProperties.class})
@ComponentScan({"com.demo.business.configure"})
public class MqkitAutoConfiguration implements InitializingBean, BeanPostProcessor {

    @Autowired
    private MqProperties connectProperties;

    public static final RabbitTemplate[] rabbitTemplates = new RabbitTemplate[10];

    public static final RabbitAdmin[] rabbitAdmins = new RabbitAdmin[10];

    public static final SimpleRabbitListenerContainerFactory[] listenerContainerFactories = new SimpleRabbitListenerContainerFactory[10];

    public static final CachingConnectionFactory[] cachingConnectionFactories = new CachingConnectionFactory[10];

    public MqkitAutoConfiguration() {
    }

    public static void initMqInstance(MqProperties properties) {
        if (Objects.isNull(properties) || CollectionUtils.isEmpty(properties.getConnect())) {
            log.warn("mqProperties connect is empty");
            return;
        }
        for (int i = 0; i < properties.getConnect().size(); i++) {
            CachingConnectionFactory connectionFactory = connectionFactory(
                    properties.getConnect().get(i).getHost(),
                    properties.getConnect().get(i).getPort(),
                    properties.getConnect().get(i).getUserName(),
                    properties.getConnect().get(i).getPassword(),
                    properties.getConnect().get(i).getVirtualHost(),
                    properties.getConnect().get(i).getPublisherReturns());
            cachingConnectionFactories[i] = connectionFactory;
            RabbitTemplate rabbitTemplate = rabbitTemplate(connectionFactory);
            rabbitTemplates[i] = rabbitTemplate;
            RabbitAdmin rabbitAdmin = rabbitAdmin(connectionFactory);
            rabbitAdmins[i] = rabbitAdmin;
            SimpleRabbitListenerContainerFactory factory = rabbitListenerContainerFactory(connectionFactory, "manual", 50);//手动确认，并设值均衡消费
            listenerContainerFactories[i] = factory;
            initExchangeAndQueue(rabbitAdmin, properties.getConnect().get(i));
        }
    }

    @Bean({"rabbitConnectionFactory"})
    @ConditionalOnMissingBean(
            name = {"rabbitConnectionFactory"}
    )
    public CachingConnectionFactory cachingConnectionFactory() {
        return cachingConnectionFactories[0];
    }

    @Bean({"demoRabbitMqTemplates"})
    public static RabbitTemplate[] rabbitTemplates() {
        return rabbitTemplates;
    }

    @Bean({"demoRabbitMqAdmins"})
    public static RabbitAdmin[] rabbitAdmins() {
        return rabbitAdmins;
    }

    @Bean({"firstRabbitMqListenFactory"})
    public static SimpleRabbitListenerContainerFactory listenerContainerFactoriesOne() {
        return listenerContainerFactories[0];
    }

    @Bean({"secondRabbitMqListenFactory"})
    public static SimpleRabbitListenerContainerFactory listenerContainerFactoriesTwo() {
        return listenerContainerFactories[1];
    }

    @Bean({"threeRabbitMqListenFactory"})
    public static SimpleRabbitListenerContainerFactory listenerContainerFactoriesThree() {
        return listenerContainerFactories[2];
    }

    public static void initExchangeAndQueue(RabbitAdmin rabbitAdmin, MqProperties.ConnectConfig connect) {
        if (Objects.isNull(connect) || CollectionUtils.isEmpty(connect.getExchange())) {
            log.warn("mqProperties exchange is empty");
            return;
        }
        Exchange exchange = null;
        Map<String, Exchange> exchangeMap = new HashMap<>();
        for (MqProperties.ExchangeConfig exchangeConfig : connect.getExchange()) {
            String type = exchangeConfig.getType();
            if (StringUtils.isEmpty(type)) {
                log.warn("exchangeConfig[{}] type is empty", exchangeConfig.getName());
            }
            Map argsMap = null;
            if (!StringUtils.isEmpty(exchangeConfig.getArguments())) {
                argsMap = JSON.parseObject(exchangeConfig.getArguments(), Map.class);
            }
            if (type.equalsIgnoreCase(ExchangeType.DIRECT.getDesc())) {
                exchange = new DirectExchange(exchangeConfig.getName(), exchangeConfig.isDurable(), exchangeConfig.isAutoDelete(), argsMap);
            }
            if (type.equalsIgnoreCase(ExchangeType.FANOUT.getDesc())) {
                exchange = new FanoutExchange(exchangeConfig.getName(), exchangeConfig.isDurable(), exchangeConfig.isAutoDelete(), argsMap);
            }

            if (type.equalsIgnoreCase(ExchangeType.TOPIC.getDesc())) {
                exchange = new TopicExchange(exchangeConfig.getName(), exchangeConfig.isDurable(), exchangeConfig.isAutoDelete(), argsMap);
            }
            rabbitAdmin.declareExchange(exchange);
            exchangeMap.put(exchangeConfig.getName(), exchange);
        }
        if (CollectionUtils.isEmpty(connect.getQueue())) {
            log.warn("mqProperties queue is empty");
            return;
        }
        Map<String, Queue> queueMap = new HashMap<>();
        for (MqProperties.QueueConfig queueConfig : connect.getQueue()) {
            if (!StringUtils.isEmpty(queueConfig.getName())) {
                Map argsMap = null;
                if (!StringUtils.isEmpty(queueConfig.getArguments())) {
                    argsMap = JSON.parseObject(queueConfig.getArguments(), Map.class);
                }
                Queue queue = new Queue(queueConfig.getName(), queueConfig.isDurable(), queueConfig.isExclusive(), queueConfig.isAutoDelete(), argsMap);
                rabbitAdmin.declareQueue(queue);
                queueMap.put(queueConfig.getName(), queue);
            }
        }
        if (CollectionUtils.isEmpty(connect.getBind())) {
            log.warn("mqProperties bind is empty");
            return;
        }
        for (MqProperties.BindConfig bindingConfig : connect.getBind()) {
            String routeKey = bindingConfig.getRouteKey();
            String queueName = bindingConfig.getQueue();
            String exchangeName = bindingConfig.getExchange();
            if (!StringUtils.isEmpty(queueName) && !StringUtils.isEmpty(exchangeName)) {
                Exchange exchanges = exchangeMap.get(exchangeName);
                if (null == exchanges) {
                    log.error("Exchange[{}] is null.", routeKey);
                } else if (!(exchanges instanceof FanoutExchange) && StringUtils.isEmpty(routeKey)) {
                    log.error("Fanout Exchange[{}] dos not support empty route key.", routeKey);
                } else {
                    Queue queue = queueMap.get(queueName);
                    if (queue == null) {
                        log.error("Queue[{}] is null.", queueName);
                    } else if (exchanges instanceof TopicExchange) {
                        TopicExchange topExchange = (TopicExchange) exchanges;
                        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(topExchange).with(routeKey));
                    } else if (exchanges instanceof FanoutExchange) {
                        FanoutExchange fanoutExchange = (FanoutExchange) exchanges;
                        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(fanoutExchange));
                    } else if (exchanges instanceof DirectExchange) {
                        DirectExchange directExchange = (DirectExchange) exchanges;
                        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(directExchange).with(routeKey));
                    }
                }
            } else {
                log.error("bind for queue[{}] or exchange[{}] is empty", queueName, exchangeName);
            }
        }
    }

    public static CachingConnectionFactory connectionFactory(String host, int port, String username, String password, String virtualHost,boolean publisherReturns) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(publisherReturns);
        return connectionFactory;
    }

    public static RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    public static SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            String acknowledge,
            Integer prefetch) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.valueOf(acknowledge.toUpperCase()));
        factory.setPrefetchCount(prefetch);
        return factory;
    }

    public static RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Override
    public void afterPropertiesSet(){
        initMqInstance(this.connectProperties);
    }
}
