package nightmare404.entity;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.world.Room;

// A monster walking around a room. Touching it starts its battle (see GamePanel.update).
//
// A regular monster PATROLS: it walks its route (an aisle, a row of the room) back and forth. If the
// player comes within NOTICE_RANGE it CHASES them, a bit faster, until they get GIVE_UP_RANGE away
// (or it gets stuck behind the furniture); then it walks back the way it came and carries on its round.
// It's slower than the player even when chasing, so a careful player can always get away.
// A BOSS stands guard on its spot and doesn't move. Its reach is 2 tiles wide, so standing in a
// 2-tile gap (like the Brute in the middle of the stairs) nobody gets past without a fight.
// When it's beaten it drops its LOOT on that spot: the key to its floor's way out, and supplies.
//
// The monsters have no walking frames (only the standing sprite from their battle), so they bob a little.
public class Monster {
    public static final int PATROL_SPEED = 2;           // Pixels per tick (the player walks at 4, sprints at 6)
    public static final int CHASE_SPEED = 3;
    public static final int NOTICE_RANGE = 3 * 32;      // Notices the player within 3 tiles...
    public static final int GIVE_UP_RANGE = 6 * 32;     // ...and gives up when they're 6 tiles away
    private static final int GIVE_UP_STUCK = 45;        // ...or when it's been stuck this many ticks
    private static final int MAX_TRAIL = 900;           // How far back (in ticks) it remembers its way home
    private static final int HEIGHT = 52, BOSS_HEIGHT = 72;   // Drawn size in the world, in pixels

    public final EnemyDef enemy;
    public final Room room;
    public final boolean boss;
    public boolean defeated;
    public boolean chasing;
    public Loot loot;           // What it drops when it's beaten (bosses), or null
    public boolean lootTaken;

    // Like the player: (x, y) is the top-left of the tile its feet are on, and the hitbox is a
    // 16 x 16 box at its feet
    public int x, y;
    public final Rectangle solidArea = new Rectangle(0, 0, 16, 16);

    private final Point[] route;            // Patrol waypoints, in pixels; the first is where it starts
    private int target;                     // The waypoint it's walking to
    private final ArrayDeque<Point> trail = new ArrayDeque<>();   // Where it's been while chasing
    private int stuck;
    private final BufferedImage image;      // The battle sprite, scaled once to its size in the world
    private final int phase;                // So the monsters don't all bob in step

    // routeTiles: {col, row} pairs, the first being where it starts. One pair = it stands still.
    public Monster(EnemyDef enemy, Room room, boolean boss, int ts, int[]... routeTiles) {
        this.enemy = enemy;
        this.room = room;
        this.boss = boss;
        route = new Point[routeTiles.length];
        for (int i = 0; i < routeTiles.length; i++) {
            route[i] = new Point(routeTiles[i][0] * ts, routeTiles[i][1] * ts);
        }
        image = scaled(BattleScreen.Sprites.get(enemy.sprite(), true), boss ? BOSS_HEIGHT : HEIGHT);
        phase = Math.abs(enemy.name().hashCode() + routeTiles[0][0] * 7) % 60;
        reset();
    }

    // Moves it (and its route) dx pixels sideways: to stand between two tiles, like the Brute
    // in the middle of the 2-tile-wide stairs
    public Monster nudge(int dx) {
        for (Point p : route) p.x += dx;
        reset();
        return this;
    }

    // Back to its starting spot, not chasing anyone (after it beats the player, for example)
    public void reset() {
        x = route[0].x;
        y = route[0].y;
        target = route.length > 1 ? 1 : 0;
        chasing = false;
        trail.clear();
        stuck = 0;
        syncHitbox();
    }

    public void update(Player player) {
        if (defeated || boss || route.length < 2) return;   // Bosses stand guard
        int dist = (int) Math.hypot(player.solidArea.getCenterX() - solidArea.getCenterX(),
                                    player.solidArea.getCenterY() - solidArea.getCenterY());
        if (!chasing && dist < NOTICE_RANGE) {
            chasing = true;
            stuck = 0;
        } else if (chasing && (dist > GIVE_UP_RANGE || stuck > GIVE_UP_STUCK)) {
            chasing = false;
        }

        if (chasing) {
            int oldX = x, oldY = y;
            stepTowards(player.x, player.y, CHASE_SPEED);
            if (x == oldX && y == oldY) {
                stuck++;
            } else {
                stuck = 0;
                trail.push(new Point(oldX, oldY));
                if (trail.size() > MAX_TRAIL) trail.removeLast();
            }
        } else if (!trail.isEmpty()) {
            Point back = trail.pop();               // Retrace its steps back to its route
            x = back.x;
            y = back.y;
            syncHitbox();
        } else {
            Point goal = route[target];
            if (x == goal.x && y == goal.y) {
                target = (target + 1) % route.length;
                return;
            }
            int oldX = x, oldY = y;
            stepTowards(goal.x, goal.y, PATROL_SPEED);
            if (x == oldX && y == oldY) target = (target + 1) % route.length;   // Something's in the way: turn round
        }
    }

