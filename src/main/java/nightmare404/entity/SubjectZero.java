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
// RISING  When the last tree falls, the camera goes over to it and it rises out of its heap (see
//         story.Director and GamePanel.watch): the heap shakes, swells, an eye opens, it pushes up out
//         of the leaves and stands (the rise_1..3 frames, made by tools/art/make_zero_rising.py).
// AWAKE   Then it wakes and hunts the player, slower than the
//         trees. It's fought on the map like them (see battle.LastStand), with longer words, and it
//         makes the ground erupt with roots around the player.
// Going down to it outside puts it back by the gate, still awake.
public class SubjectZero extends Hunter {
    public static final double HUNT_SPEED = 2.5;     // Pixels per tick (the trees hunt at 3, the player walks at 3)...
    public static final double CALM_SPEED = 1.5;     // ...or this, if the player has its brother's tentacle:

    // What Test Subject X leaves when it's beaten (see Encounters): one of its tentacles, with its DNA in
    // it. Subject Zero is X's brother; with it, Subject Zero half remembers him, and calms down: it hunts
    // at CALM_SPEED, and its roots come less often (see LastStand.rootEvery).
    public static final String CALMING_ITEM = "Subject X's tentacle";
    private static final int SIZE = 110;             // How tall it's drawn awake
    public static final int RISE_TICKS = 150;        // How long it takes to rise out of its heap (2.5 seconds)

    private final BufferedImage sleeping, attackImage, hurtImage;
    private final BufferedImage[] riseImages = new BufferedImage[3];
    private boolean awake;
    private int rising = -1;                         // Frames into its rising (-1: it hasn't started)
    public int attackPose, hurtPose;                 // Ticks left showing it striking, or hit

    // Asleep at (col, row) on the room's map, shifted half a tile right (between two tiles)
    public SubjectZero(EnemyDef enemy, Room room, int ts, int col, int row) {
        super(enemy, room, true, ts, SIZE, new int[] {col, row});
        nudge(ts / 2);
        sleeping = BattleScreen.Sprites.get(enemy.sprite() + "_sleeping", false);
        attackImage = scaled(BattleScreen.Sprites.get(enemy.sprite() + "_attack", false), SIZE);
        hurtImage = scaled(BattleScreen.Sprites.get(enemy.sprite() + "_hurt", false), SIZE);
        for (int i = 0; i < riseImages.length; i++) {
            riseImages[i] = scaled(BattleScreen.Sprites.get(enemy.sprite() + "_rise_" + (i + 1), false), SIZE);
        }
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

    // Every tree is down: it starts to rise out of its heap (see updateRise)
    public void rise() {
        rising = 0;
    }

    // True while it's rising (not awake yet)
    public boolean rising() {
        return rising >= 0 && !awake;
    }

    // One frame of rising; at the end it's awake
    public void updateRise() {
        rising++;
        if (rising >= RISE_TICKS) wake();
    }

    // It's up: from now on it hunts
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

    private double stride;                           // Part-pixels carried from tick to tick (to move 2.5 a tick)

    // How fast it hunts this player: slower if they have its brother's tentacle
    public static double speed(Player player) {
        return player.keys.contains(CALMING_ITEM) ? CALM_SPEED : HUNT_SPEED;
    }

    @Override
    public void update(Player player) {
        if (attackPose > 0) attackPose--;
        if (hurtPose > 0) hurtPose--;
        if (!hunting() || player == null) return;
        stride += speed(player);                     // 2.5: 2 pixels one tick, 3 the next
        int pixels = (int) stride;
        stride -= pixels;
        hunt(player, pixels);
    }

    @Override
    public boolean touches(Player player) {
        return hunting() && super.touches(player);
    }

    @Override
    public void draw(Graphics2D g, long ticks) {
        if (defeated) return;
        int cx = x + 16, feet = y + 32;
        if (!awake && rising >= 50) {                // Rising: up out of the leaves, frame by frame
            BufferedImage pose = rising < 75 ? riseImages[0] : rising < 100 ? riseImages[1]
                    : rising < 125 ? riseImages[2] : image;
            int shake = rising % 4 < 2 ? 2 : -2;
            if (pose != null) g.drawImage(pose, cx - pose.getWidth() / 2 + shake, feet - pose.getHeight(), null);
            return;
        }
        if (!awake) {                                // A heap of leaves, breathing very slowly
            if (sleeping == null) return;
            int h = sleeping.getHeight() + (int) Math.round(1.5 * Math.sin((ticks + phase) / 45.0));
            int shake = rising < 0 ? 0 : rising % 6 < 3 ? 2 : -2;   // (Starting to rise: it shakes)
            g.drawImage(sleeping, cx - sleeping.getWidth() / 2 + shake, feet - h, sleeping.getWidth(), h, null);
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
