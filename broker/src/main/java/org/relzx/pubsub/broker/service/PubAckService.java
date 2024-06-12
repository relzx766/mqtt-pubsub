package org.relzx.pubsub.broker.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.relzx.pubsub.broker.properties.BrokerProperties;
import org.relzx.pubsub.broker.properties.factory.BrokerPropertiesFactory;
import org.relzx.pubsub.broker.util.MessageUtil;
import org.relzx.pubsub.common.event.EventType;
import org.relzx.pubsub.common.event.PubAckRecEvent;
import org.relzx.pubsub.common.event.PublishEvent;
import org.relzx.pubsub.common.event.RepublishEvent;
import org.relzx.pubsub.common.listener.Listener;
import org.relzx.pubsub.common.util.EventBusUtil;
import org.relzx.pubsub.common.util.MessageIdUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 处理broker发布的publish请求,等待客户端puback响应
 */@Slf4j
public class PubAckService {
    @Getter
    private static final PubAckService instance = new PubAckService();
    private Cache<String, Integer> pendingAckCache;
    ConnectService connectService = ConnectService.getInstance();
    private final BrokerProperties properties= BrokerPropertiesFactory.getInstance();

    private PubAckService() {
        initCache();
        EventBusUtil.registerAsync(new Listener<PublishEvent>(EventType.PUBLISH) {
            @Override
            public void onEvent(PublishEvent event) {
                onPublishEvent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<PubAckRecEvent>(EventType.PUB_ACK_REC) {
            @Override
            public void onEvent(PubAckRecEvent event) {
                onPubAckRecEvent(event);
            }
        });
    }


    private void onPublishEvent(PublishEvent event) {
        MqttPublishMessage message = event.getMessage();
        MqttQoS qoS = message.fixedHeader().qosLevel();
        if (MqttQoS.AT_LEAST_ONCE.equals(qoS)) {
            addPendingAck(
                    MessageUtil.generateId(event.getClient().getClientIdentifier(), message.variableHeader().packetId()),
                    message.variableHeader().packetId());
        }
    }

    private void onPubAckRecEvent(PubAckRecEvent event) {
        String publisher = event.getClient().getClientIdentifier();
        int messageId = event.getMessageId();
        String id = MessageUtil.generateId(publisher, messageId);
        removePendingAck(id);
        if (MessageIdUtil.isForBroker(messageId)){
            MessageIdUtil.release(messageId);
        }
    }

    private void onProcessAckTimeout(String id, int messageId) {
        String clientId = MessageUtil.getClientId(id);
        EventBusUtil.postAsync(new RepublishEvent(
                connectService.getClientInfo(clientId),
                messageId
        ));
    }

    private void initCache() {
        pendingAckCache = Caffeine.newBuilder()
                .initialCapacity(properties.getPubackPendingCacheInitCapacity())
                .maximumSize(properties.getPubackPendingCacheMaxSize())
                .expireAfterWrite(properties.getProcessTimeout(), TimeUnit.MILLISECONDS)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .removalListener((RemovalListener<String, Integer>) (s, message, removalCause) -> {
                    if (RemovalCause.EXPIRED.equals(removalCause)){
                        //在规定时间内过期则代表未得到puback,需要重发
                        log.info("[{}] 消息 [{}] 未得到puback,需要重发", s, message);
                        onProcessAckTimeout(s, message);
                    }
                     else {
                       log.warn("放弃对消息=>{}:{} 的puback等待,原因:{}", s, message, removalCause);
                    }

                })
                .build();

    }

    /**
     * @param id  必须由客户端id+:+消息id组成
     */
    public void addPendingAck(String id, int messageId) {
        pendingAckCache.put(id, messageId);
    }

    private void removePendingAck(String id) {
        pendingAckCache.invalidate(id);
    }


}
