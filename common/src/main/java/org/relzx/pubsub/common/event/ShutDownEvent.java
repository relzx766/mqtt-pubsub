package org.relzx.pubsub.common.event;

public class ShutDownEvent implements Event {
    @Override
    public EventType getEventType() {
        return EventType.SHUTDOWN;
    }
}
