package train.websocket.websocket.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jiancai.wang on 2016/11/24.
 */
public class EchoWSHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final Logger log = LoggerFactory.getLogger(EchoWSHandler.class);
    private final ChannelGroup boosGroup;

    public EchoWSHandler(ChannelGroup boosGroup) {
        super();
        this.boosGroup = boosGroup;
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
            // boosGroup 类型端口
            boosGroup.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " joined!"));
            // 把当前建立的通道添加到group中
            boosGroup.add(ctx.channel());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame wsFrame) throws Exception {
        log.info("EchoWSHandler read msg: " + wsFrame.toString());
        boosGroup.writeAndFlush(wsFrame);
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
        log.info("EchoWSHandler is activating..........");
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
        log.info("EchoWSHandler is inactivating..........");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
