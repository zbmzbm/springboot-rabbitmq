package com.oneice.idgeneratortest;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author zbm
 * @date 2023/2/116:45
 */
@Component
public class TestListen {


    //@RabbitListener(queues = {"zbm.test.queue.2","zbm.test.queue.1"},containerFactory = "firstRabbitMqListenFactory")
    public void handleOneFeedbackMessage(Message message, Channel channel) throws IOException {
        System.out.println(message.getMessageProperties().getConsumerQueue());
        String messageText = new String(message.getBody());
        System.out.println(messageText);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //@RabbitListener(queues = "zbm.test.queue.2",containerFactory = "secondRabbitMqListenFactory")
    public void handleTwoFeedbackMessage(Message message, Channel channel) throws IOException {
        String messageText = new String(message.getBody());
        System.out.println(messageText);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
