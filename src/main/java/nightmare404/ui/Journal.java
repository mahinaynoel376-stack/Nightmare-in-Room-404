package nightmare404.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import nightmare404.entity.Clue;
import nightmare404.entity.SubjectZero;
import nightmare404.main.GamePanel;
import nightmare404.story.Director;
import nightmare404.story.Quest;
import nightmare404.world.Floor;
import nightmare404.world.World;

// The journal (J): every floor's quest, the clues found so far (and "???" for the rest), and the
// quadrangle once the player has been out there. Choose a clue with W / S and press E to read it again.
// While it's open the world waits.
public class Journal {
    private static final Font TITLE = new Font(Font.SERIF, Font.BOLD, 30);
    private static final Font HEADING = new Font(Font.SERIF, Font.BOLD, 17);
    private static final Font ENTRY = new Font(Font.SERIF, Font.PLAIN, 16);
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Color PAPER = new Color(226, 218, 196);
    private static final Color PAPER_LINE = new Color(150, 170, 210);
    private static final Color INK = new Color(34, 26, 20);
    private static final Color FADED = new Color(120, 110, 96);
    private static final Color BLOOD = new Color(150, 20, 20);

    private final GamePanel gp;
    private boolean open;
    private int selected;      // Which of the found clues is chosen

    public Journal(GamePanel gp) {
        this.gp = gp;
    }

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
        selected = Math.max(0, Math.min(selected, found().size() - 1));
    }

    public void close() {
        open = false;
    }

    public void move(int step) {
        int n = found().size();
        if (n > 0) selected = Math.floorMod(selected + step, n);
    }

    // The clue chosen to read again, or null if none has been found
    public Clue chosen() {
        List<Clue> found = found();
        if (found.isEmpty()) return null;
        return found.get(Math.min(selected, found.size() - 1));
    }

    // The clues found so far, in the order the journal lists them
    private List<Clue> found() {
        List<Clue> found = new ArrayList<>();
        for (Floor f = gp.world.top; f != null; f = f.below) {
            if (f.quest == null) continue;
            for (Clue c : f.quest.clues) {
                if (gp.flor.clues.contains(c.id)) found.add(c);
            }
        }
        return found;
    }

    public void draw(Graphics2D g2d) {
        if (!open) return;
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        int x = 90, y = 30, w = gp.screenWidth - 180, h = gp.screenHeight - 60;
        g.setColor(PAPER);
        g.fillRect(x, y, w, h);
        g.setColor(PAPER_LINE);
        for (int ly = y + 70; ly < y + h - 10; ly += 22) g.drawLine(x, ly + 5, x + w, ly + 5);
        g.setColor(new Color(190, 60, 60));
        g.drawLine(x + 44, y, x + 44, y + h);
        g.setColor(INK);
        g.drawRect(x, y, w, h);

        g.setFont(TITLE);
        g.drawString("Journal", x + 60, y + 46);
        Director director = gp.director;
        g.setFont(SMALL);
        g.setColor(FADED);
        String count = "Clues found: " + director.cluesFound() + " of " + director.cluesInAll();
        g.drawString(count, x + w - 20 - g.getFontMetrics().stringWidth(count), y + 44);

        Clue chosen = chosen();
        int ly = y + 92;
        World world = gp.world;
        for (Floor f = world.top; f != null; f = f.below) {
            Quest q = f.quest;
            if (q == null) continue;
            boolean reached = director.seen("floor" + f.number);
            g.setFont(HEADING);
            String heading = "Floor " + f.number + ":  ???";   // Not been there yet: faded, no name
            g.setColor(FADED);
            if (reached) {
                heading = "Floor " + f.number + ":  " + q.title + "   " + q.found(gp.flor.clues) + "/" + q.clues.size();
                g.setColor(INK);
            }
            g.drawString(heading, x + 60, ly);
            ly += 22;
            g.setFont(ENTRY);
            for (Clue c : q.clues) {
                boolean has = gp.flor.clues.contains(c.id);
                if (has && c == chosen) {
                    g.setColor(BLOOD);
                    g.drawString(">", x + 62, ly);
                }
                if (has) {
                    g.setColor(INK);
                    g.drawString(c.title + "  (" + c.room.name + ")", x + 80, ly);
                } else {
                    g.setColor(FADED);
                    g.drawString("???", x + 80, ly);
                }
                ly += 22;
            }
        }
        if (director.seen("outside")) {
            g.setFont(HEADING);
            g.setColor(INK);
            g.drawString("Outside:  The Quadrangle", x + 60, ly);
            ly += 22;
            g.setFont(ENTRY);
            SubjectZero zero = world.finalBoss;
            String status = "Trees still standing: " + world.treesLeft() + " of " + world.trees().size();
            if (zero != null && zero.awake()) status = "Every tree is down. Subject Zero is awake.";
            if (zero != null && zero.defeated) status = "Subject Zero is gone.";
            g.drawString(status, x + 80, ly);
        }

        g.setFont(SMALL);
        g.setColor(FADED);
        String keys = "W / S  choose     E  read again     J or Esc  close";
        if (chosen == null) keys = "J or Esc  close";      // No clues yet: nothing to choose
        g.drawString(keys, x + 60, y + h - 14);
        g.dispose();
    }
}
