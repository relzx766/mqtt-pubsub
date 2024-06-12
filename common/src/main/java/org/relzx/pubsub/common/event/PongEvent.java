package org.relzx.pubsub.common.event;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PongEvent implements Event{
    Channel channel;

    @Override
    public EventType getEventType() {
        return EventType.PONG;
    }
}
