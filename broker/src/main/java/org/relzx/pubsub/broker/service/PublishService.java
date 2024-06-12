package org.relzx.pubsub.broker.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.relzx.pubsub.broker.listener.MqttMessageListener;
import org.relzx.pubsub.broker.properties.BrokerProperties;
import org.relzx.pubsub.broker.properties.factory.BrokerPropertiesFactory;
import org.relzx.pubsub.broker.server.MqttBroker;
import org.relzx.pubsub.broker.util.MessageUtil;
import org.relzx.pubsub.common.entity.ClientInfo;
import org.relzx.pubsub.common.event.*;
import org.relzx.pubsub.common.listener.Listener;
import org.relzx.pubsub.common.util.CollectionUtil;
import org.relzx.pubsub.common.util.EventBusUtil;
import org.relzx.pubsub.common.util.MessageIdUtil;
import org.relzx.pubsub.common.util.MqttMessageUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PublishService {
    @Getter
    private static final PublishService instance = new PublishService();
    private final Map<String, Set<MqttMessageListener>> listenerMap = new HashMap<>();
    private final MqttBroker broker = MqttBroker.getInstance();
    private final ConnectService connectService = ConnectService.getInstance();
    private final Map<String, MqttPublishMessage> retainMessage = new HashMap<>();
    private Cache<String, MqttPublishMessage> messageCache;
    private final BrokerProperties properties= BrokerPropertiesFactory.getInstance();

    private PublishService() {
        initCache();
        EventBusUtil.registerAsync(new Listener<PublishRecEvent>(EventType.PUBLISH_REC) {
            @Override
            public void onEvent(PublishRecEvent event) {
                onPublishRecEvent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<RepublishEvent>(EventType.REPUBLISH) {
            @Override
            public void onEvent(RepublishEvent event) {
                onRepublishEVent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<PubRelEvent>(EventType.PUB_REL) {
            @Override
            public void onEvent(PubRelEvent event) {
                onPubRelEvent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<SessionInvalidateEvent>(EventType.SESSION_INVALIDATE) {
            @Override
            public void onEvent(SessionInvalidateEvent event) {
                onSessionInvalidateEvent(event);
            }
        });
    }

    private void initCache() {
        messageCache = Caffeine.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .maximumSize(properties.getMessageCacheMaxSize())
                .initialCapacity(properties.getMessageCacheInitCapacity())
                .expireAfterWrite(properties.getMessageCacheTime(), TimeUnit.MILLISECONDS)
                .removalListener((RemovalListener<String, MqttPublishMessage>) (key, value, cause) -> {
                    log.warn("消息=>{} 被丢弃,原因:{}",key,cause);
                })
                .build();
    }

    private void onPublishRecEvent(PublishRecEvent event) {
        MqttPublishMessage message = event.getMessage();
        MqttQoS qoS = message.fixedHeader().qosLevel();
        String topic = message.variableHeader().topicName();
        int packetId = message.variableHeader().packetId();
        ClientInfo client = event.getClient();
        if (message.fixedHeader().isRetain()) {
            retainMessage.put(topic, message);
        }
        if (MqttQoS.AT_MOST_ONCE.equals(qoS) || MqttQoS.AT_LEAST_ONCE.equals(qoS)) {
            Set<MqttMessageListener> listenerSet = listenerMap.get(topic);
            if (CollectionUtil.isNotEmpty(listenerSet)) {
                for (MqttMessageListener listener : listenerSet) {
                    listener.onMessage(message);
                }
            }
            if (MqttQoS.AT_LEAST_ONCE.equals(qoS)) {
                broker.puback(packetId, client.getChannel());
            }
        }
        if (MqttQoS.EXACTLY_ONCE.equals(qoS)) {
            broker.pubrec(packetId, client.getChannel());
        }
        if (MqttQoS.EXACTLY_ONCE.equals(qoS) || MqttQoS.AT_LEAST_ONCE.equals(qoS)) {
            messageCache.put(MessageUtil.generateId(client.getClientIdentifier(), packetId), message);
        }
    }

    private void onPubRelEvent(PubRelEvent event) {
        ClientInfo client = event.getClient();
        int messageId = event.getMessageId();
        String id = MessageUtil.generateId(client.getClientIdentifier(), messageId);
        MqttPublishMessage message = messageCache.getIfPresent(id);
        if (message != null) {
            String topic = message.variableHeader().topicName();
            Set<MqttMessageListener> listenerSet = listenerMap.get(topic);
            if (CollectionUtil.isNotEmpty(listenerSet)) {
                for (MqttMessageListener listener : listenerSet) {
                    listener.onMessage(message);
                }
            }
            broker.pubcomp(message.variableHeader().packetId(), client.getChannel());
        }else {
            log.warn("消息丢失，找不到消息=>{}:{},或已超出最大处理期限",client.getClientIdentifier(),messageId);
        }

    }

    private void onRepublishEVent(RepublishEvent event) {
        String subscriber = event.getClient().getClientIdentifier();
        int messageId = event.getMessageId();
        String id = MessageUtil.generateId(subscriber, messageId);
        MqttPublishMessage message = messageCache.getIfPresent(id);
        if (message != null) {
            String topic = message.variableHeader().topicName();
            Set<MqttMessageListener> listenerSet = listenerMap.get(topic);
            if (CollectionUtil.isNotEmpty(listenerSet)) {
                for (MqttMessageListener listener : listenerSet) {
                    ClientInfo clientInfo = listener.getClientInfo();
                    if (clientInfo.getClientIdentifier().equals(subscriber)) {
                        listener.onMessage(MqttMessageUtil.duePublishMessage(message));
                    }
                }
            }
        }else {
            log.warn("消息丢失，找不到消息=>{}:{},或已超出最大处理期限",subscriber,messageId);
        }

    }

    private void onSessionInvalidateEvent(SessionInvalidateEvent event) {
        ClientInfo client = event.getClient();
        if (client.isWillFlag()) {
            int qos = client.getWillQos();
            int messageId=MqttQoS.AT_LEAST_ONCE.value()==qos?-1:MessageIdUtil.getForBroker();
            MqttPublishMessage message = MqttMessageUtil.publishMessage(client.getWillTopic(), client.getWillMessage(), qos, messageId, client.isWillRetain());
            EventBusUtil.postAsync(new PublishRecEvent(client, message));
        }
    }

    public void addListener(MqttMessageListener listener) {
        MqttTopicSubscription subscription = listener.getSubscription();
        String topic = subscription.topicFilter();
        Set<MqttMessageListener> listenerSet = listenerMap.computeIfAbsent(topic, k -> new HashSet<>());
        listenerSet.add(listener);
        onNewListenerRegister(listener);
    }

    public void removeListener(MqttMessageListener listener) {
        String topic = listener.getSubscription().topicFilter();
        Set<MqttMessageListener> listenerSet = listenerMap.get(topic);
        if (listenerSet != null) {
            listenerSet.remove(listener);
        }
    }

    private void onNewListenerRegister(MqttMessageListener listener) {
        MqttTopicSubscription subscription = listener.getSubscription();
        String topic = subscription.topicFilter();
        MqttPublishMessage message = retainMessage.get(topic);
        if (message != null) {
            listener.onMessage(message);
        }
    }
}
