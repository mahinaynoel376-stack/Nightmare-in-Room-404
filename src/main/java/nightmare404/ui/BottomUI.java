package nightmare404.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import nightmare404.battle.BattleScreen;
import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.story.Line;

// The bottom UI: Flor's student ID card (bottom left) and her backpack (bottom right).
//
//   ┌──────────────────────────┐
//   │ ┌────┐ STUDENT ID  #404  │                 ┌─ BACKPACK ─────────────────┐
//   │ │face│ NAME: Flor        │                 │  + ×3  📕 ×1  🖊 ×1  🎫 ×1   │
//   │ └────┘ ♥▰▰▰▰▰▰  100      │                 └────────────────────────────┘
//   └──────────────────────────┘
//
// Both sit on the dark strip in the last tile row (row 18, y 576-607), which the world never covers
// because every map is 18 rows tall. They're taller than that row, so they stick up over the bottom
// of the world a little; top() says how high, so the dialogue box and notices sit above them.
//
// When Flor walks behind the card or the backpack, it fades so she (and any door there) can be seen.
//
// The health bar is a 7-frame sprite (src/main/resources/ui/hp_player_1..7.png): frame 1 is all six
// segments full, frame 7 is empty. The photo on the card gets more beaten up as the segments go.
public class BottomUI {
    // The dark strip along the bottom
    private static final Color STRIP = new Color(14, 12, 14);
    private static final Color STRIP_EDGE = new Color(60, 22, 22);

    // The ID card
    private static final int CARD_X = 12, CARD_W = 232, CARD_H = 76;
    private static final int PHOTO_W = 52, PHOTO_H = 56;
    private static final Color PAPER = new Color(226, 218, 196);
    private static final Color PAPER_EDGE = new Color(120, 106, 84);
    private static final Color INK = new Color(40, 30, 26);
    private static final Color FADED_INK = new Color(110, 98, 82);
    private static final Color SCHOOL_RED = new Color(130, 18, 18);
    private static final Color PHOTO_BACK = new Color(146, 160, 178);
    private static final Font ID_TITLE = new Font(Font.SERIF, Font.BOLD, 13);
    private static final Font ID_NUMBER = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font ID_LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    private static final Font ID_NAME = new Font(Font.SERIF, Font.ITALIC, 16);
    private static final Font ID_HP = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    // The health bar sprite: 64x32, but the drawing is only the 56x10 strip at (2, 4). Drawn twice the size.
    private static final int HP_FRAMES = 7, HP_SEGMENTS = 6;
    private static final int BAR_W = 56 * 2, BAR_H = 10 * 2;

    // The backpack
    private static final int PACK_H = 44, SLOT_W = 54, PACK_MARGIN = 12;
    private static final Color CANVAS = new Color(56, 42, 32);
    private static final Color CANVAS_DARK = new Color(24, 16, 12);
    private static final Color STITCH = new Color(122, 94, 66);
    private static final Color FLAP = new Color(80, 60, 44);
    private static final Color PALE = new Color(206, 200, 184);
    private static final Color GOLD = new Color(232, 184, 70);
    private static final Font PACK_TITLE = new Font(Font.SERIF, Font.BOLD, 11);
    private static final Font COUNT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font HOTKEY = new Font(Font.SANS_SERIF, Font.BOLD, 9);
    private static final float BEHIND_ALPHA = 0.3f;   // How see-through a panel gets with Flor behind it

    private final GamePanel gp;
    private final BufferedImage[] hpFrames = new BufferedImage[HP_FRAMES];
    private final BufferedImage face;

    public BottomUI(GamePanel gp) {
        this.gp = gp;
        for (int i = 0; i < HP_FRAMES; i++) {
            BufferedImage sheet = BattleScreen.Sprites.get("/ui/hp_player_" + (i + 1), true);
            if (sheet != null) hpFrames[i] = sheet.getSubimage(2, 4, 56, 10);
        }
        // The photo: Flor's head from her walking sprite (32x64), the 26x28 square around her face
        BufferedImage body = BattleScreen.Sprites.get(Line.FLOR_FACE, true);
        face = body == null ? null : body.getSubimage(3, 0, 26, 28);
    }

