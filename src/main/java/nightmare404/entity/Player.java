package nightmare404.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.imageio.ImageIO;

import nightmare404.battle.BattleScreen;
import nightmare404.main.GamePanel;
import nightmare404.map.Classroom;
import nightmare404.tile.Staircase;
import nightmare404.world.Floor;
import nightmare404.world.Room;

public class Player {
    GamePanel gp;

    public int x, y;
    public int speed;
    public int health;
    public int maxHealth;

    // What the player carries into battle (see BattleScreen): first aid kits, and the locker items
    // (Old Textbooks, Red Pens, Hall Passes). Both are used up in the battles and carry over between them.
    public int kits;
    public BattleScreen.Items items;

    // The keys the bosses drop (see Encounters): each opens one floor's way down, or out
    public final Set<String> keys = new LinkedHashSet<>();

    // The ids of the clues found (see story.Quests). Like the keys, giving up puts back the ones found
    // on this floor since arriving, so the clue (and its supplies) can't be kept after a lost battle.
    public final Set<String> clues = new LinkedHashSet<>();

    // Giving up after losing a battle takes the player back to how they came onto the current floor:
    // the same spot (Room 404 at the start, then the foot of the stairs they arrived by, or just outside
    // the main entrance), the same health, kits, items, keys and clues, and the monsters they've beaten
    // on this floor since are back. Out in the quadrangle, every monster beaten saves progress
    // (saveProgress) without moving that spot, so going down out there doesn't undo the whole fight.
    public Room checkpointRoom;
    public int checkpointX, checkpointY;
    private int savedHealth, savedKits;
    private int[] savedItems;
    private Set<String> savedKeys;
    private Set<String> savedClues;
    private Set<Monster> beatenBefore;  // Monsters on this floor already beaten when the player arrived
    public Rectangle solidArea;

    // The size of the player's feet tile; (x, y) is its top-left corner
    private final int size;
    // The hitbox is smaller than a tile and sits at the feet, so the player fits through 1-tile aisles
    private final int solidOffsetX;
    private final int solidOffsetY;

    // Sprite frames: [direction][frame] where frame 0 = idle, 1 = walk1, 2 = walk2
    private static final String[] DIRECTIONS = {"up", "down", "left", "right"};
    private static final String[] FRAMES = {"idle", "walk1", "walk2"};
    private final BufferedImage[][] sprites = new BufferedImage[4][3];

    // Animation state
    private int direction = 1;      // Index into DIRECTIONS, starts facing down
    private boolean moving = false;
    private int spriteCounter = 0;  // Counts update() ticks between walk frames
    // Walk cycle per direction (same order as DIRECTIONS): stride, legs together, other stride, legs together.
    // The idle frame acts as the "passing" pose so the legs don't snap between two wide strides.
    private static final int[][] WALK_CYCLES = {
        {1, 0, 2, 0}, // up
        {1, 0, 2, 0}, // down
        {1, 0, 2, 0}, // left
        {1, 0, 2, 0}, // right
    };
    private static final int WALK_STEPS = 4; // Length of every cycle above
    private int walkStep = 0;       // Index into WALK_CYCLES[direction]
    // Movement speeds in pixels per tick (at 60 FPS: walk = 180 px/s, sprint = 300 px/s)
    public static final int WALK_SPEED = 3;
    public static final int SPRINT_SPEED = 5;
    // Ticks per walk frame. Both cover about 24 px per step, so the feet keep pace with the ground
    // and the legs move faster when sprinting (walk = 7.5 steps/s, sprint = 12 steps/s).
    public static final int WALK_ANIM_TICKS = 8;
    public static final int SPRINT_ANIM_TICKS = 5;
    // The bad ending: walking out of the quadrangle, Kai is dizzy (see ui.Dawn): he walks slower
    // and sways from side to side
    public boolean dizzy;
    public static final int DIZZY_SPEED = 2;

    // Stamina: running uses it up, and he gets it back walking or standing still (faster standing).
    // Run it all the way out and he's out of breath: no running until it's back up to half.
    // At 60 FPS: 3 seconds of running from full; it fills up again in 6 seconds walking, 3 standing still.
    public static final int STAMINA_MAX = 360;
    public static final int RUN_COST = 2;            // Stamina used per frame of running
    public static final int REST_WALKING = 1;        // Stamina back per frame, walking
    public static final int REST_STILL = 2;          // ...and standing still
    public int stamina = STAMINA_MAX;
    public boolean outOfBreath;                      // Ran out: can't run until stamina is back to half

