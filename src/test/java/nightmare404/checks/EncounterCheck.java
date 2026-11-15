package nightmare404.checks;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.BattleResult;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.debug.BattleScreensDebug;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.main.GamePanel;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// The monsters and their battles: where they are, how they move, what touching one does, winning
// and losing (giving up puts the floor back as the player found it), the bosses (the Brute on the
// stairs), the keys they drop and the locked ways out, and that every battle's pictures and words load.
// No window opens: starting a battle is swapped for "remember which monster it was".
public class EncounterCheck {
    static GamePanel gp; static Player p; static int fails = 0;
    static Monster started;     // The monster whose battle was started, if any

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what); if (!ok) fails++; }
    static void put(Room room, int col, int row) { gp.currentRoom = room; p.x = col * 32; p.y = row * 32; p.update(); }
    static void ticks(int n) { for (int i = 0; i < n && started == null; i++) gp.update(); }
    static void hold(String keys, int n) {
        started = null;
        gp.upPressed = keys.contains("U"); gp.downPressed = keys.contains("D"); gp.leftPressed = keys.contains("L"); gp.rightPressed = keys.contains("R");
        ticks(n);
        gp.upPressed = gp.downPressed = gp.leftPressed = gp.rightPressed = false;
    }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor;
        gp.battleStarter = m -> started = m;
        List<Room> rooms = new ArrayList<>();
        for (Floor f = gp.world.top; f != null; f = f.below)
            for (Room r = f.head; r != null; r = r.right) {
                if (!rooms.contains(r)) rooms.add(r);
                for (Room linked : r.linkedRooms()) for (Room s = Floor.leftEnd(linked); s != null; s = s.right) if (!rooms.contains(s)) rooms.add(s);
            }

        System.out.println("1. Where the monsters are");
        check("Room 404, where the game starts, has no monsters", gp.world.floor4.find("Room 404").monsters.isEmpty());
        check("the player starts in Room 404", gp.currentRoom.name.equals("Room 404"));
        for (Floor f = gp.world.top; f != null; f = f.below) {
            check("Floor " + f.number + " has a boss: " + (f.boss == null ? "none" : f.boss.enemy.name() + " in " + f.boss.room.name), f.boss != null && f.boss.boss);
        }
        for (Room r : rooms) {
            long regular = r.monsters.stream().filter(m -> !m.boss).count();
            if (regular > 0) check(r.name + ": " + regular + " monsters (2 or 3)", regular >= 2 && regular <= 3);
        }
        check("no hallway screen has a regular monster", rooms.stream().filter(r -> r.name.startsWith("Hallway")).allMatch(r -> r.monsters.isEmpty()));
        check(gp.world.monsters.size() + " monsters in all", gp.world.monsters.size() >= 20);

        System.out.println("2. Every monster stands somewhere it can walk, away from the doors, and walks its whole route");
        for (Monster m : gp.world.monsters) {
            String who = m.enemy.name() + " (" + m.room.name + ")";
            check(who + " starts on a walkable tile", !m.room.map.collides(m.solidArea));
            double nearestDoor = Double.MAX_VALUE;
            for (int i = 0; i < m.room.map.doorCount(); i++) {
                Rectangle d = m.room.map.door(i);
                nearestDoor = Math.min(nearestDoor, Point.distance(d.getCenterX(), d.getCenterY(), m.solidArea.getCenterX(), m.solidArea.getCenterY()));
            }
            check(who + " is at least 4 tiles from any door (" + (int) (nearestDoor / 32) + ")", nearestDoor >= 4 * 32);
            if (!m.boss) {
                // Far from the player, it should cover its whole route, there and back
                Player far = farAway();
                int minX = m.x, maxX = m.x, minY = m.y, maxY = m.y, startX = m.x, startY = m.y;
                boolean back = false;
                for (int t = 0; t < 2000; t++) {
                    m.update(far);
                    minX = Math.min(minX, m.x); maxX = Math.max(maxX, m.x); minY = Math.min(minY, m.y); maxY = Math.max(maxY, m.y);
                    if (t > 20 && m.x == startX && m.y == startY) back = true;
                    if (m.room.map.collides(m.solidArea)) { check(who + " walked into something", false); break; }
                }
                int run = Math.max(maxX - minX, maxY - minY) / 32;
                check(who + " patrols " + run + " tiles and comes back", run >= 4 && back);
                // Screens you walk into from the side (the library, the stairs): keep 4 tiles from that edge
                boolean edges = (m.room.left == null || minX >= 4 * 32) && (m.room.right == null || maxX <= m.room.map.widthPx() - 5 * 32);
                check(who + " keeps 4 tiles from any screen edge you can walk in from", edges);
                m.reset();
            }
        }

        System.out.println("3. Chasing and touching");
        Room r402 = gp.world.floor4.find("Room 402");
        Monster mimic = r402.monsters.get(0);
        mimic.reset();
        put(r402, 12, 15); ticks(40);                // Far away (6 rows down): it just patrols
        check("far away, the Desk Mimic doesn't chase (" + mimic.enemy.name() + ")", !mimic.chasing);
        mimic.reset();
        put(r402, 7, 9); started = null;             // In its aisle, 3 tiles ahead of it
        ticks(120);
        check("close by, it notices and comes after the player", started == mimic);
        check("touching it starts its battle (the world pauses)", gp.inBattle);

        System.out.println("4. After a battle");
        gp.endBattle(mimic, new BattleResult(true, 63, 2));
        check("won: the Desk Mimic is gone for good", mimic.defeated);
        check("won: health and first aid kits carry over (63 HP, 2 kits)", p.health == 63 && p.kits == 2 && !gp.inBattle);
        Monster wraith = r402.monsters.get(1);
        put(r402, 12, 12);
        wraith.x = 12 * 32; wraith.y = 10 * 32;     // Pretend it chased the player down
        gp.endBattle(wraith, new BattleResult(false, 0, 1));
        check("gave up: back in Room 404, where the floor began", gp.currentRoom.name.equals("Room 404"));
        check("gave up: health and kits as they were when the floor began (100 HP, 3 kits)", p.health == 100 && p.kits == 3);
        check("gave up: the Desk Mimic, beaten since, is back", !mimic.defeated && !wraith.defeated);
        check("gave up: the Chalk Wraith is back at its post", wraith.x == 20 * 32 && wraith.y == 6 * 32);
        put(r402, 12, 13); started = null;
        ticks(30);
        check("right after a battle the monsters give the player a moment", started == null);

        System.out.println("5. The Brute stands in the middle of the stairs down, and drops the key to them");
        Floor f4 = gp.world.floor4, f3 = gp.world.floor3, f2 = gp.world.floor2, f1 = gp.world.floor1;
        Room f4west = f4.find("West Stairs");
        check("the Brute is on Floor 4's way down (" + f4.boss.room.name + ")", f4.boss.room == f4west && f4west == f4.downStairs());
        Point foot = f4west.map.stairs.downFoot;
        String[] sides = {"in the middle of the flight", "at its left edge", "at its right edge"};
        int[] xs = {foot.x, 5 * 32 - 8, 6 * 32 + 8};
        for (int i = 0; i < 3; i++) {
            putPx(f4west, xs[i], foot.y); ticks(40);
            hold("U", 150);
            check("climbing " + sides[i] + ": the Brute stops the player (its battle starts)", started == f4.boss && gp.currentRoom.floor == f4);
            gp.inBattle = false;
        }
        check("without its key, Floor 4's way down is locked", f4.locked(p.keys) && f4.lockedNote != null);
        int pens = p.items.redPens;
        putPx(f4west, foot.x, foot.y); hold("U", 150);
        gp.endBattle(f4.boss, new BattleResult(true, 80, 3));
        check("won: the Brute leaves its loot where it stood", f4.boss.defeated && !f4.boss.lootTaken
                && gp.notice().contains("glints"));
        started = null; ticks(70);
        check("standing on it picks it up: the West Stairwell Key", p.keys.contains("West Stairwell Key") && f4.boss.lootTaken);
        check("...and supplies: +1 kit (4), +1 Red Pen (" + p.items.redPens + ")", p.kits == 4 && p.items.redPens == pens + 1);
        check("...and the player reads what they found", gp.notice().contains("janitor's key"));
        check("with the key, the way down is open", !f4.locked(p.keys));
        hold("U", 40);                           // Long enough to climb the flight, not to come back up
        check("the stairs take the player down to Floor 3", gp.currentRoom.floor == f3);
        check("arriving on a new floor moves the checkpoint there", p.checkpointRoom == gp.currentRoom);

        System.out.println("6. Giving up puts the floor back as the player found it");
        Room libEntrance = f3.find("Library (Entrance)"), libWest = f3.find("Library (West)");
        Monster librarian = libEntrance.monsters.get(0), undead = libEntrance.monsters.get(1);
        librarian.defeated = true;               // Beaten before the player (last) came onto Floor 3...
        p.setCheckpoint();                       // ...as if they'd gone back up and come down again
        int textbooks = p.items.textbooks, passes = p.items.hallPasses;
        pens = p.items.redPens;
        gp.endBattle(undead, new BattleResult(true, 55, 2));
        putPx(f3.boss.room, f3.boss.x, f3.boss.y); started = null; ticks(100);   // After the grace period
        check("the Whisperer: walking up to it starts its battle", started == f3.boss);
        gp.endBattle(f3.boss, new BattleResult(true, 40, 1));
        started = null; ticks(70);
        check("won: it drops the East Stairwell Key, +1 kit, +1 Textbook, +1 Hall Pass",
                p.keys.contains("East Stairwell Key") && p.kits == 2 && p.items.textbooks == textbooks + 1 && p.items.hallPasses == passes + 1);
        Monster worm = libWest.monsters.get(0);
        put(libWest, 10, 12);
        gp.endBattle(worm, new BattleResult(false, 0, 0));
        check("gave up: back at the foot of the stairs the player came down", gp.currentRoom == f3.upStairs()
                && p.x == p.checkpointX && p.y == p.checkpointY);
        check("gave up: health exactly as it was on arrival (80, not full: " + p.health + ")", p.health == 80);
        check("gave up: kits and items as on arrival (4 kits)", p.kits == 4 && p.items.textbooks == textbooks
                && p.items.redPens == pens && p.items.hallPasses == passes);
        check("gave up: the East Stairwell Key is gone, the West one kept",
                !p.keys.contains("East Stairwell Key") && p.keys.contains("West Stairwell Key"));
        check("gave up: the monsters beaten since (the Undead, the Whisperer) are back, with the key",
                !undead.defeated && !f3.boss.defeated && !f3.boss.lootTaken);
        check("gave up: the Librarian, beaten before, stays beaten", librarian.defeated);
        check("gave up: the floor above stays as it was (the Brute stays beaten)", f4.boss.defeated && f4.boss.lootTaken);

        System.out.println("7. Every floor's way out is locked until the player has the key its boss drops");
        Room f3east = f3.find("East Stairs");
        putPx(f3east, f3east.map.stairs.downFoot.x, f3east.map.stairs.downFoot.y); hold("U", 200);
        check("Floor 3: without the East Stairwell Key, the east stairs don't take you down", gp.currentRoom.floor == f3
                && gp.notice().equals(f3.lockedNote));
        takeBossKey(f3, "East Stairwell Key");
        putPx(f3east, f3east.map.stairs.downFoot.x, f3east.map.stairs.downFoot.y); hold("U", 60);
        check("Floor 3: with it, they do", gp.currentRoom.floor == f2);
        Room f2down = f2.downStairs();
        putPx(f2down, f2down.map.stairs.downFoot.x, f2down.map.stairs.downFoot.y); hold("U", 200);
        check("Floor 2: without the Security Keycard, the west stairs stay in lockdown", gp.currentRoom.floor == f2
                && gp.notice().equals(f2.lockedNote));
        takeBossKey(f2, "Security Keycard");
        putPx(f2down, f2down.map.stairs.downFoot.x, f2down.map.stairs.downFoot.y); hold("U", 60);
        check("Floor 2: with it, they open", gp.currentRoom.floor == f1);
        Room hall = f1.find("Hallway (Principal's Office)");
        put(hall, 7, 10); hold("D", 120);
        check("Floor 1: without the Master Key, the main entrance stays shut", gp.currentRoom == hall
                && gp.notice().equals(f1.lockedNote));
        takeBossKey(f1, "Master Key");
        put(hall, 7, 10); hold("D", 120);
        check("Floor 1: with it, it leads outside", gp.currentRoom == gp.world.quadrangle);

        System.out.println("8. The battles themselves");
        int words = BattleScreen.dictionary().size();
        check("the word list is loaded (" + words + " words)", words > 100_000);
        for (EnemyDef e : BattleScreensDebug.ALL) {
            boolean sprites = BattleScreen.Sprites.get(e.sprite(), true) != null
                    && BattleScreen.Sprites.get(e.sprite() + "_attack", false) != null
                    && BattleScreen.Sprites.get(e.sprite() + "_hurt", false) != null;
            BufferedImage shot = BattleScreen.preview(e, 60);
            check(e.name() + ": its 3 sprites load and its battle screen draws", sprites && shot.getWidth() == 800);
        }

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }

    // Walks up to a floor's boss, beats it, and stands on what it drops; checks the key it gave
    static void takeBossKey(Floor f, String key) {
        putPx(f.boss.room, f.boss.x, f.boss.y); started = null; ticks(60);
        boolean fought = started == f.boss;
        gp.endBattle(f.boss, new BattleResult(true, p.health, p.kits));
        started = null; ticks(70);
        check("Floor " + f.number + ": " + f.boss.enemy.name() + " (" + f.boss.room.name + ") fights, then drops the " + key,
                fought && p.keys.contains(key) && key.equals(f.exitKey));
    }

    static void putPx(Room room, int x, int y) { gp.currentRoom = room; p.x = x; p.y = y; p.update(); }

    // A stand-in player far off the map, so a monster patrols without noticing anyone
    static Player farAway() {
        Player far = new Player(gp);
        far.solidArea.setLocation(-10_000, -10_000);
        return far;
    }
}
