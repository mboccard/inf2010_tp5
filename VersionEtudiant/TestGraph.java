public class TestGraph {

    static void verifier(String nom, boolean condition) {
        System.out.println((condition ? "OK    " : "ECHEC ") + nom);
    }

    static void testerDirige(Graph g) {
        System.out.println("=== " + g.getClass().getSimpleName() + " ===");

        g.connect(0, 1, 5);
        g.connect(0, 2, 3);
        g.connect(2, 3, 1);

        verifier("hasEdge(0,1) vrai", g.hasEdge(0, 1));
        verifier("hasEdge(1,0) faux (dirigé)", !g.hasEdge(1, 0));
        verifier("weight(0,1) == 5", g.weight(0, 1) == 5);
        verifier("adj(0) contient 1 et 2", g.adj(0).contains(1) && g.adj(0).contains(2));
        verifier("adj(0) a taille 2", g.adj(0).size() == 2);
        verifier("adj(3) vide (aucun arc sortant)", g.adj(3).isEmpty());
        verifier("E == 3", g.E() == 3);
        verifier("doublon ignoré", !g.connect(0, 1, 9));
        verifier("poids inchangé après doublon", g.weight(0, 1) == 5);
        verifier("E inchangé après doublon", g.E() == 3);
        verifier("sommet invalide (-1) ignoré", !g.connect(-1, 2));
        verifier("sommet invalide (10) ignoré", !g.connect(0, 10));

        try {
            g.weight(1, 3);
            verifier("weight absent lance une exception", false);
        } catch (java.util.NoSuchElementException e) {
            verifier("weight absent lance une exception", true);
        }

        System.out.println(g);
    }

    static void testerNonDirige(Graph g) {
        System.out.println("=== " + g.getClass().getSimpleName() + " (non dirigé) ===");

        g.connect(0, 1, 5);

        verifier("hasEdge(0,1) vrai", g.hasEdge(0, 1));
        verifier("hasEdge(1,0) vrai", g.hasEdge(1, 0));
        verifier("weight(1,0) == 5", g.weight(1, 0) == 5);
        verifier("E == 1 (une arête compte une fois)", g.E() == 1);
        verifier("doublon inverse ignoré", !g.connect(1, 0, 9));
        verifier("E toujours 1", g.E() == 1);
        verifier("adj(1) contient 0", g.adj(1).contains(0));

        System.out.println(g);
    }

    public static void main(String[] args) {
        testerDirige(new AdjacencyListGraph(4, true));
        testerDirige(new AdjacencyMatrixGraph(4, true));
        testerNonDirige(new AdjacencyListGraph(4, false));
        testerNonDirige(new AdjacencyMatrixGraph(4, false));
    }
}