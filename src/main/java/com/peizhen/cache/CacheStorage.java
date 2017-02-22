package com.peizhen.cache;

import com.peizhen.cache.manipulator.VectorEntryManipulator;
import com.peizhen.cache.map.ConcurrentMapWithValuedEvictionDecorator;
import com.peizhen.cache.map.EvictibleValueEntry;
import com.peizhen.cache.scheduler.SingleThreadEvictionValueScheduler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.lang.Class;

/**
 * Created by GPZ on 2/7/17.
 */
public class CacheStorage {

    public Map<String, Map<String, ConcurrentMapWithValuedEvictionDecorator>> storage_;

    // Comparator under different function use the same comparator
    public Map<String, EntryManipulator> manipulatorMap_;

    // Store all threshold for all function-inputCmper pair
    public Map<String, Map<String, Double>> threshold_;

    public long MAXSIZE = 100;
    // Value type is decided by functionName: used to cast output
    //public Map<String, Class<?>> valTypeContainer;
    // Key type is decided by comparatorName: used to cast output
    //public Map<String, Class<?>> keyTypeContainer;


    public CacheStorage(long max) {
        storage_ = new ConcurrentHashMap<>();
        manipulatorMap_ = new ConcurrentHashMap<>();
        threshold_ = new ConcurrentHashMap<>();
        MAXSIZE = max;
        //valTypeContainer = new HashMap<>();
        //keyTypeContainer = new HashMap<>();
    }

