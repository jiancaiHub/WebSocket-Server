package train.websocket.websocket.init;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于数据消费，在数据推送前做相关的过滤或加工
 * <p>
 * Created by jiancai.wang on 2016/11/28.
 */
public class Customer<T> {

    /**
     * 消费者可以拥有多个Topic, Purchaser会将消费者加到不同主题的Group中
     */
    // 标识符
    public final String name;
    // 消息主题清单
    public final String topic;
    // 队列能力
    public final int capacity;
    // 采购员
    public final Purchaser purchaser;
    // 消费者消息队列
    private final BlockingQueue<T> messageQueue;
    // 发送消息计数器
    public final AtomicLong sentMsgCounter = new AtomicLong(0);
    // 上线时间
    public final Date upTime;
    //
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Customer(String name, String topic, Purchaser purchaser, int capacity) {
        this.name = name;
        this.topic = topic;
        this.purchaser = purchaser;
        this.capacity = capacity;
        this.messageQueue = new LinkedBlockingDeque<>(capacity);
        this.upTime = new Date();
    }

    public void put(T msg) {
        try {
            messageQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public T take() {
        try {
            sentMsgCounter.incrementAndGet();
            return messageQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String desc() {
        return "{" +
                "topic:'" + topic + '\'' +
                ", name:'" + name + '\'' +
                ", upTime:'" + sdf.format(upTime) + '\'' +
                ", sentMsgCounter:" + sentMsgCounter.get() +
                '}';
    }

    public void close() {
        if (messageQueue != null) {
            messageQueue.clear();
        }

    }
}
