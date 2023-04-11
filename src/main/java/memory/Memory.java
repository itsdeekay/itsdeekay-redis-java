package memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ValueObject {
    private String value;
    private long pxMilliSeconds;
    private long createdAt;

    ValueObject(String value) {
        this.value = value;
        this.pxMilliSeconds = 86400000000L;
        this.createdAt = System.currentTimeMillis();
    }

    ValueObject(String value, long pxMilliSeconds) {
        this.value = value;
        this.pxMilliSeconds = pxMilliSeconds;
        this.createdAt = System.currentTimeMillis();
    }

    public String getValue(String key) {
        return this.value;
    }

    public long getPxMilliSeconds(String key) {
        return this.pxMilliSeconds;
    }

    public long getCreatedAt(String key) {
        return this.createdAt;
    }
}

public class Memory {
    private static final ConcurrentHashMap<String, ValueObject> memoryStore = new ConcurrentHashMap<String, ValueObject>();

    public static void set(String key, String value, long pxMilliSeconds) {
        memoryStore.put(key, new ValueObject(value, pxMilliSeconds));
    }

    public static String get(String key) {
        if (!memoryStore.containsKey(key))
            return null;
        if (memoryStore.get(key).getCreatedAt(key) + memoryStore.get(key).getPxMilliSeconds(key) < System
                .currentTimeMillis()) {
            memoryStore.remove(key);
            return null;
        }
        return memoryStore.get(key).getValue(key);
    }

    public static void expireKeys(){
        for(Map.Entry<String,ValueObject> entry : memoryStore.entrySet()){
            String key = entry.getKey();
            if (memoryStore.get(key).getCreatedAt(key) + memoryStore.get(key).getPxMilliSeconds(key) < System
                .currentTimeMillis()) {
            memoryStore.remove(key);
        }
        }
    }
}
