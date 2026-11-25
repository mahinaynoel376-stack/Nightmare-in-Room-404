package nightmare404.entity;

import static nightmare404.tile.TileMap.LAYER_OBJECTS;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;
import nightmare404.world.Room;

// A tree in the quadrangle that isn't a tree (see Encounters).
//
// ASLEEP   It's just one of the trees: the map's own 2 x 2 tree tiles, solid trunk and all.
//          When the player comes outside it starts counting down: a short delay, a little longer for
//          the trees further away, so they wake one after another.
// POPPING  It wakes (POP_TICKS long): the tree tiles leave the map and it's drawn here instead. First
//          it shivers and its eyes open, then it tears its roots out of the ground in a spray of dirt
//          and rises up on them, leaving torn earth behind.
// HUNTING  It comes straight for the player, wherever they are in the quadrangle, sliding along
//          anything in its way and stepping round it if it's stuck. It's slower than the player.
//
// It's fought out on the map, with no battle screen (see battle.LastStand): grabbing the player hurts
// them and knocks them back, and the player strikes it by typing the word over its head.
// Leaving the quadrangle puts every tree still standing back in its place, asleep, and coming back
// starts it all again. A beaten tree leaves a stump, and stays beaten.
// How it hunts (and steps round what's in its way) is in Hunter, shared with Subject Zero.
public class TreeMonster extends Hunter {
    public static final int WAKE_DELAY = 90;        // Ticks after the player comes outside before the nearest tree wakes...
    public static final int WAKE_PER_TILE = 3;      // ...and this many more for each tile further off a tree stands
    public static final int POP_TICKS = 60;         // How long popping out takes (shiver 0-23, rise 24-43, roar 44-59)
    public static final int HUNT_SPEED = 3;         // Pixels per tick (the player walks at 4 and sprints at 6)
    private static final int SIZE = 88;             // How tall it's drawn (the sprite's own size)
    private static final int RISE = 18;             // How far it rises on its roots (the sprite has the tree 18 px up)

    private static BufferedImage plainTree;         // The 2 x 2 tree tiles as one picture, for the shiver

    private final int treeCol, treeRow;             // Top-left of its tree tiles on the map
    private int[] treeTiles;                        // The map's tiles there (objects layer), put back when it sleeps
    private boolean asleep = true;
    private boolean uprooted;                       // True while its tiles are off the map
    private int untilWake = -1;                     // Ticks left before it wakes (-1 = not counting down)
    private int popping;                            // Ticks into popping out (0 = not popping)

    // A tree whose 2 x 2 tiles have their top-left at (col, row) on the room's map. It stands (its
    // feet, see Monster) on the trunk row, halfway across the two columns.
    public TreeMonster(EnemyDef enemy, Room room, int ts, int col, int row) {
        super(enemy, room, false, ts, SIZE, new int[] {col, row + 1});
        treeCol = col;
        treeRow = row;
        treeTiles = new int[4];
        for (int i = 0; i < 4; i++) {
            treeTiles[i] = room.map.tileAt(LAYER_OBJECTS, col + i % 2, row + i / 2);
        }
        nudge(ts / 2);
    }

    public boolean asleep() { return asleep; }
    public boolean popping() { return popping > 0; }
    @Override
    public boolean hunting() { return !defeated && !asleep && popping == 0; }
    public int treeCol() { return treeCol; }
    public int treeRow() { return treeRow; }

    // Back in its place as a tree, asleep (unless it's been beaten: then it stays gone)
    @Override
    public void reset() {
        super.reset();
        if (treeTiles == null) return;   // Still being made (Monster's constructor calls this first)
        resetHunt();
        if (defeated) return;
        asleep = true;
        untilWake = -1;
        popping = 0;
        if (uprooted) {
            for (int i = 0; i < 4; i++) {
                room.map.setTile(LAYER_OBJECTS, treeCol + i % 2, treeRow + i / 2, treeTiles[i]);
            }
            uprooted = false;
        }
    }

    // The player has come outside: back in place, and wake after a short delay (longer further off)
    @Override
    public void playerArrived(Player player) {
        if (defeated) return;
        reset();
        double tiles = Math.hypot(player.x - x, player.y - y) / 32;
        untilWake = WAKE_DELAY + (int) (WAKE_PER_TILE * tiles);
    }

    @Override
    public void update(Player player) {
        if (defeated) return;
        if (asleep) {
            if (untilWake > 0) {                   // Counting down to waking up
                untilWake--;
                if (untilWake == 0) wake();
            }
        } else if (popping > 0) {
            popping++;
            if (popping >= POP_TICKS) {
                popping = 0;
                chasing = true;
            }
        } else if (player != null) {
            hunt(player, HUNT_SPEED);
        }
    }

    // Only a hunting tree can grab the player (an asleep one is just a tree)
    @Override
    public boolean touches(Player player) {
        return hunting() && super.touches(player);
    }

    // Starts popping out: its tiles leave the map (it's drawn by draw() from now on), and the
    // ground where its trunk stood is torn up
    private void wake() {
        asleep = false;
        popping = 1;
        uprooted = true;
        for (int i = 0; i < 4; i++) {
            room.map.setTile(LAYER_OBJECTS, treeCol + i % 2, treeRow + i / 2, TileMap.EMPTY);
        }
        room.map.setTile(LAYER_OBJECTS, treeCol, treeRow + 1, TileSet.CRACK);
    }

