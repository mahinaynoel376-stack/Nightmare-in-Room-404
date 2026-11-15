package nightmare404.entity;

// What a boss drops where it falls (see Encounters): the key to its floor's way out, and some
// supplies for the battles ahead. found is what the player reads when they pick it up.
public record Loot(String key, String found, int kits, int textbooks, int redPens, int hallPasses) {

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
        s.append("+").append(n).append(" ").append(what).append(n > 1 ? "s" : "");
    }
}
