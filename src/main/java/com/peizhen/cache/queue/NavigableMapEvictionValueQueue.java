package com.peizhen.cache.queue;


import com.peizhen.cache.EvictionValueQueue;
import com.peizhen.cache.map.EvictibleValueEntry;

import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by GPZ on 2/6/17.
 */
public class NavigableMapEvictionValueQueue<K, V> implements EvictionValueQueue<K, V> {
    private final ConcurrentNavigableMap<Long, EvictibleValueEntry<K, V>> map;

    public NavigableMapEvictionValueQueue() {
        this(new ConcurrentSkipListMap<Long, EvictibleValueEntry<K, V>>());
    }

    public NavigableMapEvictionValueQueue(ConcurrentNavigableMap<Long, EvictibleValueEntry<K, V>> map) {
        if (map == null) {
            throw new NullPointerException("Map instance cannot be null");
        }

        this.map = map;
    }

    @Override
    public boolean hasEntries() {
        return !map.isEmpty();
    }

    // MODIFIED: deleted getNextEvictionTime()

    @Override
    public void putEntry(EvictibleValueEntry<K, V> e) {
        map.put(e.importance, e);
    }

    @Override
    public void removeEntry(EvictibleValueEntry<K, V> e) {
        map.remove(e.importance, e);
    }


    // only evict the least k important entries
    @Override
    public int evictEntries(int k) {
        int result = 0;
        for (int i = 0; i < k; i++) {
            if (!map.isEmpty()) {
                Map.Entry<Long, EvictibleValueEntry<K, V>> head = map.firstEntry();
                map.remove(head.getKey());
                head.getValue().evict(false); // not cancel eviction
                result += 1;
            } else {
                break;
            }
        }
        return result;
    }

    @Override
    public void evictAllExpiredEntries() {
        for (Iterator<Map.Entry<Long, EvictibleValueEntry<K, V>>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, EvictibleValueEntry<K, V>> entry = it.next();
            if(entry.getValue().shouldEvict()) {
                entry.getValue().evict(false);
                it.remove();
            }
        }
    }
}
