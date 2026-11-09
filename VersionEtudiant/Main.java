import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.function.IntFunction;

/**
 * One benchmark for SPACE and TIME of all four variants:
 * {adjacency list, adjacency matrix} x {array queue, binary-heap queue}.
 *
 * Usage:
 *   java -XX:+UseSerialGC -Xms2g -Xmx2g MainCompare in.tsv out.tsv graphCount [verify]
 *
 *   in.tsv      one line per graph size; the first TAB-separated field is V
 *   out.tsv     results are APPENDED, one TAB-separated row per (V, E); columns:
 *                 V, E
 *                 listBytes, matBytes, mem_mat_over_list     (retained memory)
 *                 <variant>_median_ms     for the 4 variants   (typical time)
 *                 <variant>_min_ms        for the 4 variants   (least interference)
 *                 <variant>_iqr_ms        for the 4 variants   (how noisy)
 *                 list_array_over_heap, mat_array_over_heap    (queue effect)
 *                 mat_over_list_array, mat_over_list_heap      (storage effect)
 *                 <variant>_slowdown_vs_best  for the 4 variants
 *               Variants: listArray, listHeap, matArray, matHeap.
 *               The four ratio columns and the slowdowns are medians of PAIRED
 *               ratios: within each timing round all four variants run on the
 *               same graph, so graph differences and machine drift cancel.
 *               A value above 1 in the first ratio group means the heap is faster;
 *               above 1 in the second means the matrix is slower.
 *   graphCount  number of edge counts per V (see edgeCounts)
 *   verify      optional: check that all four variants give identical distances
 *
 * Method. Memory: the retained size of the finished graph, after repeated GCs
 * (see MemoryMeasurement); measured once per (V, E) on the first graph.
 * Time: JIT warm-up first; GRAPHS_PER_POINT strongly connected random graphs
 * per (V, E), each timed many rounds with the four variants in rotating order;
 * GC before each graph; every result consumed so the work cannot be optimized away.
 *
 * Files: Main (edgeCounts), GraphGenerator, EdgeList, MemoryMeasurement,
 * Graph and its implementations.
 */
public class Main {

    static final long SEED = 42;
    static final long LIST_BYTES_PER_EDGE = 64;

    static final int GRAPHS_PER_POINT = 5;      // random graphs per (V, E)
    static final int MIN_REPS = 5;              // timed rounds per graph (a round = all 4 variants)
    static final int MAX_REPS = 30;
    static final double GRAPH_BUDGET_MS = 1000; // aim to spend about this long timing each graph
    static final int WARMUP_ROUNDS = 200;

    static final String[] NAMES = {"listArray", "listHeap", "matArray", "matHeap"};
    static final List<IntFunction<DistanceQueue>> QUEUES = List.of(
            ArrayDistanceQueue::new, BinaryHeapDistanceQueue::new,
            ArrayDistanceQueue::new, BinaryHeapDistanceQueue::new);

    static long sink;                           // consumes results so the work cannot be optimized away

        // ------------------------------------------------------------------
    // Which edge counts to test: change this method to change the experiment
    // ------------------------------------------------------------------

    /**
     * n distinct edge counts for V vertices, strictly increasing, exactly V
     * first and V(V-1) last, spaced so that the estimated list/matrix
     * crossover falls at the MIDDLE of the sequence (half the points favour
     * the list, half the matrix).
     *
     * With t = i/(n-1) in [0,1] we use E(t) = minE + (maxE - minE) * t^p.
     * Requiring E(1/2) = crossE gives p = ln(f) / ln(1/2), where
     * f = (crossE - minE) / (maxE - minE). p > 1 packs points toward low E.
     * crossE is where the matrix (4V^2 + 16V bytes) equals the list
     * (about LIST_BYTES_PER_EDGE bytes per edge).
     *
     * Near the low end the ideal spacing can be below one edge, so the raw
     * values are rounded UP and then forced strictly increasing (a forward
     * pass, then a backward pass that keeps the last value at V(V-1)).
     * If fewer than n distinct values exist, fewer are returned.
     */
    static long[] edgeCounts(int V, int n) {
        long minE = V;
        long maxE = (long) V * (V - 1);
        n = (int) Math.min(n, maxE - minE + 1);        // cannot have more distinct values than this
        long[] e = new long[n];
        if (n == 1) { e[0] = minE; return e; }

        double crossE = (4.0 * V * V + 16.0 * V) / LIST_BYTES_PER_EDGE;
        double f = (crossE - minE) / (maxE - minE);
        double p = (f > 0 && f < 1) ? Math.log(f) / Math.log(0.5) : 1.0;  // tiny V: linear

        for (int i = 0; i < n; i++) {
            double t = (double) i / (n - 1);
            e[i] = minE + (long) Math.ceil((maxE - minE) * Math.pow(t, p));
        }
        e[0] = minE;
        e[n - 1] = maxE;
        for (int i = 1; i < n; i++)         e[i] = Math.max(e[i], e[i - 1] + 1);   // no duplicates
        e[n - 1] = maxE;                                                           // the push may overshoot
        for (int i = n - 2; i >= 0; i--)    e[i] = Math.min(e[i], e[i + 1] - 1);   // pull back below maxE
        return e;
    }

