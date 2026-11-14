import java.util.NoSuchElementException;

public class TestGraph {

    static void verifier(String nom, boolean condition) {
        System.out.println((condition ? "OK    " : "ECHEC ") + nom);
    }

    static void testerDirige(Graph g) {
        System.out.println("=== " + g.getClass().getSimpleName() + " (dirigé) ===");

        g.connect(0, 1, 5);
        g.connect(0, 2, 3);
        g.connect(2, 3, 1);

        verifier("hasEdge(0,1) est vrai après connect(0,1)", g.hasEdge(0, 1));
        verifier("hasEdge(1,0) est faux (l'arc n'existe que dans un sens)", !g.hasEdge(1, 0));
        verifier("weight(0,1) retourne 5", g.weight(0, 1) == 5);
        verifier("adj(0) contient les voisins 1 et 2", g.adj(0).contains(1) && g.adj(0).contains(2));
        verifier("adj(0) contient exactement 2 voisins", g.adj(0).size() == 2);
        verifier("adj(3) est vide (aucun arc sortant)", g.adj(3).isEmpty());
        verifier("E() vaut 3 après 3 connect", g.E() == 3);
        verifier("connect d'un arc déjà existant retourne false", !g.connect(0, 1, 9));
        verifier("le poids de l'arc existant n'est pas modifié par le doublon", g.weight(0, 1) == 5);
        verifier("E() ne change pas après un doublon", g.E() == 3);
        verifier("connect avec un sommet négatif (-1) retourne false", !g.connect(-1, 2));
        verifier("connect avec un sommet hors limites (10) retourne false", !g.connect(0, 10));

        try {
            g.weight(1, 3);
            verifier("weight d'un arc absent lance NoSuchElementException", false);
        } catch (NoSuchElementException e) {
            verifier("weight d'un arc absent lance NoSuchElementException", true);
        }

        System.out.println(g);
    }

    static void testerNonDirige(Graph g) {
        System.out.println("=== " + g.getClass().getSimpleName() + " (non dirigé) ===");

        g.connect(0, 1, 5);

        verifier("hasEdge(0,1) est vrai", g.hasEdge(0, 1));
        verifier("hasEdge(1,0) est vrai (l'arête va dans les deux sens)", g.hasEdge(1, 0));
        verifier("weight(1,0) retourne 5 (même poids dans les deux sens)", g.weight(1, 0) == 5);
        verifier("E() vaut 1 (une arête compte une seule fois)", g.E() == 1);
        verifier("connect(1,0) retourne false (doublon de l'arête inversée)", !g.connect(1, 0, 9));
        verifier("E() vaut toujours 1 après le doublon", g.E() == 1);
        verifier("adj(1) contient le voisin 0", g.adj(1).contains(0));

        System.out.println(g);
    }

    static void testerQueue(DistanceQueue q) {
        System.out.println("=== " + q.getClass().getSimpleName() + " ===");

        verifier("isEmpty() est vrai pour une queue neuve", q.isEmpty());

        q.decreaseKey(2, 10);
        q.decreaseKey(0, 5);
        q.decreaseKey(1, 7);
        verifier("isEmpty() est faux après 3 insertions", !q.isEmpty());

        q.decreaseKey(2, 1);

        verifier("1er extractMin retourne 2 (clé abaissée de 10 à 1)", q.extractMin() == 2);
        verifier("2e extractMin retourne 0 (clé 5)", q.extractMin() == 0);
        verifier("3e extractMin retourne 1 (clé 7)", q.extractMin() == 1);
        verifier("isEmpty() est vrai après avoir tout extrait", q.isEmpty());

        try {
            q.extractMin();
            verifier("extractMin sur une queue vide lance NoSuchElementException", false);
        } catch (NoSuchElementException e) {
            verifier("extractMin sur une queue vide lance NoSuchElementException", true);
        }
    }

    static void testerOrdreQueue(DistanceQueue q) {
        System.out.println("=== " + q.getClass().getSimpleName() + " (ordre d'extraction) ===");

        long[] cles = {50, 20, 80, 10, 70, 30, 90, 60};
        for (int v = 0; v < cles.length; v++) {
            q.decreaseKey(v, cles[v]);
        }

        boolean ordonne = true;
        long precedente = Long.MIN_VALUE;
        int nbExtraits = 0;
        while (!q.isEmpty()) {
            int v = q.extractMin();
            if (cles[v] < precedente) ordonne = false;
            precedente = cles[v];
            nbExtraits++;
        }

        verifier("les 8 sommets insérés sont tous extraits", nbExtraits == cles.length);
        verifier("les clés sortent en ordre croissant", ordonne);
    }

    public static void main(String[] args) {
        testerDirige(new AdjacencyListGraph(4, true));
        testerDirige(new AdjacencyMatrixGraph(4, true));
        testerNonDirige(new AdjacencyListGraph(4, false));
        testerNonDirige(new AdjacencyMatrixGraph(4, false));

        testerQueue(new ArrayDistanceQueue(5));
        testerQueue(new BinaryHeapDistanceQueue(5));

        testerOrdreQueue(new ArrayDistanceQueue(8));
        testerOrdreQueue(new BinaryHeapDistanceQueue(8));
    }
}