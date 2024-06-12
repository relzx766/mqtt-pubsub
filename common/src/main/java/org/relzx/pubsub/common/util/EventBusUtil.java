package org.relzx.pubsub.common.util;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EventBusUtil {
    private static final Executor executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final EventBus eventBus = new EventBus("sync-eventBus");
    private static final AsyncEventBus asyncEventBus = new AsyncEventBus("async-eventBus", executor);

    public static void register(Object o) {
        eventBus.register(o);
    }
    public static void registerAsync(Object o) {
        asyncEventBus.register(o);
    }
    public static void unregister(Object o) {
        eventBus.unregister(o);
    }
    public static void unregisterAsync(Object o) {
        asyncEventBus.unregister(o);
    }
    public static void post(Object o){
        eventBus.post(o);
    }
    public static void postAsync(Object o){
        asyncEventBus.post(o);
    }
}
