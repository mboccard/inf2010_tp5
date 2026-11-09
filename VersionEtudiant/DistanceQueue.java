/**
 * Min-priority queue over the vertices 0..V-1, keyed by tentative distance.
 * This is the only thing Dijkstra needs from its "frontier" structure.
 */
public interface DistanceQueue {

    /** Insert v with this key (i.e., best known distance),
        or lower its key if v is already queued. */
    void decreaseKey(int v, long key);

    /** Remove and return the vertex with the smallest key. */
    int extractMin();

    boolean isEmpty();
}
