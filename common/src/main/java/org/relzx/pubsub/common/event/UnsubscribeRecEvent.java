package org.relzx.pubsub.common.event;

import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;

@AllArgsConstructor
@Data
public class UnsubscribeRecEvent implements Event{
    ClientInfo client;
    MqttUnsubscribeMessage message;

    @Override
    public EventType getEventType() {
        return EventType.UNSUBSCRIBE_REC;
    }
}
