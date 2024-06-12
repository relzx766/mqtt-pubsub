package org.relzx.pubsub.common.event;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscribeEvent implements Event {
    MqttTopicSubscription subscription;
    Channel channel;

    public SubscribeEvent(MqttTopicSubscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public EventType getEventType() {
        return EventType.SUBSCRIBE;
    }
}