    /**
     *
     * @return total importance
     */
    public long importance() {
        long currentSize = 0;
        Iterator it = storage_.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Map<String, ConcurrentMapWithValuedEvictionDecorator>> entry = (Map.Entry<String, Map<String, ConcurrentMapWithValuedEvictionDecorator>>)it.next();
            Iterator it1 = entry.getValue().entrySet().iterator();
            while (it1.hasNext()) {
                Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator> ent = (Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator>) it.next();
                currentSize += ent.getValue().volumn();
            }
        }
        return currentSize;
    }

    /**
     * @return the breakdown of each function's entries' importance
     * Long: 64bits, the low 16 bits store size, high 48 bits store importance(volumn)
     */
    public Map<String, Long> importanceBreakdown() {
        Map<String, Long> ret = new HashMap<>();
        Iterator it = storage_.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Map<String, ConcurrentMapWithValuedEvictionDecorator>> entry = (Map.Entry<String, Map<String, ConcurrentMapWithValuedEvictionDecorator>>)it.next();
            String name = entry.getKey();
            long num = 0;
            long size = 0;
            Iterator it1 = entry.getValue().entrySet().iterator();
            while (it1.hasNext()) {
                Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator> ent = (Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator>) it.next();
                num += ent.getValue().volumn();
                size += ent.getValue().size();
            }
            num = num << 16 + size;
            ret.put(name,num);
        }
        return ret;
    }

    /**
     * func -> double rate
     * @return Double: rate: which is  importance / size
     * space/importance reflects the computation-aware space utilization rate, larger then better
     */
    public Map<String, Double> rateBreakdown() {
        Map<String, Long> imp = importanceBreakdown();
        Map<String, Double> ret = new HashMap<>();
        for (String k : imp.keySet()) {
            imp.put(k, (imp.get(k)>>16 / imp.get(k)&0xffff));
        }
        return ret;
    }

    /*********** Register functions **********/

    public boolean hasFunction(String name) {
        return storage_.containsKey(name);
    }

    // if already exists, return false
    public boolean registerFunction(String name) {
        Map<String, ConcurrentMapWithValuedEvictionDecorator> map = new HashMap<>();
        Map<String, Double> map1 = new HashMap<>();
        //valTypeContainer.put(name, datatype);
        return (storage_.putIfAbsent(name, map) == null && threshold_.putIfAbsent(name, map1) == null);
    }

    public boolean hasManipulator(String cmpName) {
        return manipulatorMap_.containsKey(cmpName);
    }

    public boolean registerManipulator(String cmpName, EntryManipulator manipulator) {
        //keyTypeContainer.put(cmpName, keytype);
        return (manipulatorMap_.putIfAbsent(cmpName, manipulator) == null);
    }

    public boolean hasManipulatorInFunction(String name, String cmp) {
        if (hasFunction(name)) {
            if (storage_.get(name).containsKey(cmp)) return true;
        }
        return false;
    }

    /**
     * NOTE: all the types are infered by the value added into the map, so BE CAREFUL TO ADD VALUES
     * Note: 1) first register func, then register comparator, and then registerCompForFunc
     * @param name
     * @param cmpName
     * @return
     */
    public boolean registerComparatorForFunction(String name, String cmpName, Double thresh) {
        EntryManipulator manipulator = manipulatorMap_.get(cmpName);
        if (!hasFunction(name)) {
            return false;
        } else if (!hasManipulator(cmpName) || hasManipulatorInFunction(name, cmpName)){
            return false;
        }
        else {
            ConcurrentMapWithValuedEvictionDecorator submap = new ConcurrentMapWithValuedEvictionDecorator<>(
                    new ConcurrentSkipListMap<>(manipulator.getComparator()), new SingleThreadEvictionValueScheduler<>());
            storage_.get(name).put(cmpName, submap);
            threshold_.get(name).put(cmpName, thresh);
        }
        return true;
    }


    /****    Begin of LOOKUP/UPDATE      ****/


    /**
     *
     * @param name functionName
     * @param cmp comparatorName
     * @param key K
     * @return an object which contains the nearest two values as well as the confidence
     * the return value could be confidence = 1 exact, or confidence = 2 fuzzy/traverseGet, or null
     */
    public ConcurrentMapWithValuedEviction.Reply lookup(String name, String cmp, Object key) {
        if (!hasFunction(name)) return null;
        if (!hasManipulatorInFunction(name, cmp)) return null;
        EntryManipulator manip = manipulatorMap_.get(cmp);
        if (manip == null) return null;

        Object mappedKey = manip.mapKey(key);

        /**
         * Now we check whether the comparator is null to decide whether we need traverseGet
         */
        if (true/*manip.getComparator() == null*/) {
            ConcurrentMapWithValuedEviction.Reply rep = storage_.get(name).get(cmp).traverseGet(
                                                        mappedKey, true, manip);
            if (rep.confidence == 1) return rep;
            else if (rep.confidence == 2) {
                if (rep.lowerVal != null) {
                    if (!manip.isSimilarKey(rep.lowerKey, mappedKey, threshold_.get(name).get(cmp))) {
                        if (rep.higherVal == null) return null;
                        rep.lowerKey = null;
                        rep.lowerVal = null;
                    }
                }
                if (rep.higherVal != null) {
                    if (!manip.isSimilarKey(rep.higherKey, mappedKey, threshold_.get(name).get(cmp))) {
                        if (rep.lowerVal == null) return null;
                        rep.higherKey = null;
                        rep.higherVal = null;
                    }
                }
                return rep;
            } else return null;
        }

        ConcurrentMapWithValuedEviction.Reply rep = storage_.get(name).get(cmp).fuzzyGet(mappedKey, true);

        if (rep.confidence == 1) return rep;
        else if (rep.confidence == 2) {
            if (rep.lowerVal != null) {
                if (!manip.isSimilarKey(rep.lowerKey, mappedKey, threshold_.get(name).get(cmp))) {
                    if (rep.higherVal == null) return null;
                    rep.lowerKey = null;
                    rep.lowerVal = null;
                }
            }
            if (rep.higherVal != null) {
                if (!manip.isSimilarKey(rep.higherKey, mappedKey, threshold_.get(name).get(cmp))) {
                    if (rep.lowerVal == null) return null;
                    rep.higherKey = null;
                    rep.higherVal = null;
                }
            }
            return rep;
        } else return null;
    }


    //TODO: implement key coordinated mapping, call auto-tuning threshold, call evictBySpace
    //TODO: give a few implementation of EntryManipulator
    /**
     * NOTE: we will simultaneously append it to all decorators under this functionName
     * NOTE: be careful about the type when adding to it
     * put function do not need keymapper name, because we are synchronously add them
     * @return whether we have runned probing alg.
     */
    public boolean put(String name, Object key, Object val, long weight, long evictMs) {
        boolean flag = false;
        if (Math.random() > 0) { //TODO: debug set to 1
            probeThreshold(name, key, val);
            flag = true;
        }
        Iterator it = storage_.get(name).entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator> entry = (Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator>)it.next();
            EntryManipulator manip = manipulatorMap_.get(entry.getKey());
            if (manip == null) continue;
            ConcurrentMapWithValuedEvictionDecorator decor = entry.getValue();
            decor.put(manip.mapKey(key), val, weight, evictMs);
        }
        return flag;
    }

    public boolean put(String name, Object key, Object val) {
        return put(name, key, val, 1, 0);
    }


    /*******   Importance-aware Eviction By Space    *********/
    /**
     * Evict num items from num functions with lowest utilization rate.
     * When function number is less then num, only remove function number (x) entries.
     * Thus, num should not too big, num = 1 is most robust.
     * @param num
     * @return
     */
    public int evictBySpace(int num) {
        Map<String, Double> rate = rateBreakdown();
        double totalrate = 0;
        for (String k : rate.keySet()) {
            totalrate += rate.get(k);
        }

        int x = 0;
        if (rate.size() < num) {
            x = rate.size();
        } else {
            x = num;
        }
        double[] min = new double[x];
        for (int i = 0 ; i < x; i++) min[i] = totalrate;
        String[] minkey = new String[x];
        int u = x;
        // find the num functions with lowest rate
        while (u > 0) {
            for (String k : rate.keySet()) {
                if (rate.get(k) < min[x-u]) {
                    min[x-u] = rate.get(k);
                    minkey[x-u] = k;
                }
            }
            rate.remove(minkey[x-u]);
            u--;
        }
        // for each function, use findMin() to locate which Decor has min importance item
        // And then, we call evictMin() function in that specific Decor to remove the minimum importance item
        for (int i = 0; i < min.length; i++) {
            Map<String, ConcurrentMapWithValuedEvictionDecorator> tmp = storage_.get(minkey[i]);
            long minimport = -1;
            String minName = "";
            Iterator it = tmp.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator> entry = (Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator>)it.next();
                if (minimport < 0 || entry.getValue().findMin() < minimport) {
                    minimport = entry.getValue().findMin();
                    minName = entry.getKey();
                }
            }
            tmp.get(minName).evictMin();
        }
        return x;
    }

    public int evictBySpace() {
        return evictBySpace(1);
    }



    /*******    Auto-probing    *********/


    /**
     * be called in put() function with a probability
     * lookup -> compare same? -> exponential moving avg of the threshold
     * current strategy: aggresive decrease, conservative increase
     */
    public boolean probeThreshold(String name, Object key, Object val) {
        Iterator it = storage_.get(name).entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator> entry = (Map.Entry<String, ConcurrentMapWithValuedEvictionDecorator>)it.next();
            EntryManipulator manip = manipulatorMap_.get(entry.getKey());
            ConcurrentMapWithValuedEvictionDecorator decor = entry.getValue();

            if (manip == null) return false;
            Object mappedKey = manip.mapKey(key);
            ConcurrentMapWithValuedEviction.Reply rep = decor.fuzzyGet(mappedKey, false);
            if (rep.exactVal != null || rep.confidence == 3) continue;
            else if (rep.confidence == 2) {
                double ksiml, ksimh;
                boolean samel, sameh;
                double thresh = threshold_.get(name).get(entry.getKey());
                if (rep.lowerKey != null) {
                    ksiml = manip.keySimilarity(mappedKey, rep.lowerKey);
                    samel = manip.isSameResult(val, rep.lowerVal);
                    if (ksiml < thresh && !samel) { // within threshold but not same result -> aggresively decrease thresh
                        threshold_.get(name).put(entry.getKey(), thresh*0.8);
                    } else if (ksiml > thresh && samel) { // beyond threshold but same result -> conservative increase thresh
                        threshold_.get(name).put(entry.getKey(), thresh+Math.min(thresh*0.1, 0.05));
                    }
                }
                if (rep.higherKey != null) {
                    ksimh = manip.keySimilarity(mappedKey, rep.higherKey);
                    sameh = manip.isSameResult(val, rep.higherVal);
                    if (ksimh < thresh && !sameh) { // within threshold but not same result -> aggresively decrease thresh
                        threshold_.get(name).put(entry.getKey(), thresh*0.8);
                    } else if (ksimh > thresh && sameh) { // beyond threshold but same result -> conservative increase thresh
                        threshold_.get(name).put(entry.getKey(), thresh+Math.min(thresh*0.1, 0.05));
                    }
                }

            }

        }
        return true;

    }

    public static void main(String[] args) {
        CacheStorage service = new CacheStorage(100);
        VectorEntryManipulator vecManip = new VectorEntryManipulator();
        VectorEntryManipulator vecManip1 = new VectorEntryManipulator();


        service.registerFunction("func1");
        service.registerFunction("func2");
        service.registerFunction("func3");

        if (!service.hasManipulator("naive")) {
            service.registerManipulator("naive",vecManip);
        }
        if (!service.hasManipulator("simple")) {
            service.registerManipulator("simple",vecManip1);
        }

        boolean x1= service.registerComparatorForFunction("func1", "naive", 10.0);
        System.out.println(x1);
        boolean x2= service.registerComparatorForFunction("func1", "simple", 10.2);
        System.out.println(x2);
        boolean x3= service.registerComparatorForFunction("func2", "naive", 10.1);
        System.out.println(x3);


        List<Double> key1 = new ArrayList<>(Arrays.asList(2.0, 2.0, 0.0));
        String val1 = "apple";

        List<Double> key2 = new ArrayList<>(Arrays.asList(1.0, 1.0, 3.9));
        String val2 = "pear";

        service.put("func1",key1, val1, 1, 0);
        service.put("func1", key2, val2, 1, 0);

        List<Double> key3 = new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0));
        ConcurrentMapWithValuedEviction.Reply ret1 = service.lookup("func1","naive",key3);

        System.out.println(service.storage_.get("func1").get("simple").size());
        System.out.println(service.storage_.get("func1").get("naive").size());

        System.out.println(ret1.confidence);


    }
}