    public Player(GamePanel gp) {
        this.gp = gp;
        this.size = gp.tileSize;
        solidOffsetX = size / 4;
        solidOffsetY = size / 2;
        solidArea = new Rectangle(0, 0, size / 2, size / 2);
        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        // Start in the classroom's middle aisle
        x = Classroom.START_COL * gp.tileSize;
        y = Classroom.START_ROW * gp.tileSize;
        syncHitbox();
        speed = WALK_SPEED; // Current speed; update() switches it to SPRINT_SPEED while Shift is held
        maxHealth = BattleScreen.HERO_MAX_HP;
        health = maxHealth;
        kits = BattleScreen.START_KITS;
        items = new BattleScreen.Items(1, 1, 1);   // One of each to start with
        setCheckpoint();
    }

    // Remembers where the player is and how things are now, as they come onto a floor (see respawn)
    public void setCheckpoint() {
        checkpointRoom = gp.currentRoom;
        checkpointX = x;
        checkpointY = y;
        saveProgress();
    }

    // Remembers how things are now (health, supplies, keys, clues, the monsters beaten), but keeps the
    // spot giving up takes the player back to
    public void saveProgress() {
        savedHealth = health;
        savedKits = kits;
        savedItems = new int[] {items.textbooks, items.redPens, items.hallPasses};
        savedKeys = new LinkedHashSet<>(keys);
        savedClues = new LinkedHashSet<>(clues);
        beatenBefore = new HashSet<>();
        for (Monster m : gp.world.monsters) {
            if (m.room.floor == checkpointRoom.floor && m.defeated) beatenBefore.add(m);
        }
    }

    // After losing a battle and giving up: back to how things were when the player came onto this
    // floor. Monsters beaten on it since are back (with their loot), and every monster on it is back
    // at its post.
    public void respawn() {
        for (Monster m : gp.world.monsters) {
            if (m.room.floor != checkpointRoom.floor) continue;
            if (m.defeated && !beatenBefore.contains(m)) {
                m.defeated = false;
                m.lootTaken = false;
            }
            m.reset();
        }
        health = savedHealth;
        kits = savedKits;
        items.textbooks = savedItems[0];
        items.redPens = savedItems[1];
        items.hallPasses = savedItems[2];
        keys.clear();
        keys.addAll(savedKeys);
        clues.clear();
        clues.addAll(savedClues);
        gp.enterRoom(checkpointRoom);
        x = checkpointX;
        y = checkpointY;
        direction = 1;
        moving = false;
        stamina = STAMINA_MAX;           // Waking up again, he's got his breath back
        outOfBreath = false;
        syncHitbox();
    }

    // Picks up what a boss dropped: its key and its supplies
    public void collect(Loot loot) {
        keys.add(loot.key());
        addSupplies(loot);
    }

    // Adds the first aid kits and locker items in this loot (a boss's, or what's found with a clue)
    public void addSupplies(Loot loot) {
        kits += loot.kits();
        items.textbooks += loot.textbooks();
        items.redPens += loot.redPens();
        items.hallPasses += loot.hallPasses();
    }

