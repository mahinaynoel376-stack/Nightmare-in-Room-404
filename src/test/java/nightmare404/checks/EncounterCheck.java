package nightmare404.checks;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import nightmare404.battle.BattleScreen;
import nightmare404.battle.BattleScreen.Attack;
import nightmare404.battle.BattleScreen.BattleResult;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.battle.BattleScreen.Items;
import nightmare404.battle.FourthFloorEnemies;
import nightmare404.battle.LobbyEnemies;
import nightmare404.battle.QuadrangleEnemies;
import nightmare404.battle.TypingBattle;
import nightmare404.battle.TypingBattle.Duel;
import nightmare404.debug.BattleScreensDebug;
import nightmare404.entity.Clue;
import nightmare404.entity.SleepingBoss;
import nightmare404.entity.SubjectZero;
import nightmare404.entity.Haze;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.entity.TreeMonster;
import nightmare404.entity.Whisperer;
import nightmare404.main.GamePanel;
import nightmare404.map.Quadrangle;
import nightmare404.tile.TileMap;
import nightmare404.tile.TileSet;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// The monsters and their battles: where they are, how they move, what touching one does, winning
// and losing (giving up puts the floor back as the player found it), the bosses (the Brute on the
// stairs), the keys they drop and the locked ways out, the quadrangle's trees and their typing race,
// and that every battle's pictures and words load.
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
        gp.world.firstFight.defeated = true;    // The first fight, outside Room 404, is QuestCheck's
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
        int hallwayMonsters = 0;
        for (Room r : rooms) {
            long regular = r.monsters.stream().filter(m -> !m.boss).count();
            if (r.floor.indexOf(r) >= 0) {           // The hallway list: hallway screens and stairwells
                hallwayMonsters += regular;
                if (regular > 0) check(r.name + " (Floor " + r.floor.number + "): one monster patrolling", regular == 1
                        && r.name.startsWith("Hallway"));
            } else if (regular > 0 && !r.outdoors) {     // (The quadrangle's trees: see section 8)
                check(r.name + ": " + regular + " monsters (2 or 3)", regular >= 2 && regular <= 3);
            }
        }
        check(hallwayMonsters + " hallway screens have a monster (2 a floor)", hallwayMonsters == 8);
        check("the hallway outside Room 404 is safe", gp.world.floor4.find("Hallway (Room 404)").monsters.isEmpty());
        check("so are the library's entrance and the main entrance", gp.world.floor3.find("Hallway (Library Entrance)").monsters.isEmpty()
                && gp.world.floor1.find("Hallway (Principal's Office)").monsters.isEmpty());
        for (Monster m : gp.world.monsters) {
            if (m.room.floor.indexOf(m.room) >= 0 && !m.boss) {
                check(m.enemy.name() + " in " + m.room.name + ": its battle's sign says " + m.enemy.room(),
                        m.enemy.room().equals(m.room.floor.number + "F HALLWAY"));
            }
        }
        check(gp.world.monsters.size() + " monsters in all", gp.world.monsters.size() >= 20);

        System.out.println("2. Every monster stands somewhere it can walk, away from the doors, and walks its whole route");
        for (Monster m : gp.world.monsters) {
            if (m instanceof TreeMonster) continue;      // Asleep inside a tree: see section 8
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

        System.out.println("3b. Monsters can't see through walls, shelves or desks");
        gp.inBattle = false;                                // (Out of the Desk Mimic's battle for a moment)
        Room stacks = gp.world.floor3.find("Library (West)");
        Monster shelfWorm = stacks.monsters.get(0);         // A Bookworm, along the aisle on row 10
        List<Monster> others = new ArrayList<>();
        for (Monster m : stacks.monsters) if (m != shelfWorm && !m.defeated) { others.add(m); m.defeated = true; }
        shelfWorm.reset();
        put(stacks, 5, 8);                                  // 2 tiles away, a row of shelves in between
        boolean hidden = !shelfWorm.canSee(p);
        started = null; ticks(90);
        check("2 tiles away but behind a row of shelves: the Bookworm doesn't see the player",
                hidden && !shelfWorm.chasing && started == null);
        shelfWorm.reset();
        put(stacks, 7, 10); started = null; ticks(90);     // In the open, in its aisle
        check("in the open, in its aisle: it sees the player and comes after them", started == shelfWorm);
        gp.inBattle = false;
        shelfWorm.reset();
        for (Monster m : others) { m.defeated = false; m.reset(); }

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

        System.out.println("5. The Brute sleeps on the stairs down as a boulder, wakes for the janitor's pages, and beaten, clears the way");
        Floor f4 = gp.world.floor4, f3 = gp.world.floor3, f2 = gp.world.floor2, f1 = gp.world.floor1;
        Room f4west = f4.find("West Stairs");
        check("the Brute is on Floor 4's way down (" + f4.boss.room.name + ")", f4.boss.room == f4west && f4west == f4.downStairs());
        Point foot = f4west.map.stairs.downFoot;
        SleepingBoss golem = (SleepingBoss) f4.boss;
        check("it starts asleep, as a boulder", golem.asleep());
        String[] sides = {"in the middle of the flight", "at its left edge", "at its right edge"};
        int[] xs = {foot.x, 5 * 32 - 8, 6 * 32 + 8};
        for (int i = 0; i < 3; i++) {
            putPx(f4west, xs[i], foot.y); ticks(40);
            hold("U", 150);
            check("climbing " + sides[i] + " without the pages: the boulder blocks the way, and there's no battle",
                    started == null && golem.asleep() && p.y > golem.y && gp.currentRoom.floor == f4
                    && gp.notice().contains("won't budge"));
        }
        check("while it's there, Floor 4's way down is shut", f4.locked(p.keys) && f4.lockedNote != null);
        for (Clue c : f4.quest.clues) p.clues.add(c.id);      // Both of the janitor's pages
        putPx(f4west, foot.x, foot.y); ticks(40); hold("U", 150);
        check("with both pages, Flor reads the logbook to it, it wakes up, and its battle starts", started == f4.boss
                && !golem.asleep() && !golem.waking() && gp.dialogue.said("Let me read you your own words"));
        gp.endBattle(f4.boss, new BattleResult(false, 0, p.kits));
        check("gave up against it: it's a boulder again, and the pages are lost again", golem.asleep()
                && !f4.quest.complete(p.clues) && gp.currentRoom.name.equals("Room 404"));
        for (Clue c : f4.quest.clues) p.clues.add(c.id);
        started = null;
        putPx(f4west, foot.x, foot.y); ticks(40); hold("U", 150);
        check("  read to again, it wakes again", started == f4.boss);
        gp.endBattle(f4.boss, new BattleResult(true, 80, 3));
        check("won: it drops nothing, and Flor sees the way down is clear", f4.boss.defeated && f4.boss.loot == null
                && gp.dialogue.said("The stairs down are clear") && gp.notice().contains("way down is clear"));
        check("the way down is open, with no key", !f4.locked(p.keys) && p.keys.isEmpty());
        started = null;
        hold("U", 40);                           // Long enough to climb the flight, not to come back up
        check("the stairs take the player down to Floor 3", gp.currentRoom.floor == f3);
        check("arriving on a new floor moves the checkpoint there", p.checkpointRoom == gp.currentRoom);

        System.out.println("6. Giving up puts the floor back as the player found it");
        Room libEntrance = f3.find("Library (Entrance)"), libWest = f3.find("Library (West)");
        Monster librarian = libEntrance.monsters.get(0), undead = libEntrance.monsters.get(1);
        librarian.defeated = true;               // Beaten before the player (last) came onto Floor 3...
        p.setCheckpoint();                       // ...as if they'd gone back up and come down again
        int arrivedKits = p.kits, textbooks = p.items.textbooks, passes = p.items.hallPasses, pens = p.items.redPens;
        gp.endBattle(undead, new BattleResult(true, 55, 2));
        Whisperer whisperer = (Whisperer) f3.boss;
        Haze haze = f3.find("East Stairs").haze;
        putPx(f3.boss.room, f3.boss.x, f3.boss.y); started = null; ticks(100);   // After the grace period
        check("the Whisperer, without the checkout cards: intangible, mumbling blanks, no battle", started == null
                && whisperer.intangible() && gp.dialogue.said("blacked out") && gp.notice().contains("Checkout cards found: 0 of 3"));
        for (Clue c : f3.quest.clues) p.clues.add(c.id);      // All three checkout cards
        put(f3.boss.room, 4, 14); ticks(5);                    // Away from her...
        putPx(f3.boss.room, f3.boss.x, f3.boss.y); ticks(150); // ...and back with the cards
        check("with all three cards, Flor reads her the names, she turns solid, and her battle starts", started == f3.boss
                && !whisperer.intangible() && !whisperer.solidifying() && gp.dialogue.said("I'll read every name with you"));
        check("  while she's there, a haze covers the east stairs", haze.active());
        gp.endBattle(f3.boss, new BattleResult(true, 40, 1));
        check("won: the haze over the east stairs clears", !haze.active() && gp.dialogue.said("haze over the stairs thins"));
        started = null; ticks(70);
        check("she drops the three checkout cards, every name filled in, +1 kit, +1 Textbook, +1 Hall Pass",
                p.keys.contains("The three checkout cards") && gp.dialogue.said("every name, written out in full")
                && p.kits == 2 && p.items.textbooks == textbooks + 1 && p.items.hallPasses == passes + 1);
        Monster worm = libWest.monsters.get(0);
        put(libWest, 10, 12);
        gp.endBattle(worm, new BattleResult(false, 0, 0));
        check("gave up: back at the foot of the stairs the player came down", gp.currentRoom == f3.upStairs()
                && p.x == p.checkpointX && p.y == p.checkpointY);
        check("gave up: health exactly as it was on arrival (80, not full: " + p.health + ")", p.health == 80);
        check("gave up: kits and items as on arrival (" + arrivedKits + " kits)", p.kits == arrivedKits
                && p.items.textbooks == textbooks && p.items.redPens == pens && p.items.hallPasses == passes);
        check("gave up: the cards she dropped are gone, and so are the checkout cards found since",
                !p.keys.contains("The three checkout cards") && f3.quest.found(p.clues) == 0);
        check("gave up: the monsters beaten since (the Undead, the Whisperer) are back; she's intangible again, the haze is back",
                !undead.defeated && !f3.boss.defeated && !f3.boss.lootTaken && whisperer.intangible() && haze.active());
        check("gave up: the Librarian, beaten before, stays beaten", librarian.defeated);
        check("gave up: the floor above stays as it was (the Brute stays beaten)", f4.boss.defeated);

        System.out.println("7. Every floor's way out stays shut until its boss is beaten (or the player has the key it drops)");
        Room f3east = f3.find("East Stairs");
        Point f3foot = f3east.map.stairs.downFoot;
        putPx(f3east, f3foot.x, f3foot.y); hold("U", 200);
        check("Floor 3: with the Whisperer still there, the haze won't let the player onto the east stairs",
                gp.currentRoom.floor == f3 && p.y >= f3foot.y - 16 && gp.notice().contains("haze won't let you through")
                && gp.dialogue.said("It wants me to go back to the library"));
        for (Clue c : f3.quest.clues) p.clues.add(c.id);
        putPx(f3.boss.room, f3.boss.x, f3.boss.y); started = null; ticks(150);
        boolean fought = started == f3.boss;
        gp.endBattle(f3.boss, new BattleResult(true, p.health, p.kits));
        check("Floor 3: with the cards the Whisperer turns solid and is beaten, and the haze is gone", fought && !haze.active());
        putPx(f3east, f3foot.x, f3foot.y); hold("U", 60);
        check("Floor 3: then the east stairs take you down", gp.currentRoom.floor == f2);
        check("Floor 2: no lockdown, the way down is open from the start", !f2.locked(p.keys));
        Room contHall = f2.find("Hallway (Containment Lab)"), containment = f2.find("Containment Lab");
        Rectangle cdoor = null;
        for (int i = 0; i < contHall.map.doorCount(); i++) if (contHall.doorLeadsTo(i) == containment) cdoor = contHall.map.door(i);
        putPx(contHall, cdoor.x + 8, cdoor.y + 64); hold("U", 60);
        check("Floor 2: the Containment Lab is locked without its keycard", gp.currentRoom == contHall
                && gp.notice().equals(containment.lockedNote) && gp.dialogue.said("keycard reader"));
        Room chemistry = f2.find("Chemistry Lab");
        Monster assistant = null;
        for (Monster m : chemistry.monsters) {
            if (m.enemy.name().equals("Lab Assistant")) assistant = m; else m.defeated = true;
        }
        check("the Lab Assistant carries it", assistant.loot != null && assistant.loot.key().equals("Containment Keycard"));
        gp.endBattle(assistant, new BattleResult(true, p.health, p.kits));
        check("  beaten, it drops it", assistant.defeated && gp.notice().contains("glints"));
        putPx(chemistry, assistant.x, assistant.y); started = null; ticks(80);
        check("  picked up: the keycard, and Flor wonders whether to open that door", p.keys.contains("Containment Keycard")
                && gp.dialogue.said("Do I really want to open that door"));
        putPx(contHall, cdoor.x + 8, cdoor.y + 64); hold("U", 60);
        check("with the keycard, the Containment Lab opens", gp.currentRoom == containment);
        SleepingBoss specimen = (SleepingBoss) f2.boss;
        check("Test Subject X is still asleep in its pod, which blocks the way", specimen.asleep()
                && specimen.blocks(new Rectangle(specimen.x + 8, specimen.y + 16, 16, 16)));
        putPx(containment, specimen.x, specimen.y + 64); started = null; hold("U", 200);
        check("walking up to the pod: it bursts out, and its battle starts", started == f2.boss && !specimen.asleep()
                && gp.dialogue.said("The glass cracks"));
        gp.endBattle(f2.boss, new BattleResult(true, p.health, p.kits));
        started = null; ticks(80);
        check("beaten, it leaves one of its tentacles: its brother Subject Zero's calm-down", p.keys.contains(SubjectZero.CALMING_ITEM)
                && gp.dialogue.said("Subject Zero is X's brother"));
        Room f2down = f2.downStairs();
        putPx(f2down, f2down.map.stairs.downFoot.x, f2down.map.stairs.downFoot.y); hold("U", 60);
        check("Floor 2: down the west stairs", gp.currentRoom.floor == f1);
        Room hall = f1.find("Hallway (Principal's Office)");
        put(hall, 7, 10); hold("D", 120);
        check("Floor 1: without the Master Key, the main entrance stays shut", gp.currentRoom == hall
                && gp.notice().equals(f1.lockedNote));
        Room office = f1.find("Principal's Office");
        Rectangle odoor = null;
        for (int i = 0; i < hall.map.doorCount(); i++) if (odoor == null && hall.doorLeadsTo(i) == office) odoor = hall.map.door(i);
        putPx(hall, odoor.x + 8, odoor.y + 64); hold("U", 60);
        check("Floor 1: the Principal won't see Flor without the floor's three books", gp.currentRoom == hall
                && gp.dialogue.said("I do not see students without an appointment") && gp.notice().contains("Books found: 0 of 3"));
        for (Clue c : f1.quest.clues) p.clues.add(c.id);
        putPx(hall, odoor.x + 8, odoor.y + 64); hold("U", 60);
        check("Floor 1: with all three, Flor recites what she's learned, and the office door opens", gp.currentRoom == office
                && gp.dialogue.said("Is that smart enough for you"));
        takeBossKey(f1, "Master Key");
        put(hall, 7, 10); hold("D", 120);
        check("Floor 1: with it, it leads outside", gp.currentRoom == gp.world.quadrangle);

        System.out.println("8. The quadrangle's trees wake when you come outside, and come after you");
        Room quad = gp.world.quadrangle;
        List<TreeMonster> trees = new ArrayList<>();
        for (Monster m : quad.monsters) if (m instanceof TreeMonster t) trees.add(t);
        check(trees.size() + " trees in the quadrangle (fewer than before: 13), and every one is a monster",
                trees.size() == 6 && trees.size() == Quadrangle.trees().size()
                && quad.monsters.size() == trees.size() + 1 && quad.monsters.contains(gp.world.finalBoss));
        check("each one stands where a tree is drawn on the map", trees.stream().allMatch(EncounterCheck::treeOnMap));
        put(hall, 7, 10); gp.update();               // Back inside, then out again through the main entrance
        put(quad, 24, 5); started = null; ticks(1);
        check("just outside, they're all still asleep: just trees",
                trees.stream().allMatch(t -> t.asleep() && treeOnMap(t) && !t.touches(p)));
        ticks(80);
        check("a moment later, they still are", trees.stream().allMatch(TreeMonster::asleep));
        TreeMonster nearest = trees.get(0);
        for (TreeMonster t : trees) if (dist(t) < dist(nearest)) nearest = t;
        int waited = 81;
        while (nearest.asleep() && waited < 1000) { gp.update(); waited++; }
        check("then the nearest wakes (" + waited + " frames after coming out) and pops out", nearest.popping());
        check("  its tree leaves the map, and the ground where its trunk stood is torn up", !treeOnMap(nearest)
                && quad.map.tileAt(TileMap.LAYER_OBJECTS, nearest.treeCol(), nearest.treeRow() + 1) == TileSet.CRACK
                && !quad.map.isSolid(nearest.treeCol(), nearest.treeRow() + 1));
        check("  the ones further off are still asleep: they wake one after another", trees.stream().anyMatch(TreeMonster::asleep));
        check("  while it's popping out it can't grab anyone", !nearest.hunting());
        for (int i = 0; i < TreeMonster.POP_TICKS; i++) gp.update();
        check("after popping out it hunts the player", nearest.hunting() && nearest.chasing);
        double before = dist(nearest);
        for (int i = 0; i < 20; i++) gp.update();
        check("  it comes closer (" + (int) before + " -> " + (int) dist(nearest) + " px)", dist(nearest) < before);
        int hpBefore = p.health;
        started = null;
        gp.lastStand.lastHitBy = null;
        for (int i = 0; i < 1500 && gp.lastStand.lastHitBy == null; i++) gp.update();
        check("a tree reaches the player and hits them, out on the map (" + hpBefore + " -> " + p.health + " HP)",
                gp.lastStand.lastHitBy instanceof TreeMonster && p.health < hpBefore);
        check("  no battle screen opens: the quadrangle is fought on the map (see QuestCheck)", started == null && !gp.inBattle);
        check("  (a tree that did start a battle would get the typing race; everyone else gets the word battle)",
                GamePanel.typingFight(nearest) && !GamePanel.typingFight(f4.boss) && !GamePanel.typingFight(mimic));
        TreeMonster caught = (TreeMonster) gp.lastStand.lastHitBy;
        check("  it reels for a moment after hitting", caught.stunned > 0);
        gp.lastStand.fell(caught);
        check("felled: it's beaten, and doesn't hunt any more", caught.defeated && !caught.hunting());
        put(hall, 7, 10); gp.update();               // Inside, then back out
        put(quad, 24, 5); started = null; ticks(1);
        check("coming back, every tree still standing is back in its place, asleep",
                trees.stream().filter(t -> !t.defeated).allMatch(t -> t.asleep() && treeOnMap(t)));
        check("  and the beaten one stays gone", caught.defeated && !treeOnMap(caught));
        gp.lastStand.lastHitBy = null;
        for (int i = 0; i < 3000 && gp.lastStand.lastHitBy == null; i++) gp.update();
        check("the others wake again, and one reaches the player", gp.lastStand.lastHitBy instanceof TreeMonster
                && gp.lastStand.lastHitBy != caught);
        p.health = 0;
        gp.loseOutside();
        check("going down out there: back at the main entrance, where the player came out",
                gp.currentRoom == quad && p.checkpointRoom == quad && p.x == p.checkpointX && p.y == p.checkpointY);
        check("  with at least half health (" + p.health + ")", p.health >= 50);
        check("  the felled tree stays felled (every one felled outside is kept)", caught.defeated && !treeOnMap(caught));
        check("  the others are back in their places, asleep", trees.stream().filter(t -> !t.defeated)
                .allMatch(t -> t.asleep() && treeOnMap(t)));

        System.out.println("9. The typing race");
        EnemyDef testOak = new EnemyDef("Test Oak", "treant", 60, "QUADRANGLE", "",
                List.of(new Attack("Swipe", 10, 0, 0, 1, "It swipes.")), QuadrangleEnemies.HOLLOW_OAK.backdrop());
        Items items = new Items(1, 1, 1);
        Duel d = new Duel(testOak, 80, 2, items, 42);
        check("it starts with a countdown", d.state == Duel.State.READY && d.word.isEmpty());
        toTyping(d);
        check("then a word to type: \"" + d.word + "\" (3-6 letters while the tree is unhurt)", d.word.length() >= 3 && d.word.length() <= 6);
        int foe = d.foeHp, len = d.word.length();
        typeWord(d);
        check("typing it hits the tree: " + len + " letters x 2.5, x1.5 for being quick = " + (foe - d.foeHp),
                foe - d.foeHp == (int) Math.round(len * 2.5 * 1.5) && d.streak == 1);
        toTyping(d);
        int time = d.timeLeft;
        char wrong = d.word.charAt(0) == 'z' ? 'q' : 'z';
        d.type(wrong);
        check("a wrong key: no progress, the streak is broken, and it costs a quarter of a second",
                d.typed == 0 && d.streak == 0 && time - d.timeLeft == 15);
        d.type('7');
        check("  (keys that aren't letters don't count)", d.typed == 0 && time - d.timeLeft == 15);
        int hp = d.heroHp;
        while (d.state == Duel.State.TYPING) d.tick();
        check("time runs out: the tree hits back (10)", hp - d.heroHp == 10);
        toTyping(d);
        foe = d.foeHp;
        check("an Old Textbook is thrown at the tree: 15 damage straight away", d.useTextbook() && items.textbooks == 0
                && foe - d.foeHp == 15);
        toTyping(d);
        hp = d.heroHp;
        check("a First Aid Kit heals 30 (no more than full)", d.useKit() && d.heroHp == Math.min(100, hp + 30) && d.kits == 1);
        String skipped = d.word;
        hp = d.heroHp;
        check("a Hall Pass skips the word without being hit", d.useHallPass() && !d.word.equals(skipped)
                && d.state == Duel.State.TYPING && d.heroHp == hp && items.hallPasses == 0);
        check("a Red Pen...", d.useRedPen() && items.redPens == 0);
        foe = d.foeHp; len = d.word.length();
        typeWord(d);
        check("  ...doubles the next word (" + (foe - d.foeHp) + ")", foe - d.foeHp == Math.min(foe, (int) Math.round(len * 2.5 * 1.5 * 2)));
        int typedWords = 2;
        while (d.state != Duel.State.WON && typedWords < 40) { toTyping(d); if (d.state == Duel.State.TYPING) { typeWord(d); typedWords++; } }
        for (int i = 0; i < 30; i++) d.tick();
        check("type fast enough and the tree falls (" + typedWords + " words)", d.state == Duel.State.WON && d.result().won()
                && d.result().heroHp() == d.heroHp && d.result().kitsLeft() == 1);
        Items few = new Items(0, 0, 0);
        Duel slow = new Duel(testOak, 15, 0, few, 7);
        for (int i = 0; i < 5000 && slow.state != Duel.State.LOST; i++) slow.tick();
        check("type nothing and the tree wins", slow.state == Duel.State.LOST && !slow.result().won() && slow.result().heroHp() == 0);
        slow.restart();
        check("  Try Again starts the fight over", slow.state == Duel.State.READY && slow.heroHp == 15 && slow.foeHp == 60);
        check("the word list is loaded (" + TypingBattle.words().size() + " words)", TypingBattle.words().size() > 100);
        boolean treeSprites = BattleScreen.Sprites.get("treant", true) != null && BattleScreen.Sprites.get("treant_attack", false) != null
                && BattleScreen.Sprites.get("treant_hurt", false) != null;
        check("the Hollow Oak's 3 sprites load and its typing screen draws",
                treeSprites && TypingBattle.preview(QuadrangleEnemies.HOLLOW_OAK, 200).getWidth() == 800);

        System.out.println("10. The word battles");
        int words = BattleScreen.dictionary().size();
        check("the word list is loaded (" + words + " words)", words > 100_000);
        for (EnemyDef e : BattleScreensDebug.ALL) {
            boolean sprites = BattleScreen.Sprites.get(e.sprite(), true) != null
                    && BattleScreen.Sprites.get(e.sprite() + "_attack", false) != null
                    && BattleScreen.Sprites.get(e.sprite() + "_hurt", false) != null;
            BufferedImage shot = BattleScreen.preview(e, 60);
            check(e.name() + ": its 3 sprites load and its battle screen draws", sprites && shot.getWidth() == 800);
        }

        System.out.println("11. The word battles' turn timer: lenient for weak monsters, harsh for strong ones");
        List<EnemyDef> byHp = new ArrayList<>(BattleScreensDebug.ALL);
        byHp.sort(Comparator.comparingInt(EnemyDef::maxHp));
        boolean ordered = true;
        int previous = Integer.MAX_VALUE;
        StringBuilder table = new StringBuilder();
        for (EnemyDef e : byHp) {
            int secs = BattleScreen.turnSeconds(e);
            if (secs > previous) ordered = false;
            previous = secs;
            table.append(table.length() == 0 ? "" : ", ").append(e.name()).append(" ").append(secs).append("s");
        }
        System.out.println("       " + table);
        check("a stronger monster never gives more time than a weaker one", ordered);
        check("the 4th floor's monsters give 40-45 seconds a turn", List.of(FourthFloorEnemies.DESK_MIMIC, FourthFloorEnemies.CHALK_WRAITH,
                FourthFloorEnemies.CRAWLER, FourthFloorEnemies.HALL_MONITOR).stream().allMatch(e -> BattleScreen.turnSeconds(e) >= 40));
        check("the Principal gives only 15", BattleScreen.turnSeconds(LobbyEnemies.PRINCIPAL) == 15);
        check("doing nothing: the Desk Mimic waits out its 45 seconds...", BattleScreen.enemyTurnsWhileIdle(FourthFloorEnemies.DESK_MIMIC, 44) == 0);
        check("  ...then takes its turn", BattleScreen.enemyTurnsWhileIdle(FourthFloorEnemies.DESK_MIMIC, 47) == 1);
        check("the Principal attacks after 15 seconds, and again 15 seconds after that",
                BattleScreen.enemyTurnsWhileIdle(LobbyEnemies.PRINCIPAL, 14) == 0
                        && BattleScreen.enemyTurnsWhileIdle(LobbyEnemies.PRINCIPAL, 17) == 1
                        && BattleScreen.enemyTurnsWhileIdle(LobbyEnemies.PRINCIPAL, 34) == 2);

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

    // True if the tree's own tiles are on the map where it stands
    static boolean treeOnMap(TreeMonster t) {
        return t.room.map.tileAt(TileMap.LAYER_OBJECTS, t.treeCol(), t.treeRow()) == TileSet.TREE_FIRST
                && t.room.map.isSolid(t.treeCol(), t.treeRow() + 1);
    }

    static double dist(Monster m) {
        return Point.distance(m.x, m.y, p.x, p.y);
    }

    // Typing race helpers: wait for the next word; type the whole word
    static void toTyping(Duel d) {
        for (int i = 0; i < 400 && d.state != Duel.State.TYPING && d.state != Duel.State.WON && d.state != Duel.State.LOST; i++) d.tick();
    }

    static void typeWord(Duel d) {
        for (char c : d.word.toCharArray()) d.type(c);
    }

    // A stand-in player far off the map, so a monster patrols without noticing anyone
    static Player farAway() {
        Player far = new Player(gp);
        far.solidArea.setLocation(-10_000, -10_000);
        return far;
    }
}
