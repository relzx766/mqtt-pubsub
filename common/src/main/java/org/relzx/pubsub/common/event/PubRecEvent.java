package org.relzx.pubsub.common.event;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;

@Data
@AllArgsConstructor
public class PubRecEvent implements Event{
    ClientInfo client;
    int messageId;

    @Override
    public EventType getEventType() {
        return EventType.PUB_REC;
    }
}
