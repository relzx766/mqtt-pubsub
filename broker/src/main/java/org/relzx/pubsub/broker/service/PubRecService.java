package org.relzx.pubsub.broker.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.Getter;
import org.relzx.pubsub.broker.properties.BrokerProperties;
import org.relzx.pubsub.broker.properties.factory.BrokerPropertiesFactory;
import org.relzx.pubsub.broker.server.MqttBroker;
import org.relzx.pubsub.broker.util.MessageUtil;
import org.relzx.pubsub.common.entity.ClientInfo;
import org.relzx.pubsub.common.event.EventType;
import org.relzx.pubsub.common.event.PubRecEvent;
import org.relzx.pubsub.common.event.PubRelEvent;
import org.relzx.pubsub.common.event.PubRelRecEvent;
import org.relzx.pubsub.common.listener.Listener;
import org.relzx.pubsub.common.util.EventBusUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 存储broker发布的pubrec请求,等待客户端pubrel响应
 */
public class PubRecService {
    private Cache<String, Integer> pendingRelCache;
    @Getter
    private static final PubRecService instance = new PubRecService();

    private final MqttBroker broker = MqttBroker.getInstance();
    private final ConnectService connectService = ConnectService.getInstance();
    private final BrokerProperties properties= BrokerPropertiesFactory.getInstance();

    private PubRecService() {
        initCache();
        EventBusUtil.registerAsync(new Listener<PubRecEvent>(EventType.PUB_REC) {
            @Override
            public void onEvent(PubRecEvent event) {
                onPubRecEvent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<PubRelRecEvent>(EventType.PUB_REL_REC) {
            @Override
            public void onEvent(PubRelRecEvent event) {
                onPubRelRecEvent(event);
            }
        });
    }

    private void initCache() {
        pendingRelCache = Caffeine.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .maximumSize(1000)
                .initialCapacity(500)
                .expireAfterWrite(properties.getProcessTimeout(),TimeUnit.MILLISECONDS)
                .removalListener((RemovalListener<String, Integer>) (key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        onPendingPubRelTimeout(key, value);
                    }
                })
                .build();
    }

    private void onPubRecEvent(PubRecEvent event) {
        ClientInfo client = event.getClient();
        int messageId = event.getMessageId();
        pendingRelCache.put(
                MessageUtil.generateId(client.getClientIdentifier(), messageId),
                messageId
        );
    }

    private void onPubRelRecEvent(PubRelRecEvent event) {
        ClientInfo client = event.getClient();
        int messageId = event.getMessageId();
        String id = MessageUtil.generateId(client.getClientIdentifier(), messageId);
        Integer msgId = pendingRelCache.getIfPresent(id);
        pendingRelCache.invalidate(id);
        EventBusUtil.postAsync(new PubRelEvent(
                client,msgId

        ));
    }

    private void onPendingPubRelTimeout(String id, Integer messageId) {
        String clientId = MessageUtil.getClientId(id);
        ClientInfo client = connectService.getClientInfo(clientId);
        broker.pubrec(messageId, client.getChannel());
    }
}