    public void getPlayerImage() {
        // Loads e.g. /characters/player/down_walk1.png from src/main/resources
        for (int d = 0; d < DIRECTIONS.length; d++) {
            for (int f = 0; f < FRAMES.length; f++) {
                String path = "/characters/player/" + DIRECTIONS[d] + "_" + FRAMES[f] + ".png";
                try (InputStream is = getClass().getResourceAsStream(path)) {
                    if (is == null) {
                        throw new IllegalStateException("Missing player sprite: " + path);
                    }
                    sprites[d][f] = ImageIO.read(is);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void update() {
        // 1. Read input (the last key checked decides which way the sprite faces)
        moving = gp.upPressed || gp.downPressed || gp.leftPressed || gp.rightPressed;
        boolean running = gp.sprintPressed && moving && !outOfBreath;
        updateStamina(running);
        speed = running ? SPRINT_SPEED : WALK_SPEED;
        int animTicks = running ? SPRINT_ANIM_TICKS : WALK_ANIM_TICKS;
        if (dizzy) {                     // Dizzy: slower, and too unsteady to run
            speed = DIZZY_SPEED;
            animTicks = WALK_ANIM_TICKS + 4;
        }
        int dx = 0, dy = 0;
        if (gp.upPressed) { dy -= speed; direction = 0; }
        if (gp.downPressed) { dy += speed; direction = 1; }
        if (gp.leftPressed) { dx -= speed; direction = 2; }
        if (gp.rightPressed) { dx += speed; direction = 3; }
        if (dizzy && (dx != 0 || dy != 0)) {   // ...and every step sways a little to the side
            int sway = (int) Math.round(2 * Math.sin(gp.ticks / 18.0));
            if (dy != 0) dx += sway;
            else dy += sway;
        }

        // 2. Move, stopping at walls and furniture. Then, if we stepped onto a door or off the
        //    side of the screen, follow the current room's link to the next room.
        moveWithCollision(dx, dy);
        Rectangle doorHere = gp.currentRoom.map.doorTouching(solidArea);
        Room behindDoor = doorHere == null ? null : gp.currentRoom.doorLeadsTo(gp.currentRoom.map.doorIndex(doorHere));
        if (behindDoor != null && behindDoor.outdoors && gp.currentRoom.floor.locked(keys)) {
            // The way out of the building is locked until the player has the floor's key
            gp.director.lockedOut(gp.currentRoom.floor);
        } else if (behindDoor != null && behindDoor.lockedWith != null && !keys.contains(behindDoor.lockedWith)) {
            gp.director.doorLocked(behindDoor);         // A locked room (the Containment Lab)
        } else if (behindDoor != null) {
            goThroughDoor(doorHere);
        } else if (!checkStairs()) {
            checkScreenEdges();
        }

        // 3. Keep hitbox synced with visual coordinates
        syncHitbox();

        // 4. Advance the walk animation while moving, reset it when standing still
        if (moving) {
            spriteCounter++;
            if (spriteCounter >= animTicks) {
                walkStep = (walkStep + 1) % WALK_STEPS;
                spriteCounter = 0;
            }
        } else {
            spriteCounter = 0;
            walkStep = 0;
        }
    }

    // Running uses stamina up; walking or standing still gets it back
    private void updateStamina(boolean running) {
        if (running) {
            stamina = Math.max(0, stamina - RUN_COST);
            if (stamina == 0) outOfBreath = true;
        } else {
            stamina = Math.min(STAMINA_MAX, stamina + (moving ? REST_WALKING : REST_STILL));
            if (stamina >= STAMINA_MAX / 2) outOfBreath = false;
        }
    }

    // A thin bar over his head showing his stamina, only while it isn't full (red while he's out of breath)
    public void drawStamina(Graphics2D g) {
        if (stamina >= STAMINA_MAX) return;
        int w = 24, bx = x + (size - w) / 2, by = y - size - 6;
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(bx - 1, by - 1, w + 2, 5);
        g.setColor(outOfBreath ? new Color(200, 40, 36) : new Color(120, 200, 230));
        g.fillRect(bx, by, w * stamina / STAMINA_MAX, 3);
    }

    // Moves one pixel at a time so the player stops flush against walls and furniture.
    // X and Y are handled separately so you can slide along a wall while moving diagonally.
    private void moveWithCollision(int dx, int dy) {
        for (int i = 0; i < Math.abs(dx); i++) {
            x += Integer.signum(dx);
            syncHitbox();
            if (blocked()) {
                x -= Integer.signum(dx);
                break;
            }
        }
        for (int i = 0; i < Math.abs(dy); i++) {
            y += Integer.signum(dy);
            syncHitbox();
            if (blocked()) {
                y -= Integer.signum(dy);
                break;
            }
        }
        syncHitbox();
    }

    // True if the hitbox overlaps a solid tile. Near the edge of a hallway screen the hitbox can
    // stick out into the neighbouring room, so that part is checked against the neighbour's map too.
    private boolean blocked() {
        Room room = gp.currentRoom;
        if (room.map.collides(solidArea)) return true;
        for (Monster m : room.monsters) {
            if (m.blocks(solidArea)) return true;     // A sleeping boss: the boulder, the pod (see SleepingBoss)
        }
        if (room.haze != null && room.haze.blocks(solidArea)) return true;   // Floor 3's way down (see Haze)
        if (solidArea.x < 0 && room.left != null) {
            return room.left.map.collides(shifted(room.left.map.widthPx()));
        }
        if (solidArea.x + solidArea.width > room.map.widthPx() && room.right != null) {
            return room.right.map.collides(shifted(-room.map.widthPx()));
        }
        return false;
    }

    // The hitbox moved sideways, i.e. seen from the neighbouring room's coordinates
    private Rectangle shifted(int dx) {
        return new Rectangle(solidArea.x + dx, solidArea.y, solidArea.width, solidArea.height);
    }

    // Follows the link of the door being touched (classroom <-> hallway, main entrance <-> quadrangle)
    // and stands the player just past the door it's linked to. A room and its hallway screen have
    // their doors in the same order (but not the same columns: the hallway screen is narrower), and
    // each door knows which door it leads to, so the player comes out at the door they went in through.
    private void goThroughDoor(Rectangle doorHere) {
        Room here = gp.currentRoom;
        int number = here.map.doorIndex(doorHere);
        Room next = here.doorLeadsTo(number);
        Rectangle door = next.map.door(here.doorArrivesAt(number));
        gp.enterRoom(next);
        boolean goingOutside = next.outdoors && !here.outdoors;

        x = door.x + door.width / 2 - size / 2;            // Centred on the door
        if (next.map.isFloorAbove(door)) {
            y = door.y - size - 2;                         // Classroom: hitbox ends 2 px above the door
        } else {
            y = door.y + door.height - solidOffsetY + 2;   // Hallway: hitbox starts 2 px below the door
        }
        syncHitbox();
        if (goingOutside) setCheckpoint();   // Going down out there brings you back to the main entrance
    }

    // Shoves the player (dx, dy) pixels, stopping at walls: knocked back by a monster outside
    public void push(int dx, int dy) {
        moveWithCollision(dx, dy);
        syncHitbox();
    }

    // When the player's centre crosses the left or right edge, they're now in the neighbouring room.
    // x shifts by exactly that room's width, so their position in the world doesn't change; together
    // with the scrolling camera the hallway looks like one continuous corridor.
    // If there's no neighbour (null link), the edge acts as a wall.
    private void checkScreenEdges() {
        Room room = gp.currentRoom;
        int centre = x + size / 2;
        boolean changedRoom = false;
        if (centre < 0 && room.left != null) {
            gp.enterRoom(room.left);
            x += room.left.map.widthPx();
            changedRoom = true;
        } else if (centre >= room.map.widthPx() && room.right != null) {
            x -= room.map.widthPx();
            gp.enterRoom(room.right);
            changedRoom = true;
        }

        room = gp.currentRoom;
        int mapWidth = room.map.widthPx();
        if (room.left == null && x < 0) x = 0;
        if (room.right == null && x > mapWidth - size) x = mapWidth - size;
        // Just arrived on a fixed screen (no scrolling): pull the player fully onto the screen.
        // Only on arrival - doing it every frame would stop the player ever reaching the edge.
        if (changedRoom && room.fixedScreen) {
            x = Math.max(0, Math.min(x, mapWidth - size));
        }
        syncHitbox();
    }

    // Stepping onto the landing at the top of a flight takes you to another floor (see Staircase):
    // the left flight goes down, the right flight goes up. A flight that doesn't work has no zone
    // (null), so it never takes you anywhere. Returns true if the player changed floor.
    private boolean checkStairs() {
        Staircase stairs = gp.currentRoom.map.stairs;
        if (stairs == null) return false;
        Floor floor = gp.currentRoom.floor;
        if (floor.below != null && stairs.downZone != null && stairs.downZone.intersects(solidArea)) {
            if (floor.locked(keys)) {   // The way down is locked until the player has the floor's key
                gp.director.lockedOut(floor);
                return false;
            }
            arriveOnStairs(floor.below, true);
            return true;
        }
        if (floor.above != null && stairs.upZone != null && stairs.upZone.intersects(solidArea)) {
            arriveOnStairs(floor.above, false);
            return true;
        }
        return false;
    }

    // Moves to the target floor's stairwell at the other end of the same flight, at its foot.
    // (Went down? Then you arrive at the bottom of that floor's way up, and vice versa.)
    private void arriveOnStairs(Floor target, boolean wentDown) {
        Room room = wentDown ? target.upStairs() : target.downStairs();
        gp.enterRoom(room);
        Staircase arrival = room.map.stairs;
        Point foot = wentDown ? arrival.upFoot : arrival.downFoot;
        x = foot.x;
        y = foot.y;
        direction = 1;   // Face down, away from the stairs
        syncHitbox();
        setCheckpoint(); // A new floor: losing a battle here brings you back to these stairs
    }

    private void syncHitbox() {
        solidArea.x = x + solidOffsetX;
        solidArea.y = y + solidOffsetY;
    }

    public void draw(Graphics2D g2d) {
        BufferedImage image = sprites[direction][moving ? WALK_CYCLES[direction][walkStep] : 0];

        // The sprite is 1 tile wide and 2 tiles tall, so it's drawn one tile above
        // the hitbox: the feet sit on (x, y) and the head extends upward
        int spriteY = y - size;
        g2d.drawImage(image, x, spriteY, size, size * 2, null);
        // The health bar is drawn in the bottom UI bar (see BottomUI)
    }
}
