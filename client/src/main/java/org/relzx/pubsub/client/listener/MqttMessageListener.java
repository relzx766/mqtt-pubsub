package org.relzx.pubsub.client.listener;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class MqttMessageListener {
    private final MqttTopicSubscription subscription;
    public void onMessage(MqttMessage message){}
}
