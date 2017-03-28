package train.websocket.main;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;


import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import train.websocket.websocket.server.WebSocketServer;

/**
 * websocket server（kafka到浏览器之间的数据通道）
 *
 * 场景：
 * 1. 由于websocket是浏览器到后台之间的数据传输通道，用户需要在请求中指定传输的数据的内容，
 * 由于websocket是一个脱离后台服务的程序，需要在启动是提供足够的参数来说明用户需要的数据内容是什么（发送频率）。
 * 2. 数据通过建立的websocket通道推送给浏览器，但数据的来源以及数据的初加工（目前：过滤）所需的参数包括：
 * * SERVER_NAME   名称
 * * SERVER_URL    server路径
 * * SERVER_PORT   server端口
 * 3. 启动socket server
 * <p>
 * <p>
 * upgrade:
 * webscoket 不再糅合任何的业务逻辑，只是简单的工具使用（类似数据通道）。
 * 1. 后台会建立连接后一直发送进过处理后的数据。所以有一条通路数数据传入通路
 * 2. 浏览器建立连接是请求数据，浏览器的ws请求会携带相关的请求参数，需要把请求参数缓存下来
 * 原理：websocket 就是个接线员。
 * <p>
 * <p>
 * 生产消费模式：
 * 1. 一个生产者对应多个消费者模式（作为生产者会将后台发送数据给ws根据需求发送到消费队列中。
 * 消费者监听消费队列。 等到所有的消费者消费完后删除消费队列的数据。）
 * <p>
 * Created by jiancai.wang on 2016/11/22.
 */
public class StartWebSocket {
    private static final Logger log = LoggerFactory.getLogger(StartWebSocket.class);
    // 缺省启动参数 用于测试
    private final static JSONObject DEFAULT_PARAMS = new JSONObject();

    static {
        DEFAULT_PARAMS.put("wsServerCode", "WebSocket");
        DEFAULT_PARAMS.put("wsHttpServerHost", "localhost");
        DEFAULT_PARAMS.put("wsHttpServerPort", 5069);
        DEFAULT_PARAMS.put("wsHttpServerUrl", "/iov/websocket/monitor");
        DEFAULT_PARAMS.put("consoleHttpServerHost", "localhost");
        DEFAULT_PARAMS.put("consoleHttpServerPort", 5068);
    }

    /**
     * 启动入口
     */
    public static void main(String[] args) {
        try {
            JSONObject params = args.length > 0 ? JSON.parseObject(args[0]) : DEFAULT_PARAMS;
            log.info("StartWebSocket is getting param: " + params.toJSONString());
            // init socket server
            final WebSocketServer webSocketServer = new WebSocketServer(
                    params.getString("wsServerCode"),
                    params.getString("wsHttpServerHost"),
                    params.getInteger("wsHttpServerPort"),
                    params.getString("wsHttpServerUrl"),
                    params.getString("consoleHttpServerHost"),
                    params.getInteger("consoleHttpServerPort"));
            ChannelFuture f = webSocketServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    webSocketServer.destroy();
                }
            });
            f.channel().closeFuture().syncUninterruptibly();
        } catch (Exception e) {
            log.error("StartWebSocket is error: " + e);
        }
    }
}
