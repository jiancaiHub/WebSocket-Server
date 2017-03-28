package train.websocket.websocket.handler;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import train.websocket.websocket.init.Purchaser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jiancai.wang on 2016/11/22.
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private Logger log = LoggerFactory.getLogger(HttpRequestHandler.class);
    private final String serverUrl;
    private final ChannelGroup boosGroup;
    private final Purchaser purchaser;

    /**
     * Http 处理类
     *
     * @param serverUrl
     */
    public HttpRequestHandler(String serverUrl, ChannelGroup boosGroup, Purchaser purchaser) {
        super();
        this.serverUrl = serverUrl;
        this.boosGroup = boosGroup;
        this.purchaser = purchaser;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        log.info("HttpRequestHandler handle a request：" + request.uri());
        String uri = request.uri();
        String paramStr = request.uri();
        final Map<String, Object> param = new HashMap<>();
        if (uri.contains("?")) {
            uri = uri.substring(0, request.uri().indexOf("?"));
        }
        if (paramStr.contains("=")) {
            paramStr = paramStr.substring(paramStr.indexOf("?") + 1, paramStr.length());
            String[] params = paramStr.split("&");
            Arrays.stream(params).forEach(str -> {
                str = str.trim();
                String k = str.split("=")[0];
                String v = str.split("=")[1];
                if (v.contains(",")) {
                    List<String> topics = Arrays.stream(v.split(","))
                            .collect(Collectors.toList());
                    param.put(v, topics);
                } else {
                    param.put(k, v);
                }
            });
            if (param.containsKey("role")) {

                switch (param.get("role").toString().toLowerCase().substring(0, 1)) {
                    case "c":
                        ConsumeWSHandler consumeWSHandler = new ConsumeWSHandler(boosGroup, purchaser, param);
                        consumeWSHandler.channelActive(ctx);
                        ctx.pipeline().replace("WebSocketFrameHandler", "WebSocketFrameHandler", consumeWSHandler);
                        break;
                    case "p":
                        ProduceWSHandler produceWSHandler = new ProduceWSHandler(boosGroup, purchaser, param);
                        produceWSHandler.channelActive(ctx);
                        ctx.pipeline().replace("WebSocketFrameHandler", "WebSocketFrameHandler", produceWSHandler);
                        break;
                    case "m":
                        MaintenanceWSHandler maintenanceWSHandler = new MaintenanceWSHandler(boosGroup, purchaser);
                        maintenanceWSHandler.channelActive(ctx);
                        ctx.pipeline().replace("WebSocketFrameHandler", "WebSocketFrameHandler", maintenanceWSHandler);
                        break;
                }
                ctx.channel().writeAndFlush(new TextWebSocketFrame("Client " + ctx.toString() + " joined!"));
                boosGroup.add(ctx.channel());
            }
        }
        if (serverUrl.equalsIgnoreCase(uri)) {
            // only pass this request to websocketFrameHandler
            request.setUri(uri);
            ctx.fireChannelRead(request.retain());
        } else {
            log.error("HttpRequestHandler is error: Can't option can't deal this request " + request.uri());
            //
            if (HttpHeaders.is100ContinueExpected(request)) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
                ctx.writeAndFlush(response);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.close();
        cause.printStackTrace(System.err);
    }
}
