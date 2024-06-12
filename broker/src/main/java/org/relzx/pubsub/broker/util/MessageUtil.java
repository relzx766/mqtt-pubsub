package org.relzx.pubsub.broker.util;

public class MessageUtil {
    private final static String SEPARATOR = ":";


    public static String generateId(String clientId, int messageId) {
        return clientId + SEPARATOR + messageId;
    }
    public static String getClientId(String id){
        return id.split(SEPARATOR)[0];
    }
}
