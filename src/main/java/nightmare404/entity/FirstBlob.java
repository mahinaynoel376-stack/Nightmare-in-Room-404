package nightmare404.entity;

import java.awt.Color;
import java.awt.Graphics2D;

import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.world.Room;

// The first monster Kai meets: a blob of goo that drops on him from the ceiling the first time he
// steps out of Room 404 (story.Director starts it; Encounters makes it). It isn't one of the
// hallway's monsters: it never walks a round, and once beaten it stays beaten.
//
// drop(player) starts it falling beside the player. While it falls (DROP_TICKS long) the world waits
// (see GamePanel.update): a strand of goo comes down from the ceiling, its shadow grows on the floor,
// it lands with a splat and squashes flat for a moment. Then it has him: its battle starts.
// The pictures are made by tools/art/make_ceiling_blob.py.
public class FirstBlob extends Monster {
    public static final int DROP_TICKS = 36;    // 0.6 seconds...
    private static final int FALLING = 24;      // ...falling for the first 24, squashing flat for the rest
    private static final int HEIGHT_ABOVE = 220; // It drops from this far above the floor
    private static final Color GOO = new Color(52, 26, 66);

    private int falling;                        // Ticks into its drop (0 = not dropping)
    private boolean landed;                     // It has landed beside the player (until its battle's over)

    public FirstBlob(EnemyDef enemy, Room room, int ts) {
        super(enemy, room, false, ts, new int[] {8, 8});
    }

    // Drops beside the player (on their right, or their left if the right side is in the way)
    public void drop(Player player) {
        x = player.x + 28;
        y = player.y;
        syncHitbox();
        if (blocked()) {
            x = player.x - 28;
            syncHitbox();
        }
        falling = 1;
        landed = false;
    }

    public boolean falling() {
        return falling > 0;
    }

    @Override
    public void reset() {
        super.reset();
        falling = 0;
        landed = false;
    }

    // It doesn't walk anywhere: dropping just takes time
    @Override
    public void update(Player player) {
        if (falling > 0) {
            falling++;
            if (falling > DROP_TICKS) {
                falling = 0;
                landed = true;
            }
        }
    }

    // It never grabs anyone by walking into them: landing starts its battle (see GamePanel.update)
    @Override
    public boolean touches(Player player) {
        return false;
    }

    @Override
    public void draw(Graphics2D g, long ticks) {
        if (defeated || (falling == 0 && !landed)) return;   // Still up on the ceiling (or beaten)
        int cx = x + 16, feet = y + 32;
        double k = falling == 0 ? 1 : Math.min(1, falling / (double) FALLING);

        // Its shadow on the floor, growing as it comes down
        int shadow = (int) (12 + 30 * k);
        g.setColor(new Color(0, 0, 0, (int) (40 + 70 * k)));
        g.fillOval(cx - shadow / 2, feet - 4, shadow, 7);

        if (falling > 0 && falling < FALLING) {
            // Falling, faster and faster, on a strand of goo from the ceiling
            int above = (int) (HEIGHT_ABOVE * (1 - k * k));
            int top = feet - image.getHeight() - above;
            g.setColor(GOO);
            g.fillRect(cx - 1, top - 240, 3, 242);
            g.drawImage(image, cx - image.getWidth() / 2, top, null);
        } else if (falling > 0) {
            // Landed: squashed flat, then springing back, with drops of goo splashed round it
            double squash = 1 - (falling - FALLING) / (double) (DROP_TICKS - FALLING);
            int w = (int) (image.getWidth() * (1 + 0.4 * squash));
            int h = (int) (image.getHeight() * (1 - 0.35 * squash));
            g.drawImage(image, cx - w / 2, feet - h, w, h, null);
            g.setColor(GOO);
            int spread = (int) (14 + 12 * (1 - squash));
            g.fillOval(cx - spread - 3, feet - 6, 5, 4);
            g.fillOval(cx + spread - 2, feet - 5, 5, 4);
            g.fillOval(cx - spread / 2, feet - 12 - (int) (6 * squash), 4, 4);
        } else {
            g.drawImage(image, cx - image.getWidth() / 2, feet - image.getHeight(), null);   // Landed, ready
        }
    }
}
