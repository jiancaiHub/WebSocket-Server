package train.websocket.websocket.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by jiancai.wang on 2016/12/19.
 */
public class Group<T> {

    private Logger log = LoggerFactory.getLogger(Group.class);
    // 主题
    private String topic;
    // 生产逻辑
    private Consumer<T> option;
    // 消费者组
    private final Set<Customer> customerSet = new CopyOnWriteArraySet<>();
    // 生产者组
    private final Set<Producer<T>> producerSet = new CopyOnWriteArraySet<>();

    /**
     * 需要对组进行监控
     * 监控指标：
     * 1. 生产者列表
     * 2. 消费者列表
     * 3. 生产者描述
     * *    生产者name
     * *    生产者在线时间
     * *    生产者接收消息总数
     * *    生产者发送消息数目
     * 4. 消费者描述
     * *    消费者name
     * *    消费者在线时间
     * *    消费者接收消息总数
     * *    消费者发送消息数目
     *
     * @param topic
     */

    public Group(String topic) {
        this.topic = topic;
        this.option = (msg) -> {
            if (!customerSet.isEmpty()) {
                customerSet.forEach(customer -> customer.put(msg));
            }
        };
    }

    public Group(String topic, Consumer<T> option) {
        this.topic = topic;
        this.option = option;
    }

    public Consumer<T> getOption() {
        return option;
    }

    public Set<Producer<T>> getProducerSet() {
        return producerSet;
    }

    public Set<Customer> getCustomerSet() {
        return customerSet;
    }

    public void addConsumer(Customer<T> customer) {
        customerSet.add(customer);
    }

    public void removeConsumer(Customer<T> customer) {
        if (customerSet.contains(customer))
            customerSet.remove(customer);
    }

    public void addProducer(Producer<T> producer) {
        producerSet.add(producer);
    }

    public void removeProducer(Producer<T> producer) {
        if (producerSet.contains(producer))
            producerSet.remove(producer);
    }

    public String desc() {
        return "{" +
                "topic:'" + topic + '\'' +
                ", producerSet:" + producerSet.stream().map(p -> p.desc()).collect(Collectors.joining(",", "[", "]")) +
                ", customerSet:[" + customerSet.stream().map(c -> c.desc()).collect(Collectors.joining(",", "[", "]")) +
                '}';
    }
}
