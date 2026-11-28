package nightmare404.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.world.Room;

// A boss that sleeps until something wakes it (see Encounters):
//   the Brute, asleep as a boulder in the middle of Floor 4's West Stairs ("_boulder" pictures), and
//   Test Subject X, still sealed in the last pod in the Containment Lab ("_pod" pictures).
//
//   ASLEEP  It's in the way: the player can't walk through it. When the player walks up to it, the
//           Director decides what happens. The Brute only wakes once Kai has found both of the
//           janitor's pages and reads them out to it (needsClues); Test Subject X wakes as soon as
//           anyone comes near its pod.
//   WAKING  A short animation (WAKE_TICKS long), in three pictures made for it: it shakes and cracks,
//           splits open (the boulder breaks in two, the pod's glass shatters), and it comes out.
//   AWAKE   The boss as it always looks: walking into it starts its battle, like any boss.
//
// Giving up after losing to it puts it back to sleep (Player.respawn calls reset()).
// Its pictures are made by tools/art/make_golem_boulder.py and tools/art/make_specimen_pod.py.
public class SleepingBoss extends Monster {
    public static final int WAKE_TICKS = 90;    // 1.5 seconds
    private static final int CRACKING = 35;     // Ticks 1-34: it shakes, cracking
    private static final int SPLITTING = 60;    // Ticks 35-59: split open; 60-90: it comes out

    public final boolean needsClues;            // True: it only wakes once the floor's clues are all found
    private final BufferedImage asleepPicture, crackedPicture, splitPicture;
    private final Color bits, darkBits;         // What flies out as it splits: stone, or glass
    private boolean asleep = true;
    private int waking;                         // Ticks into waking up (0 = not waking)
    private boolean touchedBefore;              // The player was up against it last time we looked

    // The boss standing at (col, row). look: which pictures it sleeps in ("_boulder" or "_pod": its
    // sprite's name + look, + look + "_cracked", + look + "_split").
    public SleepingBoss(EnemyDef enemy, Room room, int ts, int col, int row, String look, boolean needsClues) {
        super(enemy, room, true, ts, new int[] {col, row});
        this.needsClues = needsClues;
        asleepPicture = picture(enemy, look);
        crackedPicture = picture(enemy, look + "_cracked");
        splitPicture = picture(enemy, look + "_split");
        if (look.equals("_pod")) {
            bits = new Color(150, 236, 204);    // Glass
            darkBits = new Color(58, 190, 148);  // ...and the green liquid
        } else {
            bits = new Color(188, 179, 158);    // Stone
            darkBits = new Color(69, 67, 66);
        }
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

    // Something has woken it: it starts to come out
    public void wake() {
        if (asleep) {
            asleep = false;
            waking = 1;
        }
    }

    // Back on its spot, and (unless it's been beaten) back to sleep
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
            if (waking > WAKE_TICKS) waking = 0;     // Awake: from now on it's the boss as it always is
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

    // Asleep (and while it wakes) it's in the way: the player can't walk through it
    @Override
    public boolean blocks(Rectangle box) {
        return !defeated && (asleep || waking > 0) && body().intersects(box);
    }

    @Override
    public void draw(Graphics2D g, long ticks) {
        if (defeated || (!asleep && waking == 0)) {
            super.draw(g, ticks);                     // The boss (or, once beaten, what it dropped)
            return;
        }
        int cx = x + 16, feet = y + 32;
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(cx - 30, feet - 6, 60, 9);

        BufferedImage picture = asleepPicture;
        int dx = 0, dy = 0;
        if (asleep) {
            if ((ticks + phase) % 240 < 20) dy = -1;  // Every few seconds it seems to breathe
        } else if (waking < CRACKING) {
            picture = crackedPicture;                 // Shaking, harder and harder
            int shake = 1 + waking / 12;
            dx = (waking / 2) % 2 == 0 ? shake : -shake;
        } else if (waking < SPLITTING) {
            picture = splitPicture;
        } else {
            picture = image;                          // Out, with a little hop
            double t = (waking - SPLITTING) / (double) (WAKE_TICKS - SPLITTING);
            dy = -(int) Math.round(Math.sin(Math.PI * t) * 6);
        }
        g.drawImage(picture, cx - picture.getWidth() / 2 + dx, feet - picture.getHeight() + dy, null);

        if (waking >= CRACKING) drawBits(g, cx, feet, waking - CRACKING);
    }

    // Bits of the boulder (or the pod's glass) flying out as it splits, and falling back down
    private void drawBits(Graphics2D g, int cx, int feet, int t) {
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * (0.1 + 0.8 * ((i * 37) % 100) / 100.0);
            double speed = 1.8 + (i % 4) * 0.6;
            double vx = Math.cos(angle) * speed * (i % 2 == 0 ? 1 : -1);
            double vy = -Math.sin(angle) * speed;
            int px = (int) (cx + vx * t);
            int py = (int) (feet - 24 + vy * t + 0.15 * t * t);
            if (py > feet) continue;                  // Landed
            g.setColor(i % 3 == 0 ? darkBits : bits);
            g.fillRect(px, py, 3, 3);
        }
    }
}
