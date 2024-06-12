package org.relzx.pubsub.broker.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Getter;
import org.relzx.pubsub.broker.properties.BrokerProperties;
import org.relzx.pubsub.broker.properties.factory.BrokerPropertiesFactory;
import org.relzx.pubsub.broker.server.MqttBroker;
import org.relzx.pubsub.broker.util.MessageUtil;
import org.relzx.pubsub.common.entity.ClientInfo;
import org.relzx.pubsub.common.event.EventType;
import org.relzx.pubsub.common.event.PubRecRecEvent;
import org.relzx.pubsub.common.event.PublishEvent;
import org.relzx.pubsub.common.event.RepublishEvent;
import org.relzx.pubsub.common.listener.Listener;
import org.relzx.pubsub.common.util.EventBusUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 处理broker发布的pubrel请求,处理客户端的pubrec响应
 */
public class PubRelService {
    @Getter
    private final static PubRelService instance = new PubRelService();
    private Cache<String, Integer> pendingRecCache;
    private final BrokerProperties properties = BrokerPropertiesFactory.getInstance();
    private final MqttBroker broker = MqttBroker.getInstance();
    ConnectService connectService = ConnectService.getInstance();

    private PubRelService() {
        initCache();
        EventBusUtil.registerAsync(new Listener<PubRecRecEvent>(EventType.PUB_REC_REC) {
            @Override
            public void onEvent(PubRecRecEvent event) {
                onPubRecRec(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<PublishEvent>(EventType.PUBLISH) {
            @Override
            public void onEvent(PublishEvent event) {
                onPublish(event);
            }
        });

    }

    private void initCache() {
        pendingRecCache = Caffeine.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .expireAfterWrite(properties.getProcessTimeout(), TimeUnit.MILLISECONDS)
                .maximumSize(1000)
                .initialCapacity(500)
                .removalListener((RemovalListener<String,Integer>) (key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        onProcessTimeout(key, value);
                    }
                })
                .build();
    }

    private void onPubRecRec(PubRecRecEvent event) {
        ClientInfo client = event.getClient();
        int messageId = event.getMessageId();
        String id = MessageUtil.generateId(client.getClientIdentifier(), messageId);
        pendingRecCache.invalidate(id);
        broker.pubrel(event.getMessageId(), client.getChannel());
    }

    private void onPublish(PublishEvent event) {
        MqttPublishMessage message = event.getMessage();
        int packetId = message.variableHeader().packetId();
        ClientInfo client = event.getClient();
        if (MqttQoS.EXACTLY_ONCE.equals(message.fixedHeader().qosLevel())) {
            String id = MessageUtil.generateId(client.getClientIdentifier(), packetId);
            pendingRecCache.put(id, packetId);
        }
    }



    private void onProcessTimeout(String id,Integer messageId) {
        String clientId = MessageUtil.getClientId(id);
        ClientInfo clientInfo = connectService.getClientInfo(clientId);
        EventBusUtil.postAsync(new RepublishEvent(clientInfo,messageId));
    }
}
