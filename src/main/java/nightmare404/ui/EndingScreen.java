package nightmare404.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import nightmare404.main.GamePanel;
import nightmare404.story.Story;

// The last screen, after Subject Zero: fades to black, then one word (REMEMBERED or FORGOTTEN),
// how many clues were found, and how to play again.
public class EndingScreen {
    private static final Font BIG = new Font(Font.SERIF, Font.BOLD, 64);
    private static final Font TEXT = new Font(Font.SERIF, Font.PLAIN, 20);
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private static final Color PALE = new Color(206, 200, 184);
    private static final Color GOLD = new Color(232, 184, 70);
    private static final Color BLOOD = new Color(170, 18, 22);

    private final GamePanel gp;

    public EndingScreen(GamePanel gp) {
        this.gp = gp;
    }

    // ticks = frames since the ending began
    public void draw(Graphics2D g2d, boolean remembered, long ticks, int found, int total) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        float fade = Math.min(1f, ticks / 90f);
        g.setComposite(AlphaComposite.SrcOver.derive(fade));
        g.setColor(remembered ? new Color(30, 24, 14) : Color.BLACK);
        g.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        if (ticks > 60) {
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, (ticks - 60) / 60f)));
            centred(g, BIG, remembered ? GOLD : BLOOD, Story.endingTitle(remembered), 250);
            centred(g, TEXT, PALE, Story.endingHint(remembered), 310);
            centred(g, TEXT, PALE, "Clues found: " + found + " of " + total, 350);
            if (ticks > 150 && (ticks / 30) % 2 == 0) {
                centred(g, SMALL, PALE, "Press Enter to play again", 440);
            }
        }
        g.dispose();
    }

    private void centred(Graphics2D g, Font font, Color color, String text, int y) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (gp.screenWidth - fm.stringWidth(text)) / 2, y);
    }
}
