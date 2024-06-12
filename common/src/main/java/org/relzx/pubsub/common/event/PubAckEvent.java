package org.relzx.pubsub.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PubAckEvent implements Event{
    /**
     * 为客户端或服务端的唯一标识
     */
    String id;
    int messageId;

    @Override
    public EventType getEventType() {
        return EventType.PUB_ACK;
    }
}
