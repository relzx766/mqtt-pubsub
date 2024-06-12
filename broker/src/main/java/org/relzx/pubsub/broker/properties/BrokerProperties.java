package org.relzx.pubsub.broker.properties;

import lombok.Data;

@Data
public class BrokerProperties {

    private String host;
    private int port;
    private String password;
    /**
     * mqtt协议的版本,目前服务端是基于mqtt3.1.1设计,因此该属性只能为4
     */
    private int mqttVersion;
    /**
     * session的过期时间,broker使用心跳来刷新session,因此,客户端必须在低于该值的时间内发送心跳,单位为秒
     */
    private int sessionTimeout;
    /**
     * session缓存的初始化数量,这个属性可以认为是客户端的所有量
     */
    private int sessionCacheCapacity;
    /**
     * 单个请求响应的超时时间,比分说publish后等待puback响应,单位为毫秒
     */
    private long processTimeout;
    /*
    以下为消息的缓存配置,对于qos=1、qos=2的消息,需要先缓存起来,目前有两种可能会导致缓存过期
    1:到达过期时间
    2:到达容量限制
    对于情况1,可以认为消息达到了最大处理时间
    对于情况2,被淘汰的消息有可能仍处于其它在处理状态,将会导致有关操作失败,因此只能根据实际情况设置合理的缓存容量
     */
    /**
     * 消息的最大缓存时间,单位为ms,
     */
    private long messageCacheTime;
    /**
     * 消息缓存的最大容量
     */
    private int messageCacheMaxSize;
    /**
     * 消息缓存的初始化容量
     */
    private int messageCacheInitCapacity;
    /*
    以下为待接收puback响应缓存的配置.因为该项属于操作缓存,因此缓存时间为{processTimeout}
     */
    /**
     * 最大容量
     */
    private int pubackPendingCacheMaxSize;
    /**
     * 初始化容量
     */
    private int pubackPendingCacheInitCapacity;
    /*
    其它的缓存也可以参照配置,使用缓存的都在service包内
     */
}
