package train.websocket.websocket.handler;

import io.netty.buffer.ByteBuf;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by jiancai.wang on 2017/1/10.
 */
public class MaintenanceWSHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final Logger log = LoggerFactory.getLogger(ConsumeWSHandler.class);
    private final static String MAINTENANCE_TOPIC = "maintenance";
    private final static String POISON_PILL = "poison_pill";
    private final static int DEFAULT_CAPACITY = 10;

    private final ChannelGroup boosGroup;
    private Purchaser purchaser;
    private Customer<byte[]> maintenance;
    private ScheduledExecutorService schedule;

    public MaintenanceWSHandler(ChannelGroup boosGroup, Purchaser purchaser) {
        super();
        this.boosGroup = boosGroup;
        this.purchaser = purchaser;
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
            // 用来发送运维结果， 但不会注册到group中用来消费数据
            this.maintenance = new Customer(ctx.channel().id().asShortText(), MAINTENANCE_TOPIC, purchaser, DEFAULT_CAPACITY);
            // 把当前建立的通道添加到group中
            boosGroup.add(ctx.channel());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame wsFrame) throws Exception {
        log.info("MaintenanceWSHandler read msg.......");
        ByteBuf directBuf = wsFrame.content();
        if (!directBuf.hasArray()) { //1
            int length = directBuf.readableBytes();//2 获取可读取字节数
            byte[] msg = new byte[length]; //3 分配型的数据来保存数据
            directBuf.getBytes(directBuf.readerIndex(), msg); //4 复制字节
            maintenance.put(msg);//5 一些操作

        }
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
                if (maintenance != null) {
                    byte[] msg = maintenance.take();
                    try {
                        String option = new String(msg, "UTF-8");
                        TextWebSocketFrame frame = new TextWebSocketFrame(purchaser.desc());
                        if (option.equals(POISON_PILL))
                            break;
                        if (option.contains(" ")) {
                            switch (option.split(" ")[0]) {
                                case "-c":
                                    break;
                                case "-p":
                                    break;
                                default:
                                    frame = new TextWebSocketFrame(purchaser.desc());
                                    break;
                            }
                        }
                        ctx.channel().writeAndFlush(frame);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        schedule.submit(run);
        log.info("MaintenanceWSHandler is activating..........");
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
        log.info("MaintenanceWSHandler is inactivating..........");
        // 毒丸任务取消
        if (!schedule.isShutdown()) {
            this.maintenance.put(POISON_PILL.getBytes());
        }
    }
}
