package org.relzx.pubsub.common.server;

public interface Server {
    void start();
    void shutdown();
    default void registerShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown)); // 注册一个关闭钩子
    }
}
