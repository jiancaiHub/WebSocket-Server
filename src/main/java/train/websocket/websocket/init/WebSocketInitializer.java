package train.websocket.websocket.init;


import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import train.websocket.websocket.handler.EchoWSHandler;
import train.websocket.websocket.handler.HttpRequestHandler;

/**
 * Created by jiancai.wang on 2016/11/22.
 */
public class WebSocketInitializer extends ChannelInitializer<Channel> {
    private final Logger log = LoggerFactory.getLogger(WebSocketInitializer.class);
    //
    private final ChannelGroup boosGroup;
    //
    private final String serverUrl;
    // to organize data
    private Purchaser purchaser;

    public WebSocketInitializer(String serverUrl, ChannelGroup boosGroup, Purchaser purchaser) {
        this.serverUrl = serverUrl;
        this.boosGroup = boosGroup;
        this.purchaser = purchaser;
    }

    /**
     * GET /chat HTTP/1.1
     * Host: server.example.com
     * Upgrade: websocket   // 支出HTTP请求为websocket连接请求
     * Connection: Upgrade
     * Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==
     * Sec-WebSocket-Protocol: chat, superchat
     * Sec-WebSocket-Version: 13
     * Origin: http://example.com
     * <p>
     * <p>
     * HTTP/1.1 101 Switching Protocols
     * Upgrade: websocket
     * Connection: Upgrade
     * Sec-WebSocket-Accept: HSmrc0sMlYUkAGmm5OPpG2HaGWk= // websocket秘钥
     * Sec-WebSocket-Protocol: chat
     *
     * @param ch
     * @throws Exception
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {

        // channel event 流动的通道， 可以对其进行挂不同的handler
        ChannelPipeline pipeline = ch.pipeline();
        // 1.
        pipeline.addLast("HttpServerCodec", new HttpServerCodec());
        // 2.
        pipeline.addLast("ChunkedWriteHandler", new ChunkedWriteHandler());
        // 3.
        pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(64 * 1024));
        // 4.
        pipeline.addLast("HttpRequestHandler", new HttpRequestHandler(serverUrl, boosGroup, purchaser));
        // 5.
        pipeline.addLast("WebSocketServerProtocolHandler", new WebSocketServerProtocolHandler(serverUrl));
        // 6.
        pipeline.addLast("WebSocketFrameHandler", new EchoWSHandler(boosGroup));
    }
}
