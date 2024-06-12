package org.relzx.pubsub.broker.service;

import io.netty.handler.codec.mqtt.*;
import lombok.Getter;
import org.relzx.pubsub.broker.listener.MqttMessageListener;
import org.relzx.pubsub.broker.server.MqttBroker;
import org.relzx.pubsub.common.entity.ClientInfo;
import org.relzx.pubsub.common.event.EventType;
import org.relzx.pubsub.common.event.SessionInvalidateEvent;
import org.relzx.pubsub.common.event.SubscribeRecEvent;
import org.relzx.pubsub.common.event.UnsubscribeRecEvent;
import org.relzx.pubsub.common.listener.Listener;
import org.relzx.pubsub.common.util.EventBusUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理订阅信息
 */
public class SubscribeService {
    @Getter
    private static final SubscribeService instance = new SubscribeService();
    private final MqttBroker broker = MqttBroker.getInstance();
    private final ConnectService connectService = ConnectService.getInstance();
    private final PublishService publishService=PublishService.getInstance();
    private final Map<String, MqttMessageListener> listenerMap = new HashMap<>();

    private SubscribeService() {
        EventBusUtil.registerAsync(new Listener<SubscribeRecEvent>(EventType.SUBSCRIBE_REC) {
            @Override
            public void onEvent(SubscribeRecEvent event) {
                onSubscribeRecEvent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<UnsubscribeRecEvent>(EventType.UNSUBSCRIBE_REC) {
            @Override
            public void onEvent(UnsubscribeRecEvent event) {
                onUnsubscribeRecEvent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<SessionInvalidateEvent>(EventType.SESSION_INVALIDATE) {
            @Override
            public void onEvent(SessionInvalidateEvent event) {
                onSessionTimeoutEvent(event);
            }
        });
    }
    private void onSubscribeRecEvent(SubscribeRecEvent event){
        MqttSubscribeMessage message = event.getMessage();
        ClientInfo client = event.getClient();
        for (MqttTopicSubscription subscription : message.payload().topicSubscriptions()) {
            MqttMessageListener listener = new MqttMessageListener(subscription, client) {
                @Override
                public void onMessage(MqttMessage message) {
                    broker.publish((MqttPublishMessage) message, getClientInfo().getChannel());
                }
            };
            publishService.addListener(listener);
            listenerMap.put(client.getClientIdentifier() + ":" + subscription.topicFilter(), listener);
        }
        broker.suback(message,client.getChannel());
    }



    private void onUnsubscribeRecEvent(UnsubscribeRecEvent event) {
        String clientId = event.getClient().getClientIdentifier();
        MqttUnsubscribeMessage message = event.getMessage();
        List<String> topics = message.payload().topics();
        for (String topic : topics) {
            MqttMessageListener listener = listenerMap.remove(clientId + ":" + topic);
            publishService.removeListener(listener);
        }
    }
    private void onSessionTimeoutEvent(SessionInvalidateEvent event){
        ClientInfo client = event.getClient();
        List<String>keys=new ArrayList<>();
        listenerMap.forEach((k,v)->{
            if (v.getClientInfo().equals(client)){
                keys.add(k);
            }
        });
        for (String key : keys) {
            listenerMap.remove(key);
        }
    }
}
