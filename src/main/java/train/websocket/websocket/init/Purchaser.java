package train.websocket.websocket.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by jiancai.wang on 2016/12/19.
 */
public class Purchaser {

    /**
     * 采购员
     * 消费者先提交消息主题
     * 等对应消息的主题的生产者
     * <p>
     * 生产者只会生产一种产品，消费者也只能消费一种产品
     */
    private Logger log = LoggerFactory.getLogger(Purchaser.class);
    // 生产消费组
    private Map<String, Group> groupMap = new ConcurrentHashMap<>();


    public Group getGroup(String topic) {
        return groupMap.get(topic);
    }

    public void registerProducer(Producer producer) {
        if (!groupMap.containsKey(producer.topic))
            groupMap.put(producer.topic, new Group(producer.topic));
        groupMap.get(producer.topic).addProducer(producer);
        log.info("生产客户端注册成功：" + producer.desc());
    }

    public void registerConsumer(Customer customer) {
        if (!groupMap.containsKey(customer.topic))
            groupMap.put(customer.topic, new Group(customer.topic));
        groupMap.get(customer.topic).addConsumer(customer);
        log.info("消费客户端注册成功：" + customer.desc());
    }

    public void unRegisterProduce(Producer producer) {
        if (groupMap.containsKey(producer.topic))
            groupMap.get(producer.topic).removeProducer(producer);
        log.info("生产客户端掉线：" + producer.desc());

    }

    public void unRegisterConsumer(Customer customer) {
        if (!groupMap.containsKey(customer.topic))
            groupMap.put(customer.topic, new Group(customer.topic));
        groupMap.get(customer.topic).removeConsumer(customer);
        log.info("生产客户端掉线：" + customer.desc());
    }

    public void close() {
        if (!groupMap.isEmpty()) {
            groupMap.clear();
        }
    }

    public String desc() {
        return "{" +
                "groupMap:" + groupMap.values().stream().map(g -> g.desc()).collect(Collectors.joining(" ,", "[", "]")) +
                '}';
    }
}
