### 目的
**由于开发时经常会用到mq进行开发，每次开发声名队列，交换机，绑定关系比较麻烦，要写一堆配置文件，为了简化开发流程，故封装了一个我们的mq的组件，已上传到私服，只用在nacos上配置就可以实现上述功能。**


### 一，引入mq组件
```
 <dependency>
       <groupId>com.demo.business</groupId>
       <artifactId>mqkit-spring-bootstarter</artifactId>
       <version>1.0-SNAPSHOT</version>
 </dependency>
```
### 二，nacos配置
```
一，配置mq链接信息
demo.mq.connect[0].host=39.106.150.48
demo.mq.connect[0].port=5672
demo.mq.connect[0].user-name=xxx
demo.mq.connect[0].password=xxx
demo.mq.connect[0].virtual-host=xxx

二，声名queue
demo.mq.connect[0].queue[0].name=zbm.test.queue.1
#设置死性队列，延迟队列
demo.mq.connect[0].queue[0].arguments={"x-message-ttl":10,"x-dead-letter-exchange":"mq_test","x-dead-letter-routing-key":"mq_test"}


三，声名exchange
demo.mq.connect[0].exchange[0].name=zbm.test.exchange.1
demo.mq.connect[0].exchange[0].type=direct

四，绑定queue和exchange
demo.mq.connect[0].bind[0].queue=zbm.test.queue.1
demo.mq.connect[0].bind[0].exchange=zbm.test.exchange.1
demo.mq.connect[0].bind[0].route-key=zbm.test.route.1
```

### 三，发送消息
```
@Autowired
@Qualifier("demoRabbitMqTemplates")
private RabbitTemplate[] rabbitTemplates;

@GetMapping("/test")
public void testRabbitMq() {
    CorrelationData data=new CorrelationData("1");
    rabbitTemplates[0].convertAndSend("zbm.test.exchange.2","zbm.test.route.2","7777",data);
}

```
### 四，接收消息
```
接收配置的链接1的消息
@RabbitListener(queues = "zbm.test.queue.2",containerFactory = "firstRabbitMqListenFactory")
public void handleOneFeedbackMessage(Message message, Channel channel) throws IOException {
    String messageText = new String(message.getBody());
    System.out.println(messageText);
    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
}

接收配置的链接2的消息
@RabbitListener(queues = "zbm.test.queue.2",containerFactory = "secondRabbitMqListenFactory")
public void handleTwoFeedbackMessage(Message message, Channel channel) throws IOException {
    String messageText = new String(message.getBody());
    System.out.println(messageText);
    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
}
```

### 五，接收ReturnsCallback

```
@Autowired
@Qualifier("demoRabbitMqTemplates")
private RabbitTemplate[] rabbitTemplates;

@PostConstruct
public void init() {
    rabbitTemplates[1].setMandatory(true);
}

@Override
public void returnedMessage(ReturnedMessage returnedMessage) {
    log.error("发送RabbitMQ消息returnedMessage，出现异常，Exchange不存在或发送至Exchange却没有发送到Queue中");
}
```

### 六，接收ConfirmCallback
```
@Autowired
 @Qualifier("demoRabbitMqTemplates")
 private RabbitTemplate[] rabbitTemplates;

 @PostConstruct
 public void init() {
     rabbitTemplates[1].setMandatory(true);
     rabbitTemplates[1].setConfirmCallback(this);
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
```
