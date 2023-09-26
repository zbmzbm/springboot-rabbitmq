package com.oneice.idgeneratortest;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 说明：生产者-》消息是否由交换机exchange发送到队列（判断队列是否存在）
 * @author zbm
 * @date 2023/2/311:48
 */
@Setter
@Slf4j
@Component
public class TestReturnBack implements RabbitTemplate.ReturnsCallback {

    @Autowired
    @Qualifier("demoRabbitMqTemplates")
    private RabbitTemplate[] rabbitTemplates;

    @PostConstruct
    public void init() {
        rabbitTemplates[0].setMandatory(true);
    }

    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.error("发送RabbitMQ消息returnedMessage，出现异常，Exchange不存在或发送至Exchange却没有发送到Queue中");
    }

}
