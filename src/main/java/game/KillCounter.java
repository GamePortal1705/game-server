package game;

import java.util.HashMap;
import java.util.Map;

/**
 * A thread safe wrapper of HashMap to keep decisions of wolves or all players.
 *
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/28/17
 */
public class KillCounter {
    private static class KillCounterSingletonHolder {
        private static final KillCounter instance = new KillCounter();
    }

    private final Map<Integer, Integer> killCounter;
    private int size;

    private KillCounter() {
        this.killCounter = new HashMap<>();
        this.size = 0;
    }

    public static KillCounter createInstance() {
        return KillCounterSingletonHolder.instance;
    }

    public synchronized void clear() {
        killCounter.clear();
        size = 0;
    }

    public synchronized boolean isEmpty() {
        return size == 0;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized void addID(int id) {
        killCounter.put(id, killCounter.getOrDefault(id, 0) + 1);
        size++;
    }

    public synchronized int playerToKill() {
        if (isEmpty()) {
            throw new IllegalArgumentException();
        }
        int max = -1;
        int id = -1;
        boolean tie = false;
        for (Map.Entry<Integer, Integer> entry : killCounter.entrySet()) {
            int val = entry.getValue();
            if (val > max) {
                tie = false;
                max = entry.getValue();
                id = entry.getKey();
            } else if (val == max) {
                tie = true;
            }
        }

        return tie ? -1 : id;
    }
}