    // ------------------------------------------------------------------
    // Timing helpers
    // ------------------------------------------------------------------

    /** One Dijkstra run from vertex 0; returns the time in milliseconds. */
    static double timeMs(Graph g, IntFunction<DistanceQueue> queue) {
        long t0 = System.nanoTime();
        Graph.ShortestPaths sp = g.dijkstra(0, queue);
        long t1 = System.nanoTime();
        sink += sp.dist[sp.dist.length - 1];
        return (t1 - t0) / 1e6;                 // ns -> ms
    }

    /** Run all four variants on a moderate graph so the JIT compiles every code path before we measure. */
    static void warmUp() {
        Random r = new Random(SEED + 1);        // separate stream: does not disturb the data
        int V = 300, E = 6000;
        EdgeList el = GraphGenerator.generateConnectedEdges(V, E, r);
        Graph list = GraphGenerator.build(new AdjacencyListGraph(V, true), el);
        Graph mat = GraphGenerator.build(new AdjacencyMatrixGraph(V, true), el);
        Graph[] g = {list, list, mat, mat};
        for (int i = 0; i < WARMUP_ROUNDS; i++)
            for (int v = 0; v < 4; v++) timeMs(g[v], QUEUES.get(v));
    }

    static double[] sorted(double[] a) { double[] c = a.clone(); Arrays.sort(c); return c; }

    static double percentile(double[] sorted, double p) {
        double idx = p * (sorted.length - 1);
        int lo = (int) Math.floor(idx), hi = (int) Math.ceil(idx);
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (idx - lo);
    }

    static double median(double[] a) { return percentile(sorted(a), 0.5); }

    // ------------------------------------------------------------------
    // Output
    // ------------------------------------------------------------------

    static FileWriter openTsv(String path) throws IOException {
        File f = new File(path);
        boolean fresh = !f.exists() || f.length() == 0;
        FileWriter w = new FileWriter(f, true);
        if (fresh) {
            StringBuilder h = new StringBuilder("V\tE\tlistBytes\tmatBytes");
            for (String n : NAMES) h.append('\t').append(n).append("_median_ms");
            w.write(h.append('\n').toString());
            w.flush();
        }
        return w;
    }

