import java.util.BitSet;
import java.util.Random;

/** Random edge lists, and building a Graph from one. */
public final class GraphGenerator {

    private GraphGenerator() {}

    public static final int MAX_WEIGHT = 100;

    /**
     * E distinct directed edges, no self-loops, random order, weights in
     * [1, MAX_WEIGHT]. Each ordered pair (a, b), a != b, gets an index
     * k in [0, V(V-1)); a BitSet records which indices are used. If more than
     * half the pairs are wanted we choose the pairs to EXCLUDE instead, so
     * rejection sampling never takes more than ~2 tries per pick, even for a
     * complete graph.
     */
    public static EdgeList generateEdges(int V, int E, Random r) {
        long maxE = (long) V * (V - 1);
        if (E > maxE) throw new IllegalArgumentException("cannot create " + E + " distinct edges with " + V + " vertices");
        if (maxE > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("V too large");
        int M = (int) maxE;

        boolean complement = 2L * E > M;
        int toPick = complement ? M - E : E;
        BitSet chosen = new BitSet(M);
        for (int picked = 0; picked < toPick; ) {
            int k = r.nextInt(M);
            if (!chosen.get(k)) { chosen.set(k); picked++; }
        }

        EdgeList el = new EdgeList(E);
        int i = 0;
        for (int k = 0; k < M; k++) {
            if (chosen.get(k) != complement) {
                int a = k / (V - 1);
                int rem = k % (V - 1);
                el.from[i] = a;
                el.to[i] = rem >= a ? rem + 1 : rem;   // skip the diagonal
                el.weight[i] = r.nextInt(MAX_WEIGHT) + 1;
                i++;
            }
        }
        for (int j = E - 1; j > 0; j--) {              // shuffle (the loop above emitted them sorted)
            int p = r.nextInt(j + 1);
            swap(el.from, j, p); swap(el.to, j, p); swap(el.weight, j, p);
        }
        return el;
    }

    /**
     * Like generateEdges, but strongly connected: every vertex can reach every
     * other, so Dijkstra from any source explores the WHOLE graph. (A random
     * digraph with E close to V leaves most vertices unreachable from the
     * source, so a timing benchmark would mostly measure how small the
     * reachable part is.)
     *
     * Construction: a random Hamiltonian cycle (V edges) plus E - V further
     * distinct random edges. Requires V <= E <= V(V-1).
     */
    public static EdgeList generateConnectedEdges(int V, int E, Random r) {
        long maxE = (long) V * (V - 1);
        if (V < 2 || E < V || E > maxE)
            throw new IllegalArgumentException("need 2 <= V <= E <= V(V-1); got V=" + V + ", E=" + E);
        if (maxE > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("V too large");
        int M = (int) maxE;

        EdgeList el = new EdgeList(E);
        BitSet used = new BitSet(M);                       // pair indices already in the graph
        int[] perm = new int[V];
        for (int v = 0; v < V; v++) perm[v] = v;
        for (int j = V - 1; j > 0; j--) swap(perm, j, r.nextInt(j + 1));

        int i = 0;
        for (int k = 0; k < V; k++) {                      // the cycle
            int a = perm[k], b = perm[(k + 1) % V];
            el.from[i] = a; el.to[i] = b;
            used.set(a * (V - 1) + (b > a ? b - 1 : b));
            i++;
        }

        int free = M - V, extra = E - V;
        boolean complement = 2L * extra > free;            // same trick as generateEdges
        int toPick = complement ? free - extra : extra;
        BitSet picked = new BitSet(M);
        for (int c = 0; c < toPick; ) {
            int k = r.nextInt(M);
            if (!used.get(k) && !picked.get(k)) { picked.set(k); c++; }
        }
        for (int k = 0; k < M; k++) {
            if (!used.get(k) && picked.get(k) != complement) {
                int a = k / (V - 1);
                int rem = k % (V - 1);
                el.from[i] = a;
                el.to[i] = rem >= a ? rem + 1 : rem;
                i++;
            }
        }

        for (int j = 0; j < E; j++) el.weight[j] = r.nextInt(MAX_WEIGHT) + 1;
        for (int j = E - 1; j > 0; j--) {
            int p = r.nextInt(j + 1);
            swap(el.from, j, p); swap(el.to, j, p); swap(el.weight, j, p);
        }
        return el;
    }

    private static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    /** Connect every edge of el in g (which should be empty) and return g. */
    public static Graph build(Graph g, EdgeList el) {
        for (int i = 0; i < el.size(); i++) g.connect(el.from[i], el.to[i], el.weight[i]);
        return g;
    }
}
