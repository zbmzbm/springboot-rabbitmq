package com.oneice.idgeneratortest;


import com.alibaba.fastjson.JSON;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Oneice
 * @since 2023/1/16 16:50
 */
@RestController
public class TestSend {
    @Autowired
    @Qualifier("demoRabbitMqTemplates")
    private RabbitTemplate[] rabbitTemplates;

    @Autowired
    @Qualifier("demoRabbitMqAdmins")
    private RabbitAdmin[] admins;

    @GetMapping("/test")
    public void testRabbitMq() {
        CorrelationData data=new CorrelationData("1");
        //rabbitTemplates[0].convertAndSend("zbm.test.exchange.2","",JSON.parse(JSON.toJSONString(data)),data);
        admins[0].declareExchange(new FanoutExchange("zbm-test1111"));
    }

}
