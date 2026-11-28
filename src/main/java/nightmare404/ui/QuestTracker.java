package nightmare404.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import nightmare404.entity.Clue;
import nightmare404.entity.SubjectZero;
import nightmare404.main.GamePanel;
import nightmare404.story.Quest;
import nightmare404.world.Floor;
import nightmare404.world.World;

// The objective, as a note taped up in the top-right corner: a scrap of lined paper, a little crooked,
// written in pen. On it: this floor's quest, a checklist of its clues (ticked off in red as they're
// found, "???" until then), how much weaker the boss will be, and how many clues Kai has found in the
// whole school (all of them is the good ending). Outside, it's the trees still standing instead.
public class QuestTracker {
    private static final Font TITLE = new Font(Font.SERIF, Font.BOLD, 14);
    private static final Font HAND = new Font(Font.SERIF, Font.ITALIC, 13);
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Color PAPER = new Color(232, 224, 200);
    private static final Color PAPER_EDGE = new Color(150, 136, 110);
    private static final Color RULE = new Color(150, 176, 206);     // The ruled lines
    private static final Color MARGIN = new Color(206, 110, 110);   // The red margin line
    private static final Color TAPE = new Color(222, 214, 170, 170);
    private static final Color INK = new Color(30, 40, 92);         // Blue pen, like the name on the ID card
    private static final Color RED_PEN = new Color(176, 24, 24);
    private static final Color FADED = new Color(120, 110, 96);

    private static final int WIDTH = 214, LINE = 17, TOP = 44;      // Line spacing, and where the first line is

    private final GamePanel gp;

    public QuestTracker(GamePanel gp) {
        this.gp = gp;
    }

    // One line on the note: its text, and its checkbox (null: no box; true: ticked)
    private record Row(String text, Boolean ticked) {}

    public void draw(Graphics2D g2d) {
        String title;
        List<Row> rows = new ArrayList<>();
        World world = gp.world;
        if (gp.currentRoom.outdoors) {
            SubjectZero zero = world.finalBoss;
            title = "The Quadrangle";
            int trees = world.trees().size(), left = world.treesLeft();
            rows.add(new Row("Fell the trees: " + (trees - left) + " of " + trees, left == 0));
            if (left == 0) rows.add(new Row("Beat Subject Zero", zero != null && zero.defeated));
            if (gp.dawn.on()) rows.add(new Row("Walk out the south gate", false));
        } else {
            Floor floor = gp.currentRoom.floor;
            Quest quest = floor.quest;
            if (quest == null) return;
            title = quest.title;
            for (Clue c : quest.clues) {
                boolean found = gp.kai.clues.contains(c.id);
                rows.add(new Row(found ? c.title : "???", found));
            }
            if (floor.boss != null && !floor.boss.defeated && quest.found(gp.kai.clues) > 0) {
                rows.add(new Row(floor.boss.enemy.name() + ": " + Math.round(quest.weakening(gp.kai.clues) * 100)
                        + "% weaker", null));
            }
        }
        String total = "Clues found: " + gp.director.cluesFound() + " / " + gp.director.cluesInAll();

        int h = TOP + rows.size() * LINE + 30;
        int x = gp.screenWidth - WIDTH - 16, y = 10;
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.rotate(Math.toRadians(1.5), x + WIDTH / 2.0, y + h / 2.0);     // Taped up a little crooked

        // The paper: a shadow, the sheet with its corner folded down, ruled lines and a red margin
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRect(x + 4, y + 4, WIDTH, h);
        int[] px = {x, x + WIDTH, x + WIDTH, x + WIDTH - 14, x};
        int[] py = {y, y, y + h - 14, y + h, y + h};
        g.setColor(PAPER);
        g.fillPolygon(px, py, 5);
        g.setColor(PAPER_EDGE);
        g.drawPolygon(px, py, 5);
        g.setColor(new Color(204, 194, 166));                            // The folded corner
        g.fillPolygon(new int[] {x + WIDTH, x + WIDTH - 14, x + WIDTH - 14}, new int[] {y + h - 14, y + h, y + h - 14}, 3);
        g.setColor(RULE);
        for (int ly = y + TOP + 4; ly < y + h - 6; ly += LINE) g.drawLine(x + 4, ly, x + WIDTH - 4, ly);
        g.setColor(MARGIN);
        g.drawLine(x + 22, y + 2, x + 22, y + h - 2);

        // A strip of tape holding it up
        g.setColor(TAPE);
        g.fillRect(x + WIDTH / 2 - 26, y - 6, 52, 13);

        // The quest, underlined
        g.setFont(TITLE);
        g.setColor(INK);
        g.drawString(title, x + 28, y + 20);
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(x + 28, y + 23, x + 28 + g.getFontMetrics().stringWidth(title), y + 23);

        // The checklist
        g.setFont(HAND);
        int ly = y + TOP + 1;
        for (Row row : rows) {
            int tx = x + 28;
            if (row.ticked() != null) {
                g.setColor(INK);
                g.drawRect(tx, ly - 10, 9, 9);
                if (row.ticked()) {                                      // A red tick
                    g.setColor(RED_PEN);
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine(tx + 1, ly - 5, tx + 4, ly - 1);
                    g.drawLine(tx + 4, ly - 1, tx + 11, ly - 12);
                    g.setStroke(new BasicStroke(1.2f));
                }
                tx += 15;
            }
            g.setColor(row.ticked() == null ? RED_PEN : row.ticked() ? INK : FADED);
            g.drawString(row.text(), tx, ly);
            ly += LINE;
        }

        // The total, and where the journal is
        g.setColor(INK);
        g.drawString(total, x + 28, ly + 2);
        g.setFont(SMALL);
        g.setColor(FADED);
        g.drawString("J  journal", x + WIDTH - 62, y + h - 6);
        g.dispose();
    }
}
