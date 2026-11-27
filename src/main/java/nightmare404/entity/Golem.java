package nightmare404.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.world.Room;

// The Brute, Floor 4's boss, asleep as a boulder in the middle of the West Stairs (see Encounters).
//
//   ASLEEP  A boulder that fills the stairs down: nobody gets past it. When the player walks up to it,
//           the Director says what's needed: the janitor's two missing pages. With both, Flor reads
//           the logbook out to it, and it starts to wake.
//   WAKING  A short animation (WAKE_TICKS long): the boulder shakes as its cracks glow and its eyes
//           open, then it splits open and the golem stands up out of it.
//   AWAKE   The Brute as it always looks: walking into it starts its battle, like any boss.
//
// Giving up after losing to it puts it back to sleep (Player.respawn calls reset()).
// Its pictures are made by tools/art/make_golem_boulder.py.
public class Golem extends Monster {
    public static final int WAKE_TICKS = 90;    // 1.5 seconds
    private static final int CRACKING = 35;     // Ticks 1-34: the boulder shakes, its cracks glowing
    private static final int SPLITTING = 60;    // Ticks 35-59: split open; 60-90: the golem stands up

    private final BufferedImage boulder, cracked, split;
    private boolean asleep = true;
    private int waking;                         // Ticks into waking up (0 = not waking)
    private boolean touchedBefore;              // The player was against the boulder last time we looked

    // The boss standing at (col, row) on the stairs
    public Golem(EnemyDef enemy, Room room, int ts, int col, int row) {
        super(enemy, room, true, ts, new int[] {col, row});
        boulder = picture(enemy, "_boulder");
        cracked = picture(enemy, "_boulder_cracked");
        split = picture(enemy, "_boulder_split");
    }

    private static BufferedImage picture(EnemyDef enemy, String pose) {
        return scaled(BattleScreen.Sprites.get(enemy.sprite() + pose, true), BOSS_HEIGHT);
    }

    public boolean asleep() {
        return asleep;
    }

    public boolean waking() {
        return waking > 0;
    }

    // Flor has read the logbook out to it: it starts to wake
    public void wake() {
        if (asleep) {
            asleep = false;
            waking = 1;
        }
    }

    // Back on its spot, and (unless it's been beaten) back to sleep as a boulder
    @Override
    public void reset() {
        super.reset();
        if (!defeated) {
            asleep = true;
            waking = 0;
        }
        touchedBefore = false;
    }

    // It never moves; waking up just takes time
    @Override
    public void update(Player player) {
        if (waking > 0) {
            waking++;
            if (waking > WAKE_TICKS) waking = 0;     // Awake: from now on it's the Brute
        }
    }

    // Asleep, walking up to it counts once (until the player steps away from it again). While it's
    // waking up it can't grab anyone. Awake, it's like any boss: touching it starts the battle.
    @Override
    public boolean touches(Player player) {
        if (waking > 0) return false;
        boolean touching = super.touches(player);
        boolean walkedUp = touching && !touchedBefore;
        touchedBefore = touching;
        if (asleep) return walkedUp;
        return touching;
    }

    // Asleep (and while it wakes) it fills the stairs: the player can't walk through it
    @Override
    public boolean blocks(Rectangle box) {
        return !defeated && (asleep || waking > 0) && body().intersects(box);
    }

    @Override
    public void draw(Graphics2D g, long ticks) {
        if (defeated || (!asleep && waking == 0)) {
            super.draw(g, ticks);                     // The Brute (or, once beaten, the key it dropped)
            return;
        }
        int cx = x + 16, feet = y + 32;
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(cx - 30, feet - 6, 60, 9);

        BufferedImage picture = boulder;
        int dx = 0, dy = 0;
        if (asleep) {
            if ((ticks + phase) % 240 < 20) dy = -1;  // Every few seconds it seems to breathe
        } else if (waking < CRACKING) {
            picture = cracked;                        // Shaking, harder and harder
            int shake = 1 + waking / 12;
            dx = (waking / 2) % 2 == 0 ? shake : -shake;
        } else if (waking < SPLITTING) {
            picture = split;
        } else {
            picture = image;                          // The golem, standing up with a little hop
            double t = (waking - SPLITTING) / (double) (WAKE_TICKS - SPLITTING);
            dy = -(int) Math.round(Math.sin(Math.PI * t) * 6);
        }
        g.drawImage(picture, cx - picture.getWidth() / 2 + dx, feet - picture.getHeight() + dy, null);

        if (waking >= CRACKING) drawRubble(g, cx, feet, waking - CRACKING);
    }

    // Bits of the boulder flying out as it splits, and falling back to the stairs
    private void drawRubble(Graphics2D g, int cx, int feet, int t) {
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * (0.1 + 0.8 * ((i * 37) % 100) / 100.0);
            double speed = 1.8 + (i % 4) * 0.6;
            double vx = Math.cos(angle) * speed * (i % 2 == 0 ? 1 : -1);
            double vy = -Math.sin(angle) * speed;
            int px = (int) (cx + vx * t);
            int py = (int) (feet - 24 + vy * t + 0.15 * t * t);
            if (py > feet) continue;                  // Landed
            g.setColor(i % 3 == 0 ? new Color(69, 67, 66) : new Color(188, 179, 158));
            g.fillRect(px, py, 3, 3);
        }
    }
}