    // ------------------------------------------------------------------
    // Driver
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Main <input.tsv> <output.tsv> <graphCount> [verify]");
            return;
        }
        int graphCount = Integer.parseInt(args[2]);
        boolean verify = args.length > 3 && args[3].equals("verify");
        Random r = new Random(SEED);
        Runtime rt = Runtime.getRuntime();

        System.out.println("Warming up the JIT...");
        warmUp();

        try (Scanner scanner = new Scanner(new File(args[0]));
             FileWriter out = openTsv(args[1])) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                final int V;
                try {
                    V = Integer.parseInt(line.split("\t")[0].trim());
                } catch (NumberFormatException e) {
                    System.out.println("Cannot read V from line \"" + line + "\"; the input file must be TAB-separated.");
                    return;
                }
                System.out.println("V = " + V);
                if (V < 2) { System.out.println("\tSkipped: need at least 2 vertices."); continue; }

                long maxE = (long) V * (V - 1);
                for (long edgeCount : edgeCounts(V, graphCount)) {
                    int E = (int) edgeCount;

                    // Timing keeps both graphs alive at once. Will they fit? (rough, conservative)
                    long needed = 4L * V * V + 16L * V + LIST_BYTES_PER_EDGE * E + 64L * V + 12L * E + maxE / 8;
                    if (needed > rt.maxMemory() * 0.8) {
                        System.out.println("\tE = " + E + ": skipped, needs ~" + needed / (1 << 20)
                                + " MB but max heap is " + rt.maxMemory() / (1 << 20) + " MB (raise -Xmx).");
                        continue;
                    }

                    long listBytes = -1, matBytes = -1;
                    List<double[]> rounds = new ArrayList<>();      // rounds.get(i)[variant] = ms

                    for (int gi = 0; gi < GRAPHS_PER_POINT; gi++) {
                        EdgeList el = GraphGenerator.generateConnectedEdges(V, E, r);

                        if (gi == 0) {                              // space: once per (V, E), before the timing graphs exist
                            listBytes = MemoryMeasurement.medianRetained(() -> new AdjacencyListGraph(V, true), el);
                            matBytes  = MemoryMeasurement.medianRetained(() -> new AdjacencyMatrixGraph(V, true), el);
                        }

                        Graph list = GraphGenerator.build(new AdjacencyListGraph(V, true), el);
                        Graph mat = GraphGenerator.build(new AdjacencyMatrixGraph(V, true), el);
                        Graph[] g = {list, list, mat, mat};

                        if (verify) {
                            long[] ref = g[0].dijkstra(0, QUEUES.get(0)).dist;
                            for (int v = 1; v < 4; v++)
                                if (!Arrays.equals(ref, g[v].dijkstra(0, QUEUES.get(v)).dist))
                                    throw new AssertionError(NAMES[v] + " disagrees for V=" + V + ", E=" + E);
                        }

                        System.gc();            // untimed: start each graph's timing from a clean heap

                        // One untimed round primes the caches and tells us how many rounds fit the budget.
                        long c0 = System.nanoTime();
                        for (int v = 0; v < 4; v++) timeMs(g[v], QUEUES.get(v));
                        double roundMs = (System.nanoTime() - c0) / 1e6;
                        int reps = (int) Math.max(MIN_REPS, Math.min(MAX_REPS, GRAPH_BUDGET_MS / roundMs));

                        for (int rep = 0; rep < reps; rep++) {
                            double[] round = new double[4];
                            for (int k = 0; k < 4; k++) {
                                int v = (k + rep) % 4;               // rotate who goes first
                                round[v] = timeMs(g[v], QUEUES.get(v));
                            }
                            rounds.add(round);
                        }
                    }

                    // ---- statistics over all rounds ----
                    int n = rounds.size();
                    double[] median = new double[4], min = new double[4], iqr = new double[4];
                    for (int v = 0; v < 4; v++) {
                        double[] s = new double[n];
                        for (int i = 0; i < n; i++) s[i] = rounds.get(i)[v];
                        Arrays.sort(s);
                        median[v] = percentile(s, 0.5);
                        min[v] = s[0];
                        iqr[v] = percentile(s, 0.75) - percentile(s, 0.25);
                    }
                    double[] qList = new double[n], qMat = new double[n], sArr = new double[n], sHeap = new double[n];
                    double[][] slow = new double[4][n];
                    for (int i = 0; i < n; i++) {
                        double[] t = rounds.get(i);
                        qList[i] = t[0] / t[1];                     // array / heap, list graph
                        qMat[i]  = t[2] / t[3];                     // array / heap, matrix graph
                        sArr[i]  = t[2] / t[0];                     // matrix / list, array queue
                        sHeap[i] = t[3] / t[1];                     // matrix / list, heap queue
                        double best = Math.min(Math.min(t[0], t[1]), Math.min(t[2], t[3]));
                        for (int v = 0; v < 4; v++) slow[v][i] = t[v] / best;
                    }
                    double[] slowdown = new double[4];
                    for (int v = 0; v < 4; v++) slowdown[v] = median(slow[v]);

                    boolean memOk = listBytes > 0 && matBytes > 0;
                    if (!memOk)
                        System.out.println("\tE = " + E + ": memory reading unreliable (list=" + listBytes
                                + ", matrix=" + matBytes + "); is System.gc() disabled? Memory columns set to NA.");

                    // ---- write the row ----
                    StringBuilder row = new StringBuilder(V + "\t" + E);
                    if (memOk) row.append('\t').append(listBytes).append('\t').append(matBytes);
                    else row.append("\tNA\tNA\tNA");
                    for (double m : median) row.append('\t').append(m);
                    out.write(row.append('\n').toString());
                    out.flush();

                    // ---- console summary ----
                    int fastest = 0;
                    for (int v = 1; v < 4; v++) if (median[v] < median[fastest]) fastest = v;
                    System.out.printf("\tE = %d%n", E);
                    if (memOk)
                        System.out.printf("\t  space: list %.2f MB, matrix %.2f MB (matrix/list %.2f; smaller: %s)%n",
                                listBytes / 1048576.0, matBytes / 1048576.0, (double) matBytes / listBytes,
                                matBytes < listBytes ? "matrix" : "list");
                    System.out.printf("\t  time (median [min] ms, %d rounds):", n);
                    for (int v = 0; v < 4; v++) System.out.printf("  %s %.4f [%.4f]", NAMES[v], median[v], min[v]);
                    System.out.printf("%n\t  fastest: %s;  array/heap: list %.2f, matrix %.2f;  matrix/list: array %.2f, heap %.2f%n",
                            NAMES[fastest], median(qList), median(qMat), median(sArr), median(sHeap));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found: " + args[0]);
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        }
        System.out.println("(checksum " + sink + ", ignore)");
    }
}
