package org.relzx.pubsub.client.properties;

import lombok.Data;

@Data
public class ClientProperties {
    /**
     * broker host
     */
    private String host;
    /**
     * broker port
     */
    private int port;
    /**
     * 请求超时
     */
    private long timeout;
    private String username;
    private String password;
    /**
     * 连接keepalive时间,具体参考{@link org.relzx.pubsub.client.processor.PingProcessor#call()}
     */
    private int keepAlive;
    /**
     * 重连最大次数,为负数时一直重试
     */
    private int maxReconnectAttempts;
    /**
     * 重连间隔
     */
    private int reconnectInterval;
    /**
     * 客户端标识
     */
    private String clientIdentifier;
    private boolean willFlag;
    private String willTopic;
    private String willMessage;
    private int willQos;
    private boolean willRetain;
}
