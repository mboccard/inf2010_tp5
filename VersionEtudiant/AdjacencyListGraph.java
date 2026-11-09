import java.util.*;

/**
 * Graph stored as one hash map (neighbor -> weight) per vertex.
 * LinkedHashMap keeps insertion order so output is deterministic.
 *
 * Space: Theta(V + E).
 * hasEdge / weight: O(1) expected.
 * adj(v): O(out-degree), so BFS is O(V + E) and heap Dijkstra is
 * O((V + E) log V).
 */
public class AdjacencyListGraph extends Graph {

    private final List<Map<Integer, Integer>> neighbors;

    public AdjacencyListGraph(int V, boolean directed) {
        super(V, directed);
        neighbors = new ArrayList<>(V);
        for (int v = 0; v < V; v++) neighbors.add(new LinkedHashMap<>());
    }

    @Override
    protected void addArc(int v1, int v2, int weight) {
    }

    @Override
    public boolean hasEdge(int v1, int v2) {
        return false;
    }

    @Override
    public int weight(int v1, int v2) {
        return 0;
    }

    @Override
    public Collection<Integer> adj(int v) {
        return new ArrayList<>();
    }
}
