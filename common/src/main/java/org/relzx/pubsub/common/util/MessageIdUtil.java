package org.relzx.pubsub.common.util;

import lombok.Data;

public class MessageIdUtil {
    private static final int MAX_ID = 65535;
    private static final int MIN_ID = 1;

    private final static int MIN_CLIENT_USABLE_ID = MIN_ID;
    private final static int MAX_CLIENT_USABLE_ID = 60000;
    private final static int MIN_BROKER_USABLE_ID = MAX_CLIENT_USABLE_ID + 1;
    private final static int MAX_BROKER_USABLE_ID = MAX_ID;

    @Data
    static class ID {
        private int id;
        private boolean using;
    }

    private static final ID[] idPool = new ID[MAX_ID];


    private static int index = 0;
    private static int CLIENT_INDEX = MIN_CLIENT_USABLE_ID - 1;
    private static int BROKER_INDEX = MIN_BROKER_USABLE_ID - 1;

    static {
        for (int i = 0; i < MAX_ID; i++) {
            idPool[i] = new ID();
            idPool[i].id = i + 1;
            idPool[i].using = false;
        }
    }

    public static synchronized int getForClient() {
        if (index == MAX_CLIENT_USABLE_ID - 1) {
            index = MIN_CLIENT_USABLE_ID - 1;
        }
        for (int i = CLIENT_INDEX; i < MAX_CLIENT_USABLE_ID; i++) {
            ID id = idPool[i];
            if (id.using) continue;
            CLIENT_INDEX++;
            id.using = true;
            return id.id;
        }

        return getForClient();
    }

    public static synchronized int getForBroker() {
        if (BROKER_INDEX >= MAX_BROKER_USABLE_ID - 1) {
            BROKER_INDEX = MIN_BROKER_USABLE_ID - 1;
        }
        for (int i = BROKER_INDEX; i < MAX_BROKER_USABLE_ID; i++) {
            ID id = idPool[i];
            if (id.using) continue;
            id.using = true;
            BROKER_INDEX++;
            return id.id;
        }
        return getForBroker();
    }

    public static boolean isForClient(int id) {
        return MIN_CLIENT_USABLE_ID <= id && MAX_CLIENT_USABLE_ID >= id;
    }

    public static boolean isForBroker(int id) {
        return MIN_BROKER_USABLE_ID <= id && MAX_BROKER_USABLE_ID >= id;
    }

    public static void release(int id) {
        if (id>=MIN_ID&&id<=MAX_ID){
            idPool[id - 1].using = false;
        }
    }
}
