package nightmare404.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

// Every health bar in the game, drawn from the pixel-art frames in src/main/resources/ui:
//   hp_lead_1.png ... hp_lead_7.png    the player's: a heart that drains along with the bar
//   hp_enemy_1.png ... hp_enemy_7.png  every monster's: a skull that fades as the bar empties
// So there are two health bars, one object each: HealthBar.PLAYER and HealthBar.MONSTER.
//
// Frame 1 is full health and frame 7 is empty; the five in between each take away one more of the
// bar's six segments. Any health left at all shows at least one segment, so only 0 shows empty.
//
// Each frame is 64 x 32 with empty space around the bar. It's cut down to the part that's drawn (the
// same box for all seven frames, so the bar doesn't shift as it empties) and scaled up by a whole
// number, with no smoothing, so the pixels stay sharp.
public class HealthBar {
    public static final int FRAMES = 7;
    private static final Font NUMBER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);

    public static final HealthBar PLAYER = new HealthBar("hp_lead");
    public static final HealthBar MONSTER = new HealthBar("hp_enemy");

    private BufferedImage[] frames = new BufferedImage[FRAMES];   // The seven frames, cut down to the bar
    private int width = 56, height = 10;                          // The bar's size in the pictures

    // Reads the seven pictures whose names start with `file` (in src/main/resources/ui)
    private HealthBar(String file) {
        BufferedImage[] full = new BufferedImage[FRAMES];
        for (int i = 0; i < FRAMES; i++) {
            String path = "/ui/" + file + "_" + (i + 1) + ".png";
            try {
                InputStream in = HealthBar.class.getResourceAsStream(path);
                if (in == null) {
                    System.err.println("Could not find " + path);
                } else {
                    full[i] = ImageIO.read(in);
                    in.close();
                }
            } catch (IOException e) {
                System.err.println("Could not load " + path + ": " + e.getMessage());
            }
        }

        // Find the smallest box around everything drawn in any of the frames
        int left = 64, top = 32, right = -1, bottom = -1;
        for (int i = 0; i < FRAMES; i++) {
            if (full[i] == null) continue;
            for (int y = 0; y < full[i].getHeight(); y++) {
                for (int x = 0; x < full[i].getWidth(); x++) {
                    Color pixel = new Color(full[i].getRGB(x, y), true);
                    if (pixel.getAlpha() == 0) continue;   // See-through: nothing drawn here
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < 0) return;                     // Nothing loaded: a plain bar is drawn instead
        width = right - left + 1;
        height = bottom - top + 1;
        for (int i = 0; i < FRAMES; i++) {
            if (full[i] != null) frames[i] = full[i].getSubimage(left, top, width, height);
        }
    }

    // The size of the bar on screen at this scale
    public int width(int scale) {
        return width * scale;
    }

    public int height(int scale) {
        return height * scale;
    }

    // Which frame shows this much health: 0 (the first, full) to 6 (the last, empty)
    public static int frame(double hp, double max) {
        if (max <= 0 || hp <= 0) return FRAMES - 1;
        double left = Math.min(1, hp / max);
        int segments = (int) Math.ceil(left * (FRAMES - 1));   // 1 to 6
        return FRAMES - 1 - segments;
    }

    // Draws the bar for `hp` out of `max` with its top-left corner at (x, y)
    public void draw(Graphics2D g, double hp, double max, int x, int y, int scale) {
        draw(g, hp, max, x, y, scale, 1f);
    }

    // The same, see-through: alpha 0 (invisible) to 1 (solid)
    public void draw(Graphics2D g, double hp, double max, int x, int y, int scale, float alpha) {
        int w = width * scale;
        int h = height * scale;
        Graphics2D g2 = (Graphics2D) g.create();
        if (alpha < 1f) g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alpha)));
        BufferedImage img = frames[frame(hp, max)];
        if (img != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(img, x, y, w, h, null);
        } else {                                  // The picture is missing: a plain red bar instead
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, w, h);
            double left = Math.max(0, Math.min(1, hp / max));
            g2.setColor(new Color(160, 14, 16));
            g2.fillRect(x + scale, y + scale, (int) Math.round((w - 2 * scale) * left), h - 2 * scale);
        }
        g2.dispose();
    }

    // The bar with its numbers beside it ("73 / 100"), for the battle screens: the numbers go to the
    // right of the bar, or to its left when the bar sits against the right edge (the monster's).
    // The bar shows `shown`, which can be part way between two numbers while it slides to `hp`.
    public void drawWithNumbers(Graphics2D g, double shown, int hp, int max, int x, int y, int scale,
                                boolean numbersLeft) {
        draw(g, shown, max, x, y, scale);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(NUMBER_FONT);
        FontMetrics fm = g2.getFontMetrics();
        String s = Math.max(0, hp) + " / " + max;
        int gap = 2 * scale + 4;
        int tx;
        if (numbersLeft) {
            tx = x - gap - fm.stringWidth(s);
        } else {
            tx = x + width(scale) + gap;
        }
        int ty = y + (height(scale) + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 220));     // A dark outline, so the number reads over anything
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                if (ox != 0 || oy != 0) g2.drawString(s, tx + ox, ty + oy);
            }
        }
        g2.setColor(Color.WHITE);
        g2.drawString(s, tx, ty);
        g2.dispose();
    }
}
