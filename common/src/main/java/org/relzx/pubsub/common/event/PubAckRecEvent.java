package org.relzx.pubsub.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;

@Data
@AllArgsConstructor
public class PubAckRecEvent implements Event{
    ClientInfo client;
    int messageId;

    @Override
    public EventType getEventType() {
        return EventType.PUB_ACK_REC;
    }
}
