package org.relzx.pubsub.common.event;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnackRecEvent implements Event{
    MqttConnAckMessage message;
    public boolean isSuccess() {
        return message.variableHeader().connectReturnCode().equals(MqttConnectReturnCode.CONNECTION_ACCEPTED);
    }

    @Override
    public EventType getEventType() {
        return EventType.CONNACK_REC;
    }
}