    // The highest point the bottom UI reaches (the top of the ID card). Boxes that sit "just above
    // the bottom bar" (the dialogue box, notices) use this so they don't end up behind the card.
    public int top() {
        return gp.screenHeight - CARD_H - 6;
    }

    public void draw(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int stripTop = gp.screenHeight - gp.tileSize;   // 608 - 32 = 576
        g.setColor(STRIP);
        g.fillRect(0, stripTop, gp.screenWidth, gp.tileSize);
        g.setColor(STRIP_EDGE);
        g.drawLine(0, stripTop, gp.screenWidth, stripTop);

        drawCard(g, gp.flor);
        drawBackpack(g, gp.flor);
        g.dispose();
    }

    // A copy of g to draw one panel with: see-through if Flor's sprite (one tile wide, two tall,
    // standing on her x, y) is behind the panel's x, y, w, h. Dispose it when done.
    private Graphics2D fadeIfBehind(Graphics2D g, int x, int y, int w, int h) {
        Graphics2D panel = (Graphics2D) g.create();
        int size = gp.tileSize;
        Rectangle flor = new Rectangle(gp.flor.x - gp.cameraX(), gp.flor.y - size - gp.cameraY(), size, size * 2);
        if (flor.intersects(x, y, w, h)) panel.setComposite(AlphaComposite.SrcOver.derive(BEHIND_ALPHA));
        return panel;
    }

