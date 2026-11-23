package nightmare404.entity;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.world.Room;

// The final boss: Subject Zero, the first subject of Project REMEMBER, the one that broke the glass of
// pod 2 in the Containment Lab. Its trail leads out to the quadrangle's south gate.
//
// ASLEEP  Until every tree in the quadrangle is beaten it sleeps by the south gate, at the end of the
//         slime trail: a heap of dead leaves and roots, breathing very slowly, easy to walk straight
//         past. It can't be touched or struck.
// AWAKE   When the last tree falls it wakes (see story.Director) and hunts the player, slower than the
//         trees. It's fought on the map like them (see battle.LastStand), with longer words, and it
//         makes the ground erupt with roots around the player.
// Going down to it outside puts it back by the gate, still awake.
public class SubjectZero extends Hunter {
    public static final int HUNT_SPEED = 2;          // Pixels per tick (the trees hunt at 3, the player walks at 4)
    private static final int SIZE = 110;             // How tall it's drawn awake

    private final BufferedImage sleeping, attackImage, hurtImage;
    private boolean awake;
    public int attackPose, hurtPose;                 // Ticks left showing it striking, or hit

    // Asleep at (col, row) on the room's map, shifted half a tile right (between two tiles)
    public SubjectZero(EnemyDef enemy, Room room, int ts, int col, int row) {
        super(enemy, room, true, ts, SIZE, new int[] {col, row});
        nudge(ts / 2);
        sleeping = BattleScreen.Sprites.get(enemy.sprite() + "_sleeping", false);
        attackImage = scaled(BattleScreen.Sprites.get(enemy.sprite() + "_attack", false), SIZE);
        hurtImage = scaled(BattleScreen.Sprites.get(enemy.sprite() + "_hurt", false), SIZE);
    }

    public boolean asleep() {
        return !awake && !defeated;
    }

    public boolean awake() {
        return awake;
    }

    @Override
    public boolean hunting() {
        return awake && !defeated;
    }

    // Every tree is down: it wakes, and from now on it hunts
    public void wake() {
        awake = true;
        chasing = true;
    }

    // Back by the gate (still awake, if it has woken)
    @Override
    public void reset() {
        super.reset();
        resetHunt();
        attackPose = hurtPose = 0;
    }

    // The player has come outside: it starts again from the gate
    @Override
    public void playerArrived(Player player) {
        reset();
        chasing = awake;
    }

    @Override
    public void update(Player player) {
        if (attackPose > 0) attackPose--;
        if (hurtPose > 0) hurtPose--;
        if (!hunting() || player == null) return;
        hunt(player, HUNT_SPEED);
    }

    @Override
    public boolean touches(Player player) {
        return hunting() && super.touches(player);
    }

    @Override
    public void draw(Graphics2D g, long ticks) {
        if (defeated) return;
        int cx = x + 16, feet = y + 32;
        if (!awake) {                                // A heap of leaves, breathing very slowly
            if (sleeping == null) return;
            int h = sleeping.getHeight() + (int) Math.round(1.5 * Math.sin((ticks + phase) / 45.0));
            g.drawImage(sleeping, cx - sleeping.getWidth() / 2, feet - h, sleeping.getWidth(), h, null);
            return;
        }
        BufferedImage pose = hurtPose > 0 ? hurtImage : attackPose > 0 ? attackImage : image;
        int sway = stunned > 0 ? ((stunned / 2) % 2 == 0 ? 3 : -3) : (int) Math.round(Math.sin((ticks + phase) / 18.0) * 2);
        Graphics2D s = (Graphics2D) g.create();
        s.setComposite(AlphaComposite.SrcOver.derive(0.4f));
        s.setColor(Color.BLACK);
        s.fillOval(cx - 40, feet - 8, 80, 14);
        s.dispose();
        g.drawImage(pose, cx - pose.getWidth() / 2 + sway, feet - pose.getHeight(), null);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(new Color(200, 20, 20));
        g.drawString("!", cx - 3, feet - pose.getHeight() - 4);
    }
}
