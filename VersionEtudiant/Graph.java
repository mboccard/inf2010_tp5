import java.util.*;
import java.util.function.IntFunction;

/**
 * Abstract weighted graph on vertices 0..V-1 (directed or undirected).
 *
 * Subclasses only decide HOW adjacency is stored (matrix, lists, ...).
 * Everything else (connect, in-degrees, transposition, topological order,
 * BFS, Dijkstra) is written once here, against the abstract primitives
 * hasEdge / weight / adj / addArc. This makes the cost of each storage
 * choice easy to compare: the algorithms are identical, only the
 * primitives differ.
 *
 * Conventions (from the course slides):
 *  - connect() on an existing edge is ignored.
 *  - Out-of-range vertices are ignored.
 *  - For an undirected graph, one edge counts once in E() but is stored
 *    as two arcs.
 */
public abstract class Graph {

    public static final long INFINITY = Long.MAX_VALUE;

    protected final int V;
    protected final boolean directed;
    protected int E;
    protected final int[] inDegree;

    protected Graph(int V, boolean directed) {
        if (V < 0) throw new IllegalArgumentException("V must be >= 0");
        this.V = V;
        this.directed = directed;
        this.E = 0;
        this.inDegree = new int[V];
    }

    // ------------------------------------------------------------------
    // Storage-specific primitives (implemented by subclasses)
    // ------------------------------------------------------------------

    /** Store ONE arc v1 -> v2 (the caller has validated and de-duplicated). */
    protected abstract void addArc(int v1, int v2, int weight);

    /** Is there an arc v1 -> v2? */
    public abstract boolean hasEdge(int v1, int v2);

    /** Weight of arc v1 -> v2; throws NoSuchElementException if absent. */
    public abstract int weight(int v1, int v2);

    /** Out-neighbors of v. */
    public abstract Collection<Integer> adj(int v);


    // ------------------------------------------------------------------
    // Common structure
    // ------------------------------------------------------------------

    public final int V() { return V; }
    public final int E() { return E; }
    public final boolean isDirected() { return directed; }
    public final int[] getInDegree() { return inDegree.clone(); }

    protected final boolean valid(int v) { return v >= 0 && v < V; }

    public final boolean connect(int v1, int v2) { return connect(v1, v2, 1); }

    /** @return true if a new edge was added, false if ignored. */
    public final boolean connect(int v1, int v2, int weight) {
        if (!valid(v1) || !valid(v2)) return false;
        if (hasEdge(v1, v2)) return false;
        addArc(v1, v2, weight);
        inDegree[v2]++;
        if (!directed && v1 != v2) {
            addArc(v2, v1, weight);
            inDegree[v1]++;
        }
        E++;
        return true;
    }


    @Override
    public String toString() {
        StringBuilder o = new StringBuilder();
        String ln = System.lineSeparator();
        String arrow = directed ? "->" : "-";
        o.append(V).append(ln).append(E).append(ln);
        for (int v = 0; v < V; v++)
            for (int w : adj(v))
                o.append(v).append(arrow).append(w).append(" (").append(weight(v, w)).append(')').append(ln);
        return o.toString();
    }

    // ------------------------------------------------------------------
    // Shortest paths object (used below for Dijkstra)
    // ------------------------------------------------------------------

    /** Result of a single-source shortest-path computation. */
    public static final class ShortestPaths {
        public final int source;    // The starting node
        public final long[] dist;   // Shortest distance to each node; INFINITY if unreachable
        public final int[] parent;  // Previous node in optimal path; -1 for source / unreachable

        ShortestPaths(int source, long[] dist, int[] parent) {
            this.source = source; this.dist = dist; this.parent = parent;
        }

        public long distanceTo(int t) { return dist[t]; }

        /** Vertices from source to t, or an empty list if t is unreachable. */
        public List<Integer> pathTo(int t) {
            LinkedList<Integer> path = new LinkedList<>();
            if (dist[t] == INFINITY) return path;
            for (int v = t; v != -1; v = parent[v]) path.addFirst(v);
            return path;
        }
    }

    private ShortestPaths init(int s) {
        if (!valid(s)) throw new IllegalArgumentException("bad source " + s);
        long[] dist = new long[V];
        int[] parent = new int[V];
        Arrays.fill(dist, INFINITY);
        Arrays.fill(parent, -1);
        dist[s] = 0;
        return new ShortestPaths(s, dist, parent);
    }

    /**
     * Dijkstra's algorithm, independent of both the graph storage (through
     * adj/weight) and the priority-queue structure (through DistanceQueue).
     *
     * @param queueFactory creates an empty queue for V vertices, e.g.
     *                     {@code ArrayDistanceQueue::new} or
     *                     {@code BinaryHeapDistanceQueue::new}
     *
     * Running time = V * extractMin + (number of successful relaxations)
     * * decreaseKey, plus V adj() calls. Requires non-negative weights.
     */
    public final ShortestPaths dijkstra(int s, IntFunction<DistanceQueue> queueFactory) {
        ShortestPaths sp = init(s);                 // best known distance to each node, and previous nodes on best known paths
        DistanceQueue q = queueFactory.apply(V);    // new queue of size V
        q.decreaseKey(s, 0);

        while (!q.isEmpty()) {
            int v = q.extractMin();          // distance of v is now final
            for (int w : adj(v)) {          // check each of v's neighbours
                int c = weight(v, w);
                if (c < 0) throw new IllegalStateException("negative weight " + v + "->" + w);
                long nd = sp.dist[v] + c;
                if (nd < sp.dist[w]) {      // Can we do better than known optima at w? Never true for already-final w
                    sp.dist[w] = nd;        // If so, update best known paths
                    sp.parent[w] = v;
                    q.decreaseKey(w, nd);   // Queue w if not done already, and update its known distance (queue priority)
                }
            }
        }
        return sp;
    }
}
