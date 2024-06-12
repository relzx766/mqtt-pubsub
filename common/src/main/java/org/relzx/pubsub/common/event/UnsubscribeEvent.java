package org.relzx.pubsub.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnsubscribeEvent implements Event {
    String topic;
    EventSource eventSource;

    @Override
    public EventType getEventType() {
        return EventType.UNSUBSCRIBE;
    }

    public enum EventSource{
        CLIENT,
        BROKER
    }
}
