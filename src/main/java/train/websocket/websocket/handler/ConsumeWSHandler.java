package train.websocket.websocket.handler;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import train.websocket.websocket.init.Customer;
import train.websocket.websocket.init.Purchaser;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by jiancai.wang on 2016/11/24.
 */
public class ConsumeWSHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final Logger log = LoggerFactory.getLogger(ConsumeWSHandler.class);
    private final static String DEFAULT_TOPIC = "default";
    private final static String POISON_PILL = "poison_pill";
    private final static int DEFAULT_CAPACITY = 10;

    private final ChannelGroup boosGroup;
    private Purchaser purchaser;
    private Map<String, Object> params;
    private Customer<byte[]> customer;
    private ScheduledExecutorService schedule;

    /**
     * @param boosGroup
     * @param purchaser : manage message
     * @param params    : containing some param about message.
     */
    public ConsumeWSHandler(ChannelGroup boosGroup, Purchaser purchaser, Map<String, Object> params) {
        super();
        this.boosGroup = boosGroup;
        this.purchaser = purchaser;
        this.params = params;
        this.schedule = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * official:Gets called if an user event was triggered.
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        // HANDSHAKE_COMPLETE ws 捂手完成
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            // 当http连接成后。会将http报文处理的handler移除，消息直接传输给websocket frame处理handler
            ctx.pipeline().remove(HttpRequestHandler.class);
            // 连接成功
            log.info("Client " + ctx.channel() + " joined!");
            // add customer to clerk's list of customer.
            String topic = params.containsKey("topic") ? params.get("topic").toString() : DEFAULT_TOPIC;
            this.customer = new Customer(ctx.channel().id().asShortText(), topic, purchaser, DEFAULT_CAPACITY);
            purchaser.registerConsumer(customer);
            // 把当前建立的通道添加到group中
            boosGroup.add(ctx.channel());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame wsFrame) throws Exception {
        log.info("ConsumeWSHandler read msg: " + wsFrame.toString());
    }

    /**
     * Get calls when this channel was start
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        Runnable run = () -> {
            while (true) {
                if (customer != null) {
                    byte[] msg = customer.take();
                    try {
                        String message = new String(msg, "UTF-8");
                        if (message.equals(POISON_PILL))
                            break;
                        ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        schedule.submit(run);
        log.info("ConsumeWSHandler is activating..........");
    }

    /**
     * Get calls when this channel was stop.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("ConsumeWSHandler is inactivating..........");
        // remove customer from clerk
        purchaser.unRegisterConsumer(customer);
        // 毒丸任务取消
        if (!schedule.isShutdown()) {
            this.customer.put(POISON_PILL.getBytes());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("ConsumeWSHandler is inactivating..........");
        // remove customer from clerk
        purchaser.unRegisterConsumer(customer);
        // 毒丸任务取消
        if (!schedule.isShutdown()) {
            this.customer.put(POISON_PILL.getBytes());
        }
        ctx.close();
        cause.printStackTrace();
    }
}
