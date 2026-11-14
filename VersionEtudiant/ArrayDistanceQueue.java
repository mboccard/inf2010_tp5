import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Unsorted array: decreaseKey is O(1), extractMin scans all V slots: O(V).
 * Dijkstra total: O(V^2 + E) (plus neighbor enumeration cost of the graph).
 * Best on dense graphs.
 */
public class ArrayDistanceQueue implements DistanceQueue {

    //Pour chacun de key et queued, les indices correspond aux noeuds.
    private final long[] key;
    private final boolean[] queued;
    private int size = 0;

    public ArrayDistanceQueue(int V) {
        key = new long[V];
        queued = new boolean[V];
        Arrays.fill(key, Long.MAX_VALUE);
    }

    @Override
    public void decreaseKey(int v, long k) {
        if (!queued[v]) {
            queued[v] = true;
            size++;
        }
        key[v] = k;
    }

    @Override
    public int extractMin() {
        int min = -1;
        for (int v = 0 ; v < key.length ; v++) {
            if (queued[v] && (min == -1 || key[v] < key[min])) {
                min = v;
            }
        }
        if (min == -1) {
            throw new NoSuchElementException();
        }
        queued[min] = false;
        size--;
        return min;
    }

    @Override
    public boolean isEmpty() { return size == 0; }
}
