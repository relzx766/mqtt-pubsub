package org.relzx.pubsub.common.event;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.relzx.pubsub.common.entity.ClientInfo;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublishRecEvent implements Event{
    ClientInfo client;
    MqttPublishMessage message;

    public PublishRecEvent(MqttPublishMessage message) {
        this.message = message;
    }

    @Override
    public EventType getEventType() {
        return EventType.PUBLISH_REC;
    }
}
