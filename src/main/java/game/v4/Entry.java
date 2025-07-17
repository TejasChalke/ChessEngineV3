package game.v4;

public class Entry {
    public static byte TYPE_EXACT = 0;
    public static byte TYPE_LOWER_BOUND = 1;
    public static byte TYPE_UPPER_BOUND = 2;

    byte type;
    int depth;
    int value;
    Move move;
    long hash;

    Entry(long h, byte t, int d, int v, Move m) {
        hash = h;
        type = t;
        depth = d;
        value = v;
        move = m;
    }
}
