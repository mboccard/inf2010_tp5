import java.util.Arrays;

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
    }

    @Override
    public int extractMin() {
        return -1;
    }

    @Override
    public boolean isEmpty() { return size == 0; }
}
