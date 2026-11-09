/** A list of weighted directed edges stored as three parallel int arrays. */
public final class EdgeList {
    public final int[] from, to, weight;

    public EdgeList(int E) {
        from = new int[E];
        to = new int[E];
        weight = new int[E];
    }

    public int size() { return from.length; }
}
