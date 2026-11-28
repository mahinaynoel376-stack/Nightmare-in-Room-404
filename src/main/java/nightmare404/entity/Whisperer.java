package nightmare404.entity;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.world.Room;

// The Whisperer, Floor 3's boss: the head librarian, still reading out the names of the students the
// school forgot so they won't be lost for good (see Encounters). But the names have been blacked out:
// she can't remember them any more, so she just keeps whispering the blanks.
//
//   INTANGIBLE   See-through, drifting at the far end of the library, mumbling. The player walks
//                straight through her: there's no battle. Walking up to her, the Director says what's
//                needed: the three checkout cards, whose names fill in the blanks.
//   SOLIDIFYING  With all three cards, Flor reads the names out to her, and she slowly turns solid
//                (SOLID_TICKS long).
//   SOLID        The Whisperer as she looks in battle: walking into her starts it, like any boss.
//
// Giving up after losing to her makes her intangible again (Player.respawn calls reset()).
// The see-through picture is made by tools/art/make_whisperer_intangible.py.
public class Whisperer extends Monster {
    public static final int SOLID_TICKS = 60;   // 1 second
    private static final Font WHISPER_FONT = new Font(Font.SERIF, Font.ITALIC, 11);
    private static final String[] MUMBLING = {
            "______", "...______, Oct 2...", "don't forget", "______ ______", "...again...", "______, Oct 8"};

    private final BufferedImage faded;
    private boolean intangible = true;
    private int solidifying;                    // Ticks into turning solid (0 = not happening)
    private boolean touchedBefore;              // The player was up against her last time we looked

    // The boss standing at (col, row)
    public Whisperer(EnemyDef enemy, Room room, int ts, int col, int row) {
        super(enemy, room, true, ts, new int[] {col, row});
        faded = scaled(BattleScreen.Sprites.get(enemy.sprite() + "_intangible", true), BOSS_HEIGHT);
    }

    public boolean intangible() {
        return intangible;
    }

    public boolean solidifying() {
        return solidifying > 0;
    }

    // Flor has read her the names: she starts to turn solid
    public void solidify() {
        if (intangible) {
            intangible = false;
            solidifying = 1;
        }
    }

    // Back on her spot, and (unless she's been beaten) intangible again
    @Override
    public void reset() {
        super.reset();
        if (!defeated) {
            intangible = true;
            solidifying = 0;
        }
        touchedBefore = false;
    }

    // She never moves; turning solid just takes time
    @Override
    public void update(Player player) {
        if (solidifying > 0) {
            solidifying++;
            if (solidifying > SOLID_TICKS) solidifying = 0;   // Solid: from now on she can be fought
        }
    }

    // Intangible, walking up to her counts once (until the player steps away again). While she turns
    // solid she can't grab anyone. Solid, she's like any boss: touching her starts the battle.
    @Override
    public boolean touches(Player player) {
        if (solidifying > 0) return false;
        boolean touching = super.touches(player);
        boolean walkedUp = touching && !touchedBefore;
        touchedBefore = touching;
        if (intangible) return walkedUp;
        return touching;
    }

    @Override
    public void draw(Graphics2D g, long ticks) {
        if (defeated || (!intangible && solidifying == 0)) {
            super.draw(g, ticks);                     // Solid (or, once beaten, what she dropped)
            return;
        }
        int cx = x + 16, feet = y + 32;
        int bob = (int) Math.round(Math.sin((ticks + phase) / 20.0) * 3) - 4;   // She floats
        int left = cx - faded.getWidth() / 2, top = feet - faded.getHeight() + bob;
        if (intangible) {
            g.drawImage(faded, left, top, null);
            drawMumbling(g, cx, top, ticks);
            return;
        }
        // Turning solid: the see-through picture fades out as the solid one fades in
        float solid = solidifying / (float) SOLID_TICKS;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(1 - solid));
        g2.drawImage(faded, left, top, null);
        g2.setComposite(AlphaComposite.SrcOver.derive(solid));
        g2.drawImage(image, cx - image.getWidth() / 2, feet - image.getHeight() + bob, null);
        g2.dispose();
    }

    // Bits of what she's whispering, drifting up from her and fading away: mostly blanks
    private void drawMumbling(Graphics2D g, int cx, int top, long ticks) {
        g.setFont(WHISPER_FONT);
        for (int i = 0; i < 3; i++) {
            long cycle = (ticks + phase + i * 40) / 120;          // Each one lasts 2 seconds
            double t = ((ticks + phase + i * 40) % 120) / 120.0;  // 0 to 1 through its 2 seconds
            String words = MUMBLING[(int) ((cycle * 7 + i * 3) % MUMBLING.length)];
            int alpha = (int) (170 * Math.sin(Math.PI * t));
            int wx = cx - 40 + i * 26 - g.getFontMetrics().stringWidth(words) / 4;
            int wy = top - 4 - (int) (t * 26);
            g.setColor(new Color(214, 224, 246, Math.max(0, alpha)));
            g.drawString(words, wx, wy);
        }
    }
}
