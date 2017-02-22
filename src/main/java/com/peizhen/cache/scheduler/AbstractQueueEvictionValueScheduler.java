package com.peizhen.cache.scheduler;


import com.peizhen.cache.EvictionValueQueue;
import com.peizhen.cache.EvictionValueScheduler;
import com.peizhen.cache.map.EvictibleValueEntry;
import com.peizhen.cache.queue.NavigableMapEvictionValueQueue;

/**
 * An abstract {@link EvictionValueScheduler} which uses an {@link EvictionValueQueue} to
 * store {@link EvictibleValueEntry} instances in the order they should be evicted.
 * This class does not implement the actual eviction functionality, it should be
 * implemented by its subclasses.
 *
 * @author Stoyan Rachev
 *
 * @param <K>
 *            the type of keys maintained by this map
 *
 * @param <V>
 *            the type of mapped values
 */
public abstract class AbstractQueueEvictionValueScheduler<K, V> implements EvictionValueScheduler<K, V> {

    private final EvictionValueQueue<K, V> queue;

    /**
     * Creates an eviction scheduler with a {@link NavigableMapEvictionValueQueue}.
     */
    public AbstractQueueEvictionValueScheduler() {
        this(new NavigableMapEvictionValueQueue<K, V>());
    }

    /**
     * Creates an eviction scheduler with the specified queue.
     *
     * @param queue
     *            the queue to be used
     *
     * @throws NullPointerException
     *             if the queue is <code>null</code>
     */
    public AbstractQueueEvictionValueScheduler(EvictionValueQueue<K, V> queue) {
        super();
        if (queue == null) {
            throw new NullPointerException("Queue cannot be null");
        }

        this.queue = queue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scheduleEviction(EvictibleValueEntry<K, V> e) {
        if (e.isEvictible()) {
            queue.putEntry(e);
            onScheduleEviction(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelEviction(EvictibleValueEntry<K, V> e) {
        if (e.isEvictible()) {
            queue.removeEntry(e);
            onCancelEviction(e);
        }
    }


    // evict one for default
    protected void evictEntries() {
        if (queue.evictEntries(1) == 1) {
            onEvictEntries();
        }
    }

    // evict one for default
    protected void evictEntries(int num) {
        if (queue.evictEntries(num) == num) {
            onEvictEntries();
        } else {
            onPartialEvictEntries(num);
        }
    }

    protected void evictAllExpiredEntries() {
        queue.evictAllExpiredEntries();
    }

    /**
     * Returns <code>true</code> if there are any evictions scheduled. This
     * simply calls the <tt>hasEntries</tt> method on the queue.
     */
    protected boolean hasScheduledEvictions() {
        return queue.hasEntries();
    }


    /**
     * Actually schedules the eviction of the specified entry. It is guaranteed
     * that this entry is evictible. Subclasses should override this method to
     * provide the actual scheduling functionality.
     *
     * @param e
     *            the entry for which the eviction should be scheduled
     */
    protected abstract void onScheduleEviction(EvictibleValueEntry<K, V> e);

    /**
     * Actually cancels the eviction of the specified entry. It is guaranteed
     * that this entry is evictible. Subclasses should override this method to
     * provide the actual cancellation functionality.
     *
     * @param e
     *            the entry for which the eviction should be cancelled
     */
    protected abstract void onCancelEviction(EvictibleValueEntry<K, V> e);

    /**
     * Performs additional activities upon automated eviction of entries, if
     * needed. Subclasses should override this method if they need to perform
     * such activities.
     */
    protected abstract void onEvictEntries();

    protected abstract void onPartialEvictEntries(int num);


    final class EvictionRunnable implements Runnable {
        @Override
        public void run() {
            evictEntries();
        }
    }
}

