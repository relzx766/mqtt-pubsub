package org.relzx.pubsub.common.listener;

import com.google.common.eventbus.Subscribe;
import org.relzx.pubsub.common.event.Event;
import org.relzx.pubsub.common.event.EventType;

public abstract class Listener<T extends Event> {
    EventType eventType;

    public Listener(EventType eventType) {
        this.eventType = eventType;
    }


    public void onEvent(T event) {
    }


    @Subscribe
    public final void onP(T event) {
        if (eventType.equals(event.getEventType())) {
            onEvent(event);
        }
    }
}
