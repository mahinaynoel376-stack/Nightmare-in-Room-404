package nightmare404.entity;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

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
    // Movement speeds in pixels per tick (at 60 FPS: walk = 240 px/s, sprint = 360 px/s)
    public static final int WALK_SPEED = 4;
    public static final int SPRINT_SPEED = 6;
    // Ticks per walk frame. Both cover 24 px per step, so the feet keep pace with the ground
    // and the legs move faster when sprinting (walk = 10 steps/s, sprint = 15 steps/s).
    public static final int WALK_ANIM_TICKS = 6;
    public static final int SPRINT_ANIM_TICKS = 4;

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
        maxHealth = 100;
        health = maxHealth;
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
        speed = gp.sprintPressed ? SPRINT_SPEED : WALK_SPEED;
        int animTicks = gp.sprintPressed ? SPRINT_ANIM_TICKS : WALK_ANIM_TICKS;
        int dx = 0, dy = 0;
        if (gp.upPressed) { dy -= speed; direction = 0; }
        if (gp.downPressed) { dy += speed; direction = 1; }
        if (gp.leftPressed) { dx -= speed; direction = 2; }
        if (gp.rightPressed) { dx += speed; direction = 3; }

        // 2. Move, stopping at walls and furniture. Then, if we stepped onto a door or off the
        //    side of the screen, follow the current room's link to the next room.
        moveWithCollision(dx, dy);
        Rectangle doorHere = gp.currentRoom.map.doorTouching(solidArea);
        if (doorHere != null && gp.currentRoom.doorLeadsTo(gp.currentRoom.map.doorIndex(doorHere)) != null) {
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

        x = door.x + door.width / 2 - size / 2;            // Centred on the door
        if (next.map.isFloorAbove(door)) {
            y = door.y - size - 2;                         // Classroom: hitbox ends 2 px above the door
        } else {
            y = door.y + door.height - solidOffsetY + 2;   // Hallway: hitbox starts 2 px below the door
        }
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
