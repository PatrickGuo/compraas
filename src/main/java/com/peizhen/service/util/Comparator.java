package com.peizhen.service.util;

/**
 * Created by GPZ on 2/6/17.
 */
public abstract class Comparator <T, E> {

    // fields
    double m_threshold;

    public void setThresh(double th) {m_threshold = th;}

    public abstract int findSimilarity(T val);
    public abstract E keyMapper(T key);
}
