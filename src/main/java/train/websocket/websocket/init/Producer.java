package train.websocket.websocket.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jiancai.wang on 2016/12/19.
 */
public class Producer<T> {

    /**
     * 生产者
     * 1. 每个生产者都有特定的主题，用来负责生产主题下的消息。 主题：消息的标识符
     * 2. 能够生产消息（往往生产消息的动作是由其他的线程来实现，比如websocket接收消息）
     */
    private Logger log = LoggerFactory.getLogger(Producer.class);
    // 标识符
    public final String name;
    // 消息主题
    public final String topic;
    // 采购员
    public final Purchaser purchaser;
    // 发送消息计数器
    public final AtomicLong receivedMsgCounter = new AtomicLong(0);
    // 上线时间
    public final Date upTime;
    //
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Producer(String name, String topic, Purchaser purchaser) {
        this.name = name;
        this.topic = topic;
        this.purchaser = purchaser;
        this.upTime = new Date();
    }

    public void produce(T msg) {
        if (purchaser.getGroup(topic).getOption() != null) {
            purchaser.getGroup(topic).getOption().accept(msg);
            receivedMsgCounter.incrementAndGet();
        }
    }

    public String desc() {
        return "{" +
                "topic:'" + topic + '\'' +
                ", name:'" + name + '\'' +
                ", upTime:'" + sdf.format(upTime) + '\'' +
                ", receivedMsgCounter:" + receivedMsgCounter.get() +
                '}';
    }
}
