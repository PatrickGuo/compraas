package com.peizhen.cache.map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Map.Entry;

/**
 * Created by GPZ on 2/6/17.
 */
public class EvictibleValueEntry<K, V> implements Entry<K, V> {
    private final ConcurrentMapWithValuedEvictionDecorator<K, V> map;

    private final K key;

    private volatile V value;


    public final long importance;
    public final long weight;
    public final long freq;

    public final boolean evictible;

    public final long evictionTime;
    public final long evictMs;


    EvictibleValueEntry(ConcurrentMapWithValuedEvictionDecorator<K, V> map, K key, V value,
                        long weight, long evictMs) {
        if (value == null) {
            throw new NullPointerException("Value cannot be null");
        }

        if (evictMs < 0) {
            throw new IllegalArgumentException("Eviction time cannot be less than zero");
        }

        this.map = map;
        this.key = key;
        this.value = value;
        this.freq = 1;
        this.weight = weight;
        this.importance = weight * this.freq;
        this.evictMs = evictMs;
        this.evictible = (evictMs > 0);
        this.evictionTime = (evictible) ? System.nanoTime() + NANOSECONDS.convert(evictMs, MILLISECONDS) : 0;
    }

    EvictibleValueEntry(ConcurrentMapWithValuedEvictionDecorator<K, V> map, K key, V value,
                        long weight, long freq, long evictMs) {
        if (value == null) {
            throw new NullPointerException("Value cannot be null");
        }

        if (evictMs < 0) {
            throw new IllegalArgumentException("Eviction time cannot be less than zero");
        }

        this.map = map;
        this.key = key;
        this.value = value;
        this.freq = freq;
        this.weight = weight;
        this.importance = weight * freq;
        this.evictMs = evictMs;
        this.evictible = (evictMs > 0);
        this.evictionTime = (evictible) ? System.nanoTime() + NANOSECONDS.convert(evictMs, MILLISECONDS) : 0;
    }

    @Override
    public K getKey() {
        return this.key;
    }

    @Override
    public V getValue() {
        return this.value;
    }

    @Override
    public synchronized V setValue(V value) {
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }

        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    public boolean isEvictible() {
        return this.evictible;
    }

    public long getEvictionTime() {
        return this.evictionTime;
    }

    public boolean shouldEvict() {
        return (this.evictible) && (System.nanoTime() > this.evictionTime);
    }

    public void evict(boolean cancelPendingEviction) {
        this.map.evict(this, cancelPendingEviction);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s, %d, %d, %d]", (key != null) ? key : "null", value, evictMs, weight, freq);
    }


}
