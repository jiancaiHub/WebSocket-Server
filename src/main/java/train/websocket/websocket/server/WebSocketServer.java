package train.websocket.websocket.server;

import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import train.websocket.websocket.init.Purchaser;
import train.websocket.websocket.init.WebSocketInitializer;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Objects;

/**
 * Created by jiancai.wang on 2016/11/22.
 */
public class WebSocketServer {
    private final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    // A thread-safe {@link Set} that contains open {@link Channel}s and provides
    // various bulk operations on them.
    private final ChannelGroup boosGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    // be used to handle channels of accepted
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    // channel
    private Channel channel;
    // get name from database
    private String serverName;
    // server will option this serverUrl
    private String serverUrl;
    // server will option this serverHost
    private String serverHost;
    // server will monitor this port
    private Integer serverPort;
    // console server host
    private String consoleHttpServerHost;
    // console server port. heartbeat will be send to this port.
    private Integer consoleHttpServerPort;
    // manage message send ang receive
    private Purchaser purchaser;
    // heartbeat
    private HeartbeatServer heartbeatServer;

    public WebSocketServer(String serverName, String serverHost, Integer serverPort, String serverUrl, String consoleHttpServerHost, Integer consoleHttpServerPort) {
        try {
            Objects.isNull(serverName);
            Objects.isNull(serverHost);
            Objects.isNull(serverPort);
            Objects.isNull(serverUrl);
            Objects.isNull(consoleHttpServerHost);
            Objects.isNull(consoleHttpServerPort);
            this.serverName = serverName;
            this.serverHost = serverHost;
            this.serverPort = serverPort;
            this.serverUrl = serverUrl;
            this.consoleHttpServerHost = consoleHttpServerHost;
            this.consoleHttpServerPort = consoleHttpServerPort;
            this.purchaser = new Purchaser();
        } catch (Exception e) {
            log.error("WebSocketServer 初始化失败! " + e);
        }
    }

    /**
     * websocket server： 用于数据的实时推送
     * ** 是一个独立websocket服务程序，参数是由前台启动命令下发的
     */
    public ChannelFuture start() {
        // 服务启动入口
        // web bootstrap
        log.info("WebSocket server " + serverName + " is starting .........");
        ServerBootstrap boot = new ServerBootstrap();
        // 配置线程池 通道组 通道处理器 channelGroup
        // workerGroup 用于请求接收 EventLoopGroup
        boot
                .group(workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(createInitializer(boosGroup));
        // 绑定端口（监听端口）
        ChannelFuture f = boot
                .bind(new InetSocketAddress(serverPort))
                .syncUninterruptibly();
        // i/o 操作的封装类
        channel = f.channel();
        //start heartbeats
        heartbeatServer = new HeartbeatServer(consoleHttpServerHost, consoleHttpServerPort, null);
        JSONObject heartbeat = new JSONObject();
        // inti param
        heartbeat.put("wsServerCode", serverName);
        heartbeat.put("wsHttpServerHost", serverHost);
        heartbeat.put("wsHttpServerPort", serverPort);
        heartbeat.put("wsHttpServerUrl", serverUrl);
        heartbeat.put("timestamp", new Date());
        heartbeatServer.onHeartbeats(heartbeat);
        return f;
    }

    // channel handler 主要处理流程
    public ChannelHandler createInitializer(ChannelGroup channelGroup) {
        return new WebSocketInitializer(serverUrl, channelGroup, purchaser);
    }

    /**
     * socket server close
     */
    public void destroy() {
        if (heartbeatServer.isShutdown())
            heartbeatServer.onClose();
        if (channel != null)
            channel.close();
        boosGroup.close();
        workerGroup.shutdownGracefully();
        purchaser.close();
        log.info("WebSocket server " + serverName + " is stopping .........");
    }

}
