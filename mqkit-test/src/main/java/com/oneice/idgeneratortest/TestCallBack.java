package com.oneice.idgeneratortest;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 说明：消息发送者-》判断消息是否发送到exchange（判断交换机是否存在）
 * @author zbm
 * @date 2023/2/311:17
 */
@Component
public class TestCallBack implements RabbitTemplate.ConfirmCallback{

     @Autowired
     @Qualifier("demoRabbitMqTemplates")
     private RabbitTemplate[] rabbitTemplates;

     @PostConstruct
     public void init() {
         rabbitTemplates[0].setMandatory(true);
         rabbitTemplates[0].setConfirmCallback(this);
     }


    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        System.out.println(correlationData);
        System.out.println("call back ");
        if (ack) {
            System.out.println("消息成功发送到exchange");
        } else {
            System.out.println("消息发送exchange失败:" + cause);
        }
    }
}