    @Override
    public void draw(Graphics2D g, long ticks) {
        int cx = x + 16, feet = y + 32;
        if (defeated) {
            if (uprooted) drawStump(g, cx, feet);
            return;
        }
        if (asleep) return;                     // The map's own tree tiles show it
        if (popping > 0 && popping < 24) {
            drawShiver(g, cx, feet, popping);
        } else if (popping > 0 && popping < 44) {
            drawRising(g, cx, feet, popping - 24);
        } else {
            int lift = 0, sway = 0;            // Bobbing and swaying as it walks (not while it pops out)
            if (popping == 0) {
                lift = (int) Math.round(Math.abs(Math.sin((ticks + phase) / 7.0)) * 3);
                sway = (int) Math.round(Math.sin((ticks + phase) / 14.0) * 1.5);
            }
            if (stunned > 0) {                 // Reeling: it shakes
                if ((stunned / 2) % 2 == 0) {
                    sway = 3;
                } else {
                    sway = -3;
                }
            }
            shadow(g, cx, feet);
            g.drawImage(image, cx - image.getWidth() / 2 + sway, feet - image.getHeight() - lift, null);
            if (popping > 0 || chasing) {
                g.setFont(new Font("Arial", Font.BOLD, 18));
                g.setColor(new Color(200, 20, 20));
                g.drawString("!", cx - 3, feet - image.getHeight() - lift - 4);
            }
        }
    }

    // Part 1 of popping out: the tree shakes harder and harder, leaves fall, and its eyes open
    private void drawShiver(Graphics2D g, int cx, int feet, int t) {
        int jitter = 1 + t / 8;                // Shaking one way then the other, harder as t grows
        if ((t / 2) % 2 != 0) jitter = -jitter;
        BufferedImage tree = plainTree();
        if (tree != null) g.drawImage(tree, cx - 32 + jitter, feet - 64, null);
        g.setColor(new Color(0, 143, 68));
        for (int i = 0; i < 5; i++) {               // Leaves shaken loose, drifting down
            int lx = cx - 26 + (i * 13 + phase * 7) % 52, ly = feet - 60 + ((t + i * 9) * 2) % 50;
            g.fillRect(lx, ly, 3, 2);
        }
        if (t >= 10) {                              // Eyes open on the trunk
            int alpha = Math.min(255, (t - 10) * 40);
            g.setColor(new Color(255, 70, 40, alpha / 2));
            g.fillRect(cx - 6 + jitter, feet - 19, 5, 5);
            g.fillRect(cx + 1 + jitter, feet - 19, 5, 5);
            g.setColor(new Color(255, 90, 50, alpha));
            g.fillRect(cx - 5 + jitter, feet - 18, 3, 2);
            g.fillRect(cx + 2 + jitter, feet - 18, 3, 2);
        }
    }

    // Part 2: it rises up out of the ground on its roots (they appear from below the ground line),
    // throwing dirt out on both sides
    private void drawRising(Graphics2D g, int cx, int feet, int t) {
        double k = t / 20.0;
        double eased = 1 - (1 - k) * (1 - k);
        int sink = (int) Math.round(RISE * (1 - eased));
        shadow(g, cx, feet);
        Graphics2D clip = (Graphics2D) g.create();
        clip.clipRect(cx - 60, feet - 120, 120, 120);   // Nothing below the ground line: the roots come up out of it
        clip.drawImage(image, cx - image.getWidth() / 2, feet + sink - image.getHeight(), null);
        clip.dispose();
        for (int i = 0; i < 14; i++) {                  // Clods of dirt, flying out and falling back
            double angle = Math.PI * (0.15 + 0.7 * ((i * 37 + phase) % 100) / 100.0);
            double speed = 2.2 + ((i * 53) % 10) / 4.0;
            double vx = Math.cos(angle) * speed, vy = -Math.sin(angle) * speed;
            if (i % 2 != 0) vx = -vx;                   // Every other one flies the other way
            double px = cx + vx * t, py = feet - 4 + vy * t + 0.18 * t * t;
            if (py > feet + 2) continue;
            if (i % 3 == 0) {
                g.setColor(new Color(60, 14, 0));
            } else {
                g.setColor(new Color(96, 62, 30));
            }
            g.fillRect((int) px, (int) py, 3, 3);
        }
    }

    // What's left when it's beaten: a withered stump
    private void drawStump(Graphics2D g, int cx, int feet) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(cx - 14, feet - 7, 28, 8);
        g.setColor(Color.BLACK);
        g.fillRect(cx - 9, feet - 14, 18, 11);
        g.fillOval(cx - 10, feet - 18, 20, 9);
        g.setColor(new Color(128, 69, 13));
        g.fillRect(cx - 8, feet - 13, 16, 9);
        g.setColor(new Color(185, 105, 44));
        g.fillOval(cx - 9, feet - 17, 18, 7);
        g.setColor(new Color(128, 69, 13));
        g.drawOval(cx - 5, feet - 15, 10, 3);
    }

    private void shadow(Graphics2D g, int cx, int feet) {
        Graphics2D s = (Graphics2D) g.create();
        s.setComposite(AlphaComposite.SrcOver.derive(0.35f));
        s.setColor(Color.BLACK);
        s.fillOval(cx - 26, feet - 6, 52, 10);
        s.dispose();
    }

    // The tree tiles (tiles/props/tree_0..3) put together, loaded once
    private static BufferedImage plainTree() {
        if (plainTree != null) return plainTree;
        BufferedImage tree = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tree.createGraphics();
        for (int i = 0; i < 4; i++) {
            try (InputStream in = TreeMonster.class.getResourceAsStream("/tiles/props/tree_" + i + ".png")) {
                if (in != null) g.drawImage(ImageIO.read(in), (i % 2) * 32, (i / 2) * 32, null);
            } catch (IOException e) {
                System.err.println("Could not load tree_" + i + ".png: " + e.getMessage());
            }
        }
        g.dispose();
        plainTree = tree;
        return tree;
    }
}
