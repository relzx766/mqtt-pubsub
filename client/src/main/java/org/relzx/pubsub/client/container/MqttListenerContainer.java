package org.relzx.pubsub.client.container;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.relzx.pubsub.client.MqttClient;
import org.relzx.pubsub.client.listener.MqttMessageListener;
import org.relzx.pubsub.client.properties.ClientProperties;
import org.relzx.pubsub.client.properties.PropertiesFactory;
import org.relzx.pubsub.common.event.EventType;
import org.relzx.pubsub.common.event.PublishRecEvent;
import org.relzx.pubsub.common.event.ShutDownEvent;
import org.relzx.pubsub.common.event.UnsubscribeEvent;
import org.relzx.pubsub.common.listener.Listener;
import org.relzx.pubsub.common.util.EventBusUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MqttListenerContainer {
    private final Map<String, Set<MqttMessageListener>> listenerMap = new HashMap<>();
    private final MqttClient mqttClient=MqttClient.getInstance();
    private final ClientProperties properties= PropertiesFactory.getProperties();

    public MqttListenerContainer() {
        EventBusUtil.registerAsync(new Listener<PublishRecEvent>(EventType.PUBLISH_REC) {
            @Override
            public void onEvent(PublishRecEvent event) {
                onMessageReceivedEvent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<UnsubscribeEvent>(EventType.UNSUBSCRIBE) {
            @Override
            public void onEvent(UnsubscribeEvent event) {
                onUnsubscribeEvent(event);
            }
        });
        EventBusUtil.registerAsync(new Listener<ShutDownEvent>(EventType.SHUTDOWN) {
            @Override
            public void onEvent(ShutDownEvent event) {
                onShutDownEvent(event);
            }
        });
    }


    private void onUnsubscribeEvent(UnsubscribeEvent event) {
        unsubscribe(event.getTopic());
    }

    private void onMessageReceivedEvent(PublishRecEvent event) {
        MqttPublishMessage message = event.getMessage();
        String topic = message.variableHeader().topicName();
        Set<MqttMessageListener> listenerSet = listenerMap.get(topic);
        if (listenerSet != null) {
            for (MqttMessageListener listener : listenerSet) {
                listener.onMessage(message);
            }
        }
    }

    private void onShutDownEvent(ShutDownEvent event) {
        listenerMap.clear();
    }

    public void addListener(MqttMessageListener listener) {
        MqttTopicSubscription subscription = listener.getSubscription();
        String topic = subscription.topicFilter();
        Set<MqttMessageListener> listenerSet = listenerMap.computeIfAbsent(topic, k -> new HashSet<>());
        listenerSet.add(listener);
        mqttClient.subscribe(subscription.qualityOfService(), properties.getTimeout(),topic);
    }

    public void removeListener(MqttMessageListener listener) {
        String topic = listener.getSubscription().topicFilter();
        Set<MqttMessageListener> listenerSet = listenerMap.get(topic);
        if (listenerSet != null) {
            listenerSet.remove(listener);
        }
        mqttClient.unsubscribe(topic);
    }


    private void unsubscribe(String topic) {
        listenerMap.remove(topic);
    }


}
