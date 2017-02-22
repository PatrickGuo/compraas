package com.peizhen.cache.manipulator;

import com.peizhen.cache.EntryManipulator;

import java.util.Comparator;
import java.util.List;

/**
 * Created by GPZ on 2/14/17.
 */
public class VectorEntryManipulator implements EntryManipulator<List<Double>, double[], String> {

    public Comparator<double[]> comparator;

    public VectorEntryManipulator() {
        comparator = new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                double val1 = 0, val2 = 0;
                for (int i = 0; i < o1.length; i++) {
                    val1 += o1[i] * o1[i];
                    val2 += o2[i] * o2[i];
                }
                if (val1 < val2) {
                    return -1;
                } else if (val1 > val2) {
                    return 1;
                } else {
                    return 0;
                }
            }

            /** This is lexical ordered comparison for vector
            public int compare(double[] o1, double[] o2) {
                double diff = 0;
                for (int i = 0; i < o1.length; i++) {
                    if (o1[i] > o2[i]) {
                        return 1;
                    } else if (o2[i] > o1[i]) {
                        return -1;
                    }
                }
                return 0;
            }
            */
        };

    }


    @Override
    public Comparator<double[]> getComparator() {
        return this.comparator;
    }

    @Override
    public double[] mapKey(List<Double> key) {
        double[] vec = key.stream().mapToDouble((Double i) -> i.doubleValue()).toArray();
        double len = 0;
        for (int i = 0; i < vec.length; i++) {
            len += vec[i]*vec[i];
        }
        len = Math.sqrt(len);
        for (int i = 0; i < vec.length; i++) {
            vec[i] /= len;
        }
        return vec;
    }

    @Override
    public double keySimilarity(double[] key1, double[] key2) {
        double[] mk1 = key1;
        double[] mk2 = key2;
        double sim = 0;
        for (int i =0 ; i < mk1.length; i++) {
            sim += Math.sqrt(Math.abs(mk1[i] - mk2[i]) * Math.abs(mk1[i] - mk2[i]));
        }
        return sim;
    }

    @Override
    public boolean isSameResult(String val1, String val2) {
        return val1.equals(val2);
    }

    @Override
    public boolean isSimilarKey(double[] key1, double[] key2, double threshold) {
        return (keySimilarity(key1, key2) < threshold);
    }
}
