package train.websocket.websocket.server;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by jiancai.wang on 2016/12/12.
 */
public class HeartbeatServer {
    private Logger log = LoggerFactory.getLogger(HeartbeatServer.class);

    // console server host
    private String consoleHttpServerHost;
    // console server port. heartbeat will be send to this port.
    private Integer consoleHttpServerPort;
    // url
    private String heartbeatsUrl;
    // path
    private String heartbeatsPath;
    // request path
    private final String DEFAULT_PATH = "/iov/rest/common/ws/server/heartbeat";
    // heartbeat scheduled
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    //
    private Boolean shutdown = true;

    public HeartbeatServer(String consoleHttpServerHost, Integer consoleHttpServerPort, String heartbeatsPath) {
        this.consoleHttpServerHost = consoleHttpServerHost;
        this.consoleHttpServerPort = consoleHttpServerPort;
        this.heartbeatsPath = heartbeatsPath == null ? DEFAULT_PATH : heartbeatsPath;

    }

    public void onHeartbeats(JSONObject heartbeats) {

        this.heartbeatsUrl = "http://" + consoleHttpServerHost + ":" + consoleHttpServerPort + heartbeatsPath;
        String jsonStr = JSON.toJSONString(heartbeats);
        Client client = ClientBuilder.newClient();

        scheduler.scheduleWithFixedDelay(() -> {

            String response = client.target(heartbeatsUrl)
                    .request()
                    .post(Entity.entity(jsonStr, "application/json"), String.class);
            JSONObject json = JSON.parseObject(response);
            if (json.getString("status").equals("success")) ;
            {
                log.info("[heartbeats] - " + jsonStr);
            }

        }, 0, 30, TimeUnit.SECONDS);

        this.shutdown = false;
    }

    public void onClose() {

        if (scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
