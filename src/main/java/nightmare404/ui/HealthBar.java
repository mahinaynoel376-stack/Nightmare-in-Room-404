package nightmare404.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

// Every health bar in the game, drawn from the pixel-art frames in src/main/resources/ui:
//   hp_lead_1.png ... hp_lead_7.png    the player's: a heart that drains along with the bar
//   hp_enemy_1.png ... hp_enemy_7.png  every monster's: a skull that fades as the bar empties
// Frame 1 is full health and frame 7 is empty; the five in between each take away one more of the
// bar's six segments. Any health left at all shows at least one segment, so only 0 shows empty.
//
// Each frame is 64 x 32 with empty space around the bar. It's cut down to the part that's drawn (the
// same box for all seven frames, so the bar doesn't shift as it empties) and scaled up by a whole
// number, with no smoothing, so the pixels stay sharp.
public final class HealthBar {
    public enum Style {
        LEAD("hp_lead"),     // The player
        ENEMY("hp_enemy");   // The monsters

        private final String file;
        private BufferedImage[] frames;   // Loaded the first time this style is drawn
        private Rectangle box;            // The part of the 64 x 32 frames that has anything in it

        Style(String file) {
            this.file = file;
        }
    }

    public static final int FRAMES = 7;
    private static final Font NUMBER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);

    private HealthBar() { }

    // The size of the bar on screen at this scale
    public static int width(Style style, int scale) {
        return box(style).width * scale;
    }

    public static int height(Style style, int scale) {
        return box(style).height * scale;
    }

    // Which frame shows this much health: 0 (the first, full) to 6 (the last, empty)
    public static int frame(double hp, double max) {
        if (max <= 0 || hp <= 0) return FRAMES - 1;
        int segments = (int) Math.ceil(Math.min(1, hp / max) * (FRAMES - 1));   // 1 to 6
        return FRAMES - 1 - segments;
    }

    // Draws the bar for `hp` out of `max` with its top-left corner at (x, y)
    public static void draw(Graphics2D g, Style style, double hp, double max, int x, int y, int scale) {
        draw(g, style, hp, max, x, y, scale, 1f);
    }

    // The same, see-through: alpha 0 (invisible) to 1 (solid)
    public static void draw(Graphics2D g, Style style, double hp, double max, int x, int y, int scale, float alpha) {
        load(style);
        Rectangle box = style.box;
        int w = box.width * scale, h = box.height * scale;
        Graphics2D g2 = (Graphics2D) g.create();
        if (alpha < 1f) g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alpha)));
        BufferedImage img = style.frames[frame(hp, max)];
        if (img != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(img, x, y, w, h, null);
        } else {                                  // The picture is missing: a plain bar instead
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, w, h);
            g2.setColor(style == Style.LEAD ? new Color(160, 14, 16) : new Color(120, 26, 140));
            g2.fillRect(x + scale, y + scale, (int) Math.round((w - 2 * scale) * Math.max(0, Math.min(1, hp / max))),
                    h - 2 * scale);
        }
        g2.dispose();
    }

    // The bar with its numbers beside it ("73 / 100"), for the battle screens: the numbers go to the
    // right of the bar, or to its left when the bar sits against the right edge (the monster's).
    // The bar shows `shown`, which can be part way between two numbers while it slides to `hp`.
    public static void drawWithNumbers(Graphics2D g, Style style, double shown, int hp, int max, int x, int y,
                                       int scale, boolean numbersLeft) {
        draw(g, style, shown, max, x, y, scale);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(NUMBER_FONT);
        FontMetrics fm = g2.getFontMetrics();
        String s = Math.max(0, hp) + " / " + max;
        int gap = 2 * scale + 4;
        int tx = numbersLeft ? x - gap - fm.stringWidth(s) : x + width(style, scale) + gap;
        int ty = y + (height(style, scale) + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 220));     // A dark outline, so the number reads over anything
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) if (ox != 0 || oy != 0) g2.drawString(s, tx + ox, ty + oy);
        }
        g2.setColor(Color.WHITE);
        g2.drawString(s, tx, ty);
        g2.dispose();
    }

    private static Rectangle box(Style style) {
        load(style);
        return style.box;
    }

    // Reads the seven frames once, and finds the box around everything drawn in any of them
    private static synchronized void load(Style style) {
        if (style.frames != null) return;
        BufferedImage[] full = new BufferedImage[FRAMES];
        Rectangle box = null;
        for (int i = 0; i < FRAMES; i++) {
            String path = "/ui/" + style.file + "_" + (i + 1) + ".png";
            try (InputStream in = HealthBar.class.getResourceAsStream(path)) {
                if (in == null) System.err.println("Could not find " + path);
                else full[i] = ImageIO.read(in);
            } catch (IOException e) {
                System.err.println("Could not load " + path + ": " + e.getMessage());
            }
            if (full[i] != null) {
                Rectangle drawn = drawnPart(full[i]);
                if (drawn != null) box = box == null ? drawn : box.union(drawn);
            }
        }
        if (box == null) box = new Rectangle(0, 0, 56, 10);   // Nothing loaded: the plain bar's size
        BufferedImage[] frames = new BufferedImage[FRAMES];
        for (int i = 0; i < FRAMES; i++) {
            if (full[i] != null) frames[i] = full[i].getSubimage(box.x, box.y, box.width, box.height);
        }
        style.box = box;
        style.frames = frames;
    }

    // The smallest box around the pixels that aren't see-through (null if there are none)
    private static Rectangle drawnPart(BufferedImage img) {
        int left = img.getWidth(), top = img.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) continue;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        return right < 0 ? null : new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }
}
