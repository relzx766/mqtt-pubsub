package org.relzx.pubsub.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.relzx.pubsub.common.entity.ClientInfo;
@Data
@AllArgsConstructor
public class SessionInvalidateEvent implements Event{
    ClientInfo client;
    Cause cause;

    public SessionInvalidateEvent(ClientInfo client) {
        this.client = client;
        this.cause=Cause.EXPIRE;
    }

    @Override
    public EventType getEventType() {
        return EventType.SESSION_INVALIDATE;
    }

    public enum Cause{
        /**
         * 过期
         */
        EXPIRE,
        /**
         * 显示清除
         */
        INVALIDATE
    }
}
