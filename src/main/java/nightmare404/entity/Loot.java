package nightmare404.entity;

// What a boss drops where it falls (see Encounters): the key to its floor's way out, and some
// supplies for the battles ahead. found is what the player reads when they pick it up.
public class Loot {
    private final String key;
    private final String found;
    private final int kits, textbooks, redPens, hallPasses;

    public Loot(String key, String found, int kits, int textbooks, int redPens, int hallPasses) {
        this.key = key;
        this.found = found;
        this.kits = kits;
        this.textbooks = textbooks;
        this.redPens = redPens;
        this.hallPasses = hallPasses;
    }

    public String key() {
        return key;
    }

    public String found() {
        return found;
    }

    public int kits() {
        return kits;
    }

    public int textbooks() {
        return textbooks;
    }

    public int redPens() {
        return redPens;
    }

    public int hallPasses() {
        return hallPasses;
    }

    // "+1 First Aid Kit, +1 Red Pen": the supplies, for the message
    public String supplies() {
        StringBuilder s = new StringBuilder();
        add(s, kits, "First Aid Kit");
        add(s, textbooks, "Old Textbook");
        add(s, redPens, "Red Pen");
        add(s, hallPasses, "Hall Pass");
        return s.toString();
    }

    private static void add(StringBuilder s, int n, String what) {
        if (n <= 0) return;
        if (s.length() > 0) s.append(", ");
        s.append("+").append(n).append(" ").append(what);
        if (n > 1) s.append("s");
    }
}
