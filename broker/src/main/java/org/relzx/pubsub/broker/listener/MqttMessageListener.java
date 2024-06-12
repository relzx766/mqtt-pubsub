package org.relzx.pubsub.broker.listener;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;

@AllArgsConstructor
@Data
public abstract class MqttMessageListener {
    private final MqttTopicSubscription subscription;
    private final ClientInfo clientInfo;

    public void onMessage(MqttMessage message) {
    }
}
