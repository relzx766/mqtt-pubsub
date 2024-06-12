package org.relzx.pubsub.broker.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.Getter;
import org.relzx.pubsub.common.event.EventType;
import org.relzx.pubsub.common.event.PubRelEvent;
import org.relzx.pubsub.common.listener.Listener;
import org.relzx.pubsub.common.util.EventBusUtil;

public class PubCompService {
    @Getter
    private final static PubCompService instance=new PubCompService();
    private Cache<String,Integer>pendingPubcompCache;
    private PubCompService(){
        EventBusUtil.registerAsync(new Listener<PubRelEvent>(EventType.PUB_REL) {
        });

    }

}
