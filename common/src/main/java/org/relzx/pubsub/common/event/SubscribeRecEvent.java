package org.relzx.pubsub.common.event;

import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;

@Data
@AllArgsConstructor
public class SubscribeRecEvent implements Event{
    ClientInfo client;
    MqttSubscribeMessage message;

    @Override
    public EventType getEventType() {
        return EventType.SUBSCRIBE_REC;
    }
}
