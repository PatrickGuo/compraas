package com.peizhen.service;

import com.peizhen.cache.ConcurrentMapWithTimedEviction;
import com.peizhen.cache.map.ConcurrentHashMapWithTimedEviction;
import com.peizhen.service.util.Comparator;
import com.peizhen.service.util.Statistics;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by GPZ on 2/5/17.
 */
public class ServiceDaemon {

    // store objects sent from client that implemented the comparator method
    HashMap<String, Comparator> cmpMap;
    // store tuned threshold of all functions
    HashMap<String, Double> threshMap;
    // store information
    HashMap<String, Statistics> statMap;
    // cache layer
    ConcurrentMapWithTimedEviction cache;

    public ServiceDaemon() {
        cmpMap = new HashMap<String, Comparator>();
        threshMap = new HashMap<String, Double>();
        statMap = new HashMap<String, Statistics>();
        cache = new ConcurrentHashMapWithTimedEviction();
    }


    public Object lookup() {
        // map key
        // find nearest from cache: cache.lookup(key);
        return new Object();
    }

    public void update() {
        // map key
        // find nearest: replace or insert
        // calculate value
        // cache.push()
    }

    public void registerComparator(String name, Comparator x) {
        cmpMap.put(name, x);
    }

    // 1) tune threshold. Step size: % when big, fixed when small;
    // 2) set 80% as satisfied bar, lower will trigger a change
    public void tuneThresh(String name, Double feedback) {}


    // Interaction with client
    public void handler (int val, Serializable params) {
        switch (val) {
            case 1:
                // deserialize object
                lookup();
                break;
            case 2:
                // deserialize object
                update();
                break;
            case 3:
                // deserialize object
                // registerComparator(params1, params2);
                break;
            case 4:
                // deserialize object
                // tuneThresh(params1, params2);
                break;

        }
    }



    public static void main(String[] args) {
        ServiceDaemon dae = new ServiceDaemon();
        System.out.println("Start from here.");
    }
}
