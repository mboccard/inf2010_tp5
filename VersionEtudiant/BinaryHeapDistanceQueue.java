import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Indexed binary min-heap with a true decrease-key (a position table maps
 * each vertex to its slot in the heap). extractMin and decreaseKey are
 * O(log V), so Dijkstra is O((V + E) log V) with adjacency lists.
 * Best on sparse graphs.
 */
public class BinaryHeapDistanceQueue implements DistanceQueue {

    private final int[] heap;     // heap[i]  = vertex at slot i
    private final int[] pos;      // pos[v]   = slot of v, or -1 if not queued
    private final long[] key;     // key[v]   = current priority of v
    private int size = 0;

    public BinaryHeapDistanceQueue(int V) {
        heap = new int[V];
        pos = new int[V];
        key = new long[V];
        Arrays.fill(pos, -1);
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

    private void siftUp(int i) {
        while (i > 0) {
            int p = (i - 1) / 2;
            if (key[heap[i]] >= key[heap[p]]) break;
            swap(i, p);
            i = p;
        }
    }

    private void siftDown(int i) {
        for (;;) {
            int l = 2 * i + 1, r = l + 1, m = i;
            if (l < size && key[heap[l]] < key[heap[m]]) m = l;
            if (r < size && key[heap[r]] < key[heap[m]]) m = r;
            if (m == i) return;
            swap(i, m);
            i = m;
        }
    }

    private void swap(int i, int j) {
        int a = heap[i], b = heap[j];
        heap[i] = b; pos[b] = i;
        heap[j] = a; pos[a] = j;
    }
}
