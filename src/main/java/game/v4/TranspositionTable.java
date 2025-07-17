package game.v4;

public class TranspositionTable {
    public Entry[] lookupEntries;
    private final int size;
    private int lookUpIndex;
    public static final int lookUpFailedValue = (int)1e9;

    public TranspositionTable(int size) {
        lookupEntries = new Entry[size];
        this.size = size; // size is expected to be power of 2
    }

    public void clear() {
        lookupEntries = new Entry[size];
    }

    public void putEntry(long hash, byte type, int depth, int eval, Move move) {
        lookUpIndex = (int)(hash % size);
        lookupEntries[lookUpIndex] = new Entry(hash, type, depth, eval, move);
    }

    public boolean hasEntry(long hash, int depth) {
        lookUpIndex = (int)(hash % size);
        return lookupEntries[lookUpIndex] != null && lookupEntries[lookUpIndex].hash == hash && lookupEntries[lookUpIndex].depth >= depth;
    }

    public int getEval(int alpha, int beta) {
        Entry entry = lookupEntries[lookUpIndex];
        if (entry.type == Entry.TYPE_EXACT) return entry.value;
        else if (entry.type == Entry.TYPE_LOWER_BOUND && beta <= entry.value) return entry.value;
        else if (entry.type == Entry.TYPE_UPPER_BOUND && entry.value <= alpha) return entry.value;
        else return lookUpFailedValue;
    }

    public Move getMove() {
        return lookupEntries[lookUpIndex].move;
    }
}
