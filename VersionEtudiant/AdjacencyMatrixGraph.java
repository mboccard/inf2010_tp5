import java.util.*;

/**
 * Graph stored as a V x V matrix of weights; NO_EDGE marks a missing arc.
 *
 * Space: Theta(V^2) regardless of E.
 * hasEdge / weight: O(1).
 * adj(v): O(V) (must scan a full row), so BFS and Dijkstra cost
 * O(V^2) at least, even on a sparse graph.
 */
public class AdjacencyMatrixGraph extends Graph {

    private static final int NO_EDGE = Integer.MAX_VALUE;
    private final int[][] w;

    public AdjacencyMatrixGraph(int V, boolean directed) {
        super(V, directed);
        w = new int[V][V];
        for (int[] row : w) Arrays.fill(row, NO_EDGE);
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
