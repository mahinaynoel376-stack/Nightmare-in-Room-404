package nightmare404.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import nightmare404.entity.SubjectZero;
import nightmare404.main.GamePanel;
import nightmare404.story.Quest;
import nightmare404.world.Floor;
import nightmare404.world.World;

// The quest tracker: a small box in the top-right corner saying what this floor's quest is and how far
// along it is ("Pages 1/2 · Brute -20%"). Outside, it counts the trees still standing instead.
public class QuestTracker {
    private static final Font TITLE = new Font(Font.SERIF, Font.BOLD, 14);
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Color BOX = new Color(8, 6, 6, 190);
    private static final Color BORDER = new Color(130, 18, 18);
    private static final Color PALE = new Color(206, 200, 184);
    private static final Color GOLD = new Color(232, 184, 70);

    private final GamePanel gp;

    public QuestTracker(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2d) {
        String title, progress;
        boolean done;
        World world = gp.world;
        if (gp.currentRoom.outdoors) {
            SubjectZero zero = world.finalBoss;
            title = "THE QUADRANGLE";
            done = zero != null && zero.defeated;
            progress = "Trees standing: " + world.treesLeft() + " of " + world.trees().size();
            if (zero != null && zero.awake() && !zero.defeated) progress = "Subject Zero is awake";
        } else {
            Floor floor = gp.currentRoom.floor;
            Quest quest = floor.quest;
            if (quest == null) return;
            title = quest.title.toUpperCase();
            int found = quest.found(gp.flor.clues);
            done = found == quest.clues.size();
            progress = quest.things + " " + found + "/" + quest.clues.size();
            if (floor.boss != null && !floor.boss.defeated && found > 0) {
                progress += "  ·  " + floor.boss.enemy.name() + " -" + Math.round(quest.weakening(gp.flor.clues) * 100) + "%";
            }
        }
        String hint = "J  journal";

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(TITLE);
        FontMetrics tm = g.getFontMetrics();
        g.setFont(SMALL);
        FontMetrics sm = g.getFontMetrics();
        int w = Math.max(tm.stringWidth(title), Math.max(sm.stringWidth(progress), sm.stringWidth(hint))) + 24;
        int h = 58, x = gp.screenWidth - w - 14, y = 12;
        g.setColor(BOX);
        g.fillRect(x, y, w, h);
        if (done) {                      // Done: in gold
            g.setColor(GOLD.darker());
        } else {
            g.setColor(BORDER);
        }
        g.drawRect(x, y, w, h);
        g.setFont(TITLE);
        if (done) {
            g.setColor(GOLD);
        } else {
            g.setColor(PALE);
        }
        g.drawString(title, x + 12, y + 19);
        g.setFont(SMALL);
        g.setColor(PALE);
        g.drawString(progress, x + 12, y + 36);
        g.setColor(new Color(150, 144, 130));
        g.drawString(hint, x + 12, y + 51);
        g.dispose();
    }
}
