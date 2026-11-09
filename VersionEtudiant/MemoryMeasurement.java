import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Measures the heap memory RETAINED by a finished graph.
 *
 * For each trial: force GC, read used heap, build the graph, force GC again,
 * read used heap, and take the difference. The median over TRIALS is reported.
 * Measuring once per graph (not once per edge) and only after collections
 * avoids the negative values caused by GC running during construction.
 *
 * Do not run with -XX:+DisableExplicitGC: it turns System.gc() into a no-op.
 */
public final class MemoryMeasurement {

    private MemoryMeasurement() {}

    public static final int TRIALS = 5;
    public static final int GC_ROUNDS = 5;

    /**
     * Used heap with all unreachable objects gone.
     *
     * One System.gc() is NOT enough. HotSpot's serial mark-compact collector
     * only compacts fully on every 4th full collection (MarkSweepAlwaysCompactCount=4);
     * on the others it leaves "dead wood" (unreachable objects) in place, and
     * freeMemory() still counts it as used. A graph that was just discarded
     * therefore keeps inflating the next baseline reading for up to 3 GCs,
     * which is another source of negative differences.
     *
     * Dead wood can only make a reading too HIGH, never too low, so we run
     * GC_ROUNDS (>= 4) collections, which always includes a compacting one,
     * and keep the smallest reading.
     */
    public static long usedAfterGC() {
        Runtime rt = Runtime.getRuntime();
        long min = Long.MAX_VALUE;
        for (int i = 0; i < GC_ROUNDS; i++) {
            System.gc();
            min = Math.min(min, rt.totalMemory() - rt.freeMemory());
        }
        return min;
    }

    /** Bytes retained by the graph that build.get() returns. */
    public static long retainedBytes(Supplier<Graph> build) {
        long before = usedAfterGC();
        Graph g = build.get();
        long after = usedAfterGC();
        Reference.reachabilityFence(g);          // g must stay alive until the second reading
        return after - before;
    }

    /**
     * Median of TRIALS measurements of the graph obtained by filling a fresh
     * empty graph (from the supplier) with the edges of el.
     */
    public static long medianRetained(Supplier<Graph> empty, EdgeList el) {
        long[] results = new long[TRIALS];
        for (int t = 0; t < TRIALS; t++)
            results[t] = retainedBytes(() -> GraphGenerator.build(empty.get(), el));
        Arrays.sort(results);
        return results[TRIALS / 2];
    }
}
