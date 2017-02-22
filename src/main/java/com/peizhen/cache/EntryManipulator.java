package com.peizhen.cache;

import java.util.Comparator;

/**
 * Created by GPZ on 2/14/17.
 */
public interface EntryManipulator<K, T, V> {

    public Comparator<T> getComparator();

    public T mapKey(K key);

    public double keySimilarity(T key1, T key2); // result must be a value greater than 0, and smaller than 1

    public boolean isSameResult(V val1, V val2);

    public boolean isSimilarKey(T key1, T key2, double threshold);

}


