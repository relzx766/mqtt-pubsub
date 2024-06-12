package org.relzx.pubsub.common.event;

import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;

@Data
@AllArgsConstructor
public class PubCompRecEvent implements Event{
    ClientInfo client;
    int messageId;

    public PubCompRecEvent(int messageId) {
        this.messageId = messageId;
    }

    @Override
    public EventType getEventType() {
        return EventType.PUB_COMP_REC;
    }
}
