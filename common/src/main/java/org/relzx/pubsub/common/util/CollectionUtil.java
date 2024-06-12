package org.relzx.pubsub.common.util;

import java.util.Collection;

public class CollectionUtil {
    public static boolean isEmpty(Collection<?> collection){
        return collection==null|| collection.isEmpty();
    }
    public static boolean isNotEmpty(Collection<?> collection){
        return !isEmpty(collection);
    }

    public static boolean isEmpty(byte[] bytes) {
        return  bytes == null || bytes.length == 0;
    }
    public static boolean isNotEmpty(byte[]bytes){
        return !isEmpty(bytes);
    }

}
