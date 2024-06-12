package org.relzx.pubsub.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;

@Data
@AllArgsConstructor
public class PubRelRecEvent implements Event{
    ClientInfo client;
    int messageId;

    @Override
    public EventType getEventType() {
        return EventType.PUB_REL_REC;
    }
}
