import java.util.LinkedList;

/**
 * InefficientArrayDistanceQueue: keys and values are both stored
 * in arrays with corresponding indices.  Both decreaseKey and extractMin
 * iterate over these arrays
 */
public class InefficientArrayDistanceQueue implements DistanceQueue {

    private long[] queueKeys;
    private int[] queueValues;
    private int size = 0;

    public InefficientArrayDistanceQueue(int V) {
        queueKeys = new long[V];
        queueValues = new int[V];
    }

    @Override
    public void decreaseKey(int v, long k) {
        int i = 0;
        while (i < size){
            if (v == queueValues[i]) {
                queueKeys[i] = k;
                return;
            }
        }
        queueValues[size] = v;
        queueKeys[size++] = k;
    }

    @Override
    //À faire
    public int extractMin() {
        return 0;
    }

    @Override
    public boolean isEmpty() { return size == 0; }
}