    // A panel's shadow, fill and 1 px edge
    private static void panel(Graphics2D g, int x, int y, int w, int h, Color fill, Color edge) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(x + 3, y + 3, w, h);
        g.setColor(fill);
        g.fillRect(x, y, w, h);
        g.setColor(edge);
        g.drawRect(x, y, w - 1, h - 1);
    }

    // How many of the bar's six segments are gone: 0 at full health, 6 at none. Rounding the
    // segments left up means any health at all keeps at least one showing.
    private static int segmentsLost(int health, int maxHealth) {
        if (health <= 0) return HP_SEGMENTS;
        int left = (health * HP_SEGMENTS + maxHealth - 1) / maxHealth;
        return HP_SEGMENTS - Math.min(HP_SEGMENTS, left);
    }

    // ------------------------------------------------------------------------------- the ID card

    private void drawCard(Graphics2D g2d, Player player) {
        int x = CARD_X, y = top();
        int lost = segmentsLost(player.health, player.maxHealth);
        boolean critical = lost >= 4;
        Graphics2D g = fadeIfBehind(g2d, x, y, CARD_W, CARD_H);

        // The card, with a band of school red along the bottom
        panel(g, x, y, CARD_W, CARD_H, PAPER, PAPER_EDGE);
        g.setColor(SCHOOL_RED);
        g.fillRect(x + 1, y + CARD_H - 5, CARD_W - 2, 4);
        // Low on health: the card's edge pulses red
        if (critical) {
            int pulse = (int) (110 + 110 * Math.sin(gp.ticks * 0.15));
            g.setColor(new Color(200, 20, 20, Math.max(0, pulse)));
            g.drawRect(x - 1, y - 1, CARD_W + 1, CARD_H + 1);
        }

        // The slot the lanyard clips through
        g.setColor(new Color(60, 52, 44));
        g.fill(new RoundRectangle2D.Double(x + CARD_W / 2.0 + 10, y + 4, 26, 5, 5, 5));

        // The photo
        int px = x + 9, py = y + (CARD_H - 4 - PHOTO_H) / 2;
        drawPhoto(g, px, py, lost);

        // The writing, to the right of the photo
        int tx = px + PHOTO_W + 10, right = x + CARD_W - 9;
        g.setFont(ID_TITLE);
        g.setColor(SCHOOL_RED);
        g.drawString("STUDENT ID", tx, y + 20);
        g.setFont(ID_NUMBER);
        g.setColor(INK);
        String number = "#404";
        g.drawString(number, right - g.getFontMetrics().stringWidth(number), y + 20);
        g.setColor(FADED_INK);
        g.drawLine(tx, y + 25, right, y + 25);

        g.setFont(ID_LABEL);
        g.setColor(FADED_INK);
        g.drawString("NAME:", tx, y + 41);
        int nameX = tx + g.getFontMetrics().stringWidth("NAME:") + 6;
        g.drawLine(nameX - 2, y + 43, right, y + 43);
        g.setFont(ID_NAME);
        g.setColor(new Color(30, 40, 92));          // Written in blue pen
        g.drawString(Line.FLOR, nameX + 2, y + 41);

        // The health bar sprite, and the number beside it
        int hpY = y + 49;
        BufferedImage bar = hpFrames[lost];
        if (bar != null) g.drawImage(bar, tx - 2, hpY, BAR_W, BAR_H, null);
        g.setFont(ID_HP);
        g.setColor(critical ? new Color(176, 20, 22) : INK);
        g.drawString(String.valueOf(Math.max(0, player.health)), tx + BAR_W + 4, hpY + 15);
        g.dispose();
    }

    // Flor's photo. The more of the bar is gone, the worse it looks: scratched, then stained red and
    // flickering, and with no health left it's only static.
    private void drawPhoto(Graphics2D g, int x, int y, int lost) {
        Graphics2D p = (Graphics2D) g.create();
        p.clipRect(x, y, PHOTO_W, PHOTO_H);
        p.setColor(PHOTO_BACK);
        p.fillRect(x, y, PHOTO_W, PHOTO_H);
        p.setColor(new Color(126, 140, 160));      // A darker floor line behind her, like a school photo
        p.fillRect(x, y + PHOTO_H - 14, PHOTO_W, 14);

        if (lost >= HP_SEGMENTS) {
            drawStatic(p, x, y);
        } else {
            // Low health: every so often the photo jumps a pixel, like a bad signal
            int jitter = lost >= 4 && (gp.ticks / 6) % 7 == 0 ? 1 : 0;
            if (face != null) p.drawImage(face, x + jitter, y + 2, PHOTO_W, PHOTO_H, null);
            if (lost >= 2) drawScratches(p, x, y, lost);
            if (lost >= 4) {
                p.setColor(new Color(150, 0, 0, 40 * (lost - 3)));
                p.fillRect(x, y, PHOTO_W, PHOTO_H);
            }
        }
        p.dispose();
        g.setColor(INK);
        g.drawRect(x - 1, y - 1, PHOTO_W + 1, PHOTO_H + 1);
    }

    // Scratches across the photo: two, then four, then six, always in the same places
    private static void drawScratches(Graphics2D g, int x, int y, int lost) {
        int[][] marks = {
                {6, 8, 20, 30}, {34, 6, 46, 22}, {10, 40, 26, 52},
                {38, 30, 48, 50}, {20, 4, 30, 20}, {2, 24, 12, 46}};
        Graphics2D s = (Graphics2D) g.create();
        s.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        s.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int count = Math.min(marks.length, (lost - 1) * 2);
        for (int i = 0; i < count; i++) {
            int[] m = marks[i];
            s.setColor(new Color(120, 10, 10, 200));
            s.drawLine(x + m[0], y + m[1], x + m[2], y + m[3]);
            s.setColor(new Color(240, 230, 210, 120));   // The pale edge of the scratch
            s.drawLine(x + m[0] + 1, y + m[1], x + m[2] + 1, y + m[3]);
        }
        s.dispose();
    }

    // Grey static: 2x2 blocks of random greys, a new pattern every few frames
    private void drawStatic(Graphics2D g, int x, int y) {
        long seed = gp.ticks / 3;
        for (int by = 0; by < PHOTO_H; by += 2) {
            for (int bx = 0; bx < PHOTO_W; bx += 2) {
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                int v = 30 + (int) ((seed >>> 33) % 120);
                g.setColor(new Color(v, v, v));
                g.fillRect(x + bx, y + by, 2, 2);
            }
        }
    }

    // ----------------------------------------------------------------------------- the backpack

    private void drawBackpack(Graphics2D g2d, Player player) {
        String key = gp.currentRoom.floor.exitKey;
        boolean hasKey = key != null && player.keys.contains(key);
        int w = 16 + (hasKey ? 5 : 4) * SLOT_W;
        int x = gp.screenWidth - PACK_MARGIN - w;
        int y = gp.screenHeight - PACK_H - 8;
        Graphics2D g = fadeIfBehind(g2d, x, y - 9, w, PACK_H + 9);   // (The name tab sticks up 9 px)

        // The bag: dark canvas with a stitched seam just inside its edge
        panel(g, x, y, w, PACK_H, CANVAS, CANVAS_DARK);
        Graphics2D seam = (Graphics2D) g.create();
        seam.setColor(STITCH);
        seam.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f));
        seam.drawRect(x + 4, y + 4, w - 9, PACK_H - 9);
        seam.dispose();

        // The name tab, sewn on over the top edge
        g.setFont(PACK_TITLE);
        FontMetrics fm = g.getFontMetrics();
        String title = "BACKPACK";
        int tabW = fm.stringWidth(title) + 16, tabX = x + 12, tabY = y - 9;
        g.setColor(FLAP);
        g.fillRect(tabX, tabY, tabW, 16);
        g.setColor(CANVAS_DARK);
        g.drawRect(tabX, tabY, tabW - 1, 15);
        g.setColor(PALE);
        g.drawString(title, tabX + 8, tabY + 12);

        // The slots, in the order of the 1-4 keys outside
        int[] counts = {player.kits, player.items.textbooks, player.items.redPens, player.items.hallPasses};
        int sx = x + 10, iconY = y + PACK_H / 2 + 3;
        for (int i = 0; i < counts.length; i++) {
            drawSlot(g, i, counts[i], sx + i * SLOT_W, iconY);
        }
        if (hasKey) drawKeySlot(g, sx + 4 * SLOT_W, iconY);
        g.dispose();
    }

    // One item: its picture (the same drawings as the battle screen, at half size) and how many.
    // None left: the picture is faded. Outside, where 1-4 use the items, the key's number is shown too.
    private void drawSlot(Graphics2D g, int which, int count, int x, int cy) {
        Graphics2D icon = (Graphics2D) g.create();
        icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (count <= 0) {   // Faded, on top of however faded the backpack already is
            AlphaComposite bag = (AlphaComposite) g.getComposite();
            icon.setComposite(bag.derive(bag.getAlpha() * 0.3f));
        }
        icon.translate(x + 13, cy);
        icon.scale(0.45, 0.45);
        if (which == 0) BattleScreen.Art.firstAid(icon, 0, 0, true);
        else BattleScreen.Art.item(icon, which - 1, 0, 0);
        icon.dispose();

        g.setFont(COUNT);
        g.setColor(count > 0 ? PALE : new Color(110, 100, 90));
        g.drawString("×" + count, x + 27, cy + 5);

        if (gp.currentRoom.outdoors) {
            g.setFont(HOTKEY);
            g.setColor(CANVAS_DARK);
            g.fillRect(x - 2, cy - 15, 9, 10);
            g.setColor(GOLD);
            g.drawString(String.valueOf(which + 1), x, cy - 7);
        }
    }

    // The key to this floor's way out, once Flor has it
    private static void drawKeySlot(Graphics2D g, int x, int cy) {
        Graphics2D k = (Graphics2D) g.create();
        k.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        k.translate(x + 2, cy);
        k.rotate(Math.toRadians(-20));
        k.setColor(GOLD);
        k.fill(new Ellipse2D.Double(-1, -6, 11, 11));
        k.fillRect(8, -2, 16, 4);
        k.fillRect(17, 1, 3, 5);
        k.fillRect(21, 1, 3, 4);
        k.setColor(CANVAS);
        k.fill(new Ellipse2D.Double(2, -3, 5, 5));
        k.dispose();
        g.setFont(COUNT);
        g.setColor(GOLD);
        g.drawString("KEY", x + 29, cy + 5);
    }
}