    // True if it's close enough to the player to grab them
    public boolean touches(Player player) {
        return !defeated && reach().intersects(player.solidArea);
    }

    // True if it's been beaten, its loot is still lying there, and the player is standing on it
    public boolean lootUnder(Player player) {
        return defeated && loot != null && !lootTaken && reach().intersects(player.solidArea);
    }

    // The area it grabs from: its hitbox, a little bigger; for a boss, 2 tiles wide
    private Rectangle reach() {
        Rectangle reach = boss ? new Rectangle(x + 16 - 32, y + 8, 64, 24) : new Rectangle(solidArea);
        reach.grow(4, 4);
        return reach;
    }

    // Moves up to `speed` pixels towards (tx, ty), across first, then down; one pixel at a time so it
    // stops flush against walls and furniture, like the player
    private void stepTowards(int tx, int ty, int speed) {
        int moves = speed;
        while (moves > 0 && x != tx) {
            int dx = Integer.signum(tx - x);
            x += dx;
            syncHitbox();
            if (blocked()) { x -= dx; syncHitbox(); break; }
            moves--;
        }
        while (moves > 0 && y != ty) {
            int dy = Integer.signum(ty - y);
            y += dy;
            syncHitbox();
            if (blocked()) { y -= dy; syncHitbox(); break; }
            moves--;
        }
    }

    // Walls and furniture block it, it never steps into a doorway, and it stays on its own screen
    private boolean blocked() {
        return room.map.collides(solidArea) || room.map.doorTouching(solidArea) != null
                || x < 0 || x > room.map.widthPx() - 32;
    }

    private void syncHitbox() {
        solidArea.x = x + 8;
        solidArea.y = y + 16;
    }

    // Draws it standing on its feet tile, with a shadow; a red "!" over its head while it's chasing
    public void draw(Graphics2D g, long ticks) {
        if (defeated) {
            if (loot != null && !lootTaken) drawLoot(g, ticks);
            return;
        }
        int cx = x + 16, feet = y + 32;
        int lift = (int) Math.round(Math.abs(Math.sin((ticks + phase) / 10.0)) * (boss ? 2 : 3));
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(cx - image.getWidth() * 2 / 5, feet - 5, image.getWidth() * 4 / 5, 8);
        g.drawImage(image, cx - image.getWidth() / 2, feet - image.getHeight() - lift, null);
        if (chasing) {
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.setColor(new Color(200, 20, 20));
            g.drawString("!", cx - 3, feet - image.getHeight() - lift - 4);
        }
    }

    // The dropped loot: a key glinting on the floor where the boss fell
    private void drawLoot(Graphics2D g, long ticks) {
        int cx = x + 16, cy = y + 22;
        float pulse = (float) (0.5 + 0.5 * Math.sin(ticks / 12.0));
        g.setColor(new Color(255, 214, 120, (int) (60 + 70 * pulse)));
        g.fillOval(cx - 16, cy - 10, 32, 20);
        Graphics2D k = (Graphics2D) g.create();
        k.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        k.translate(cx, cy - (int) Math.round(2 * pulse));
        k.rotate(Math.toRadians(-20));
        k.setColor(new Color(60, 40, 10));
        k.fillOval(-11, -6, 12, 12);                       // The bow (the ring)
        k.fillRect(-1, -2, 14, 4);                         // The shaft
        k.fillRect(8, 2, 3, 5); k.fillRect(12, 2, 2, 4);   // The teeth
        k.setColor(new Color(232, 184, 70));
        k.fillOval(-10, -5, 10, 10);
        k.fillRect(0, -1, 12, 2);
        k.fillRect(9, 1, 2, 4); k.fillRect(12, 1, 1, 3);
        k.setColor(new Color(60, 40, 10));
        k.fillOval(-7, -2, 4, 4);                          // The hole in the bow
        k.dispose();
    }

    // The sprite scaled to `height` pixels tall (no wider than 1.3 times that), pixel for pixel for
    // small sprites and smoothly for big ones (the Hall Monitor's picture is 1200 x 960)
    private static BufferedImage scaled(BufferedImage src, int height) {
        if (src == null) {
            BufferedImage missing = new BufferedImage(32, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = missing.createGraphics();
            g.setColor(new Color(60, 60, 64));
            g.fillRect(0, 0, 32, height);
            g.dispose();
            return missing;
        }
        while (src.getHeight() > height * 4) {   // A big picture: halve it a few times first, so it stays smooth
            BufferedImage half = new BufferedImage(src.getWidth() / 2, src.getHeight() / 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = half.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, half.getWidth(), half.getHeight(), null);
            g.dispose();
            src = half;
        }
        double fit = Math.min((double) height / src.getHeight(), height * 1.3 / src.getWidth());
        int w = Math.max(1, (int) Math.round(src.getWidth() * fit)), h = Math.max(1, (int) Math.round(src.getHeight() * fit));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, src.getHeight() <= 128
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
