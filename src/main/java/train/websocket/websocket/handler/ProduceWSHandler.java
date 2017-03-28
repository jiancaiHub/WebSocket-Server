package train.websocket.websocket.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import train.websocket.websocket.init.Producer;
import train.websocket.websocket.init.Purchaser;

import java.util.Map;

/**
 * Created by jiancai.wang on 2016/11/24.
 */
public class ProduceWSHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private Logger log = LoggerFactory.getLogger(ProduceWSHandler.class);
    private final static String DEFAULT_TOPIC = "default";
    private final ChannelGroup boosGroup;
    private Purchaser purchaser;
    private Map<String, Object> params;
    private Producer<byte[]> producer;

    /**
     * @param boosGroup
     * @param purchaser :   message manager
     * @param params    :   some param
     */
    public ProduceWSHandler(ChannelGroup boosGroup, Purchaser purchaser, Map<String, Object> params) {
        super();
        this.boosGroup = boosGroup;
        this.purchaser = purchaser;
        this.params = params;
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
            // 建立连接
            log.info("Client " + ctx.channel() + " joined!");
            // 注册一个生产者到purchaser
            String topic = params.containsKey("topic") ? params.get("topic").toString() : DEFAULT_TOPIC;
            producer = new Producer(ctx.channel().id().asShortText(), topic, purchaser);
            purchaser.registerProducer(producer);
            // 把当前建立的通道添加到group中
            boosGroup.add(ctx.channel());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame wsFrame) throws Exception {
        log.info("ProduceWSHandler read msg.......");
        ByteBuf directBuf = wsFrame.content();
        if (!directBuf.hasArray()) { //1
            int length = directBuf.readableBytes();//2 获取可读取字节数
            byte[] msg = new byte[length]; //3 分配型的数据来保存数据
            directBuf.getBytes(directBuf.readerIndex(), msg); //4 复制字节
            producer.produce(msg);//5 一些操作
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
        log.info("ProduceWSHandler is activating..........");
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
        purchaser.unRegisterProduce(producer);
        log.info("ProduceWSHandler is inactivating..........");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
