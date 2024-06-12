package org.relzx.pubsub.common.event;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;

@AllArgsConstructor
@Data
public class PublishEvent implements Event{
    ClientInfo client;
    MqttPublishMessage message;

    @Override
    public EventType getEventType() {
        return EventType.PUBLISH;
    }
}
