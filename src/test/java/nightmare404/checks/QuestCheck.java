package nightmare404.checks;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nightmare404.battle.LastStand;
import nightmare404.entity.Clue;
import nightmare404.entity.Hunter;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.entity.SubjectZero;
import nightmare404.entity.TreeMonster;
import nightmare404.main.GamePanel;
import nightmare404.story.Line;
import nightmare404.story.Quest;
import nightmare404.story.Quests;
import nightmare404.tile.TileSet;
import nightmare404.ui.DialogueBox;
import nightmare404.world.Floor;
import nightmare404.world.Room;
import nightmare404.world.World;

// The story, the quests and the fight outside:
//   the dialogue box (it pauses the world, types out, and goes on when a key is pressed), the clues
//   (where they lie, picking them up, how they weaken each floor's boss, giving up putting them back),
//   the bosses reacting to what the player knows, the journal; then the quadrangle, fought on the map:
//   focusing (the world slows, the player stands still, focus runs out), typing a monster's word to
//   strike it, strikes stopped by walls but not from right behind cover, the items, felling the trees,
//   Subject Zero asleep then awake, its roots and saplings, and the two endings.
// No window opens: dialogue closes as soon as it opens (DialogueBox.autoSkip), except where a test turns
// that off to try the box itself, and starting a battle just remembers which monster it was.
public class QuestCheck {
    static GamePanel gp; static Player p; static World w; static LastStand stand; static int fails = 0;

    static void check(String what, boolean ok) {
        if (ok) {
            System.out.println("  ok   " + what);
        } else {
            System.out.println("  FAIL " + what);
            fails++;
        }
    }
    static void put(Room room, int col, int row) { gp.currentRoom = room; p.x = col * 32; p.y = row * 32; p.update(); }
    static void putPx(Room room, int x, int y) { gp.currentRoom = room; p.x = x; p.y = y; p.update(); }
    static void ticks(int n) { for (int i = 0; i < n && gp.battleMonster == null; i++) gp.update(); }
    static void hold(String keys, int n) {
        gp.battleMonster = null;
        gp.upPressed = keys.contains("U"); gp.downPressed = keys.contains("D"); gp.leftPressed = keys.contains("L"); gp.rightPressed = keys.contains("R");
        ticks(n);
        gp.upPressed = false;
        gp.downPressed = false;
        gp.leftPressed = false;
        gp.rightPressed = false;
    }
    static nightmare404.battle.BattleScreen.BattleResult won() { return new nightmare404.battle.BattleScreen.BattleResult(true, p.health, p.kits); }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.flor; w = gp.world; stand = gp.lastStand;
        Floor f4 = w.floor4, f3 = w.floor3, f1 = w.floor1;

        System.out.println("1. The opening");
        ticks(1);
        check("the game opens with the intro in Room 404", gp.dialogue.said("STAY IN YOUR SEAT") && gp.currentRoom.name.equals("Room 404"));
        check("...which explains the controls and the clues", gp.dialogue.said("J opens your journal") && gp.dialogue.said("20%"));

        System.out.println("2. The dialogue box");
        DialogueBox box = new DialogueBox();
        box.autoSkip = false;
        box.open(List.of(Line.narrate("First line."), Line.flor("Second.")));
        check("a conversation opens", box.isOpen());
        box.update(true);
        check("a key press right as it opens doesn't skip anything", box.isOpen());
        for (int i = 0; i < 20; i++) box.update(false);
        box.update(true);
        check("pressing goes on to the next line", box.isOpen());
        box.update(true);
        check("pressing while it types shows the whole line, without going on", box.isOpen());
        box.update(true);
        check("pressing on the last line closes it", !box.isOpen());
        gp.dialogue.autoSkip = false;
        gp.dialogue.open(List.of(Line.narrate("Wait.")));
        int x0 = p.x, y0 = p.y;
        hold("D", 40);
        check("while it's open the world waits: holding S doesn't move the player", p.x == x0 && p.y == y0 && gp.dialogue.isOpen());
        while (gp.dialogue.isOpen()) gp.dialogue.update(true);
        gp.dialogue.autoSkip = true;

        System.out.println("3. Every floor has a quest, with two clues in its side rooms");
        List<Clue> all = Quests.all(w);
        check("8 clues in all", all.size() == 8);
        for (Floor f = w.top; f != null; f = f.below) {
            Quest q = f.quest;
            String quest = "no quest";
            if (q != null) quest = q.title + ", " + q.clues.size() + " clues";
            check("Floor " + f.number + ": " + quest, q != null && q.clues.size() == 2);
            if (q == null) continue;
            for (Clue c : q.clues) {
                String where = c.id + " (" + c.title + ", " + c.room.name + ")";
                double nearest = Double.MAX_VALUE;
                for (int i = 0; i < c.room.map.doorCount(); i++) {
                    Rectangle d = c.room.map.door(i);
                    nearest = Math.min(nearest, Point.distance(d.getCenterX(), d.getCenterY(), c.area().getCenterX(), c.area().getCenterY()));
                }
                check(where + ": on a walkable tile, 4+ tiles from the door, in a side room",
                        !c.room.map.collides(c.area()) && nearest >= 4 * 32 && !c.room.name.startsWith("Hallway")
                        && c.room != f.boss.room && c.room.clues.contains(c));
            }
        }

        System.out.println("4. Picking clues up weakens the floor's boss");
        Monster brute = f4.boss;
        check("with no clues the Brute starts at full health (90)", gp.director.foeStartHp(brute) == 90);
        Room r402 = f4.find("Room 402"), r403 = f4.find("Room 403");
        for (Monster m : r402.monsters) m.defeated = true;    // Nobody in the way
        Clue page1 = r402.clues.get(0), page2 = r403.clues.get(0);
        int kits = p.kits;
        p.setCheckpoint();
        putPx(r402, page1.x, page1.y); ticks(1);
        check("walking over the page picks it up, and the player reads it", p.clues.contains(page1.id)
                && gp.dialogue.said("Kids keep staying late in 404"));
        check("...finds a first aid kit with it (" + p.kits + "), and is told the Brute is weaker",
                p.kits == kits + 1 && gp.notice().contains("20% weaker"));
        check("one clue: the Brute starts at 80% (72)", gp.director.foeStartHp(brute) == 72);
        Monster mimic = r402.monsters.get(0);
        mimic.defeated = false;
        gp.endBattle(mimic, new nightmare404.battle.BattleScreen.BattleResult(false, 0, 0));
        check("giving up after a lost battle puts the clue back", !p.clues.contains(page1.id) && p.kits == kits);
        putPx(r402, page1.x, page1.y); ticks(1);
        check("picked up again, it says so in one line instead of the whole page", p.clues.contains(page1.id)
                && gp.dialogue.said("You pick up the torn logbook page again."));
        for (Monster m : r403.monsters) m.defeated = true;
        putPx(r403, page2.x, page2.y); ticks(1);
        check("both pages: the Brute starts at 60% (54)", gp.director.foeStartHp(brute) == 54 && f4.quest.complete(p.clues));
        check("the other bosses aren't weakened by Floor 4's pages", gp.director.foeStartHp(f3.boss) == f3.boss.enemy.maxHp());

        System.out.println("5. The bosses react to what the player knows");
        Room west = f4.find("West Stairs");
        putPx(west, west.map.stairs.downFoot.x, west.map.stairs.downFoot.y); ticks(40);
        hold("U", 150);
        check("knowing both pages, Flor recognises the janitor before the Brute's battle", gp.battleMonster == brute
                && gp.dialogue.said("You're the janitor"));
        gp.inBattle = false;
        gp.endBattle(brute, won());
        gp.battleMonster = null; ticks(70);
        check("its key is picked up with a few words about it", p.keys.contains("West Stairwell Key")
                && gp.dialogue.said("Now I've got the keys"));

        System.out.println("6. The journal");
        gp.journal.open();
        check("J opens the journal; it lists the clues found", gp.journal.isOpen() && gp.journal.chosen() == page1);
        gp.journal.move(1);
        check("S chooses the next clue", gp.journal.chosen() == page2);
        gp.journal.close();
        check("J or Esc closes it", !gp.journal.isOpen());

        System.out.println("7. Outside: the fight is on the map");
        for (Monster m : w.monsters) if (!m.room.outdoors) m.defeated = true;   // Skip ahead: every key
        for (Floor f = w.top; f != null; f = f.below) if (f.boss.loot != null) { f.boss.lootTaken = true; p.keys.add(f.boss.loot.key()); }
        Room hall = f1.find("Hallway (Principal's Office)");
        put(hall, 7, 10); hold("D", 120);
        Room quad = w.quadrangle;
        check("through the main entrance, outside, and the scene plays", gp.currentRoom == quad
                && gp.dialogue.said("tears itself out of the earth") && gp.dialogue.said("SPACE to focus"));
        check("going outside moves the checkpoint to the main entrance", p.checkpointRoom == quad);
        SubjectZero zero = w.finalBoss;
        check("Subject Zero sleeps by the south gate, and can't be struck or grab anyone", zero.asleep()
                && !zero.hunting() && zero.room == quad && zero.y / 32 >= 33 && quad.monsters.contains(zero));
        TreeMonster first = null;
        for (int i = 0; i < 600 && first == null; i++) {
            gp.update();
            for (TreeMonster t : w.trees()) if (t.hunting()) first = t;
        }
        check("the trees wake and hunt, with no battle screen", first != null && gp.battleMonster == null);
        ticks(2);
        String word = stand.wordOf(first);
        check("a hunting tree has a word over it: \"" + word + "\" (3-5 letters while it's unhurt)",
                word != null && word.length() >= 3 && word.length() <= 5);
        stand.toggleFocus();
        check("SPACE focuses", stand.focused());
        int px = p.x;
        hold("R", 20);
        check("while focusing the player stands still (holding D does nothing)", p.x == px);
        p.x = 24 * 32; p.y = 16 * 32; p.update();
        double before = dist(first);
        for (int i = 0; i < 30; i++) gp.update();
        double slow = before - dist(first);
        stand.toggleFocus();
        p.x = 24 * 32; p.y = 16 * 32; p.update();
        before = dist(first);
        for (int i = 0; i < 30; i++) gp.update();
        double fast = before - dist(first);
        check("...and the world slows right down (a tree moves " + (int) slow + " px instead of " + (int) fast + ")",
                slow > 0 && slow < fast * 0.6);

        System.out.println("8. Striking by typing");
        Set<Character> firsts = new HashSet<>();
        boolean unique = true;
        for (Hunter h : stand.foesInView()) unique &= firsts.add(stand.wordOf(h).charAt(0));
        check("no two words on screen start with the same letter, so the first letter picks the monster", unique);
        p.health = 100;
        stand.useItem(4);                                  // A Hall Pass: everything stands still a moment
        check("a Hall Pass (4) holds every monster still for a moment", stand.frozen() && p.items.hallPasses == 0);
        while (stand.focus() < LastStand.FOCUS_MAX) gp.update();
        stand.toggleFocus();
        stand.streak = 0;
        Hunter aim = stand.foesInView().get(0);
        word = stand.wordOf(aim);
        int hp = stand.healthOf(aim);
        stand.type(word.charAt(0));
        check("the first letter picks the monster", stand.target() == aim);
        char wrong = 'z';                        // A letter that isn't the next one
        if (word.charAt(1) == 'z') wrong = 'q';
        double focusBefore = stand.focus();
        stand.type(wrong);
        check("a wrong key costs focus (and the streak) but keeps the word", stand.focus() < focusBefore - 20
                && stand.target() == aim && stand.streak == 0);
        for (int i = 1; i < word.length(); i++) stand.type(word.charAt(i));
        check("the whole word: a strike flies at it, and it gets a new word", stand.strikesInFlight() == 1
                && !word.equals(stand.wordOf(aim)));
        stand.toggleFocus();
        for (int i = 0; i < 200 && stand.strikesInFlight() > 0; i++) gp.update();
        int expected = (int) Math.round(word.length() * LastStand.DAMAGE_PER_LETTER);
        check("it lands: " + word.length() + " letters x 3.5 = " + expected + " damage (" + hp + " -> " + stand.healthOf(aim) + ")",
                stand.healthOf(aim) == hp - expected);
        check("cover: walls, the bus and trees stop strikes; fences, benches and bushes don't",
                quad.map.stopsShots(10, 1) && quad.map.stopsShots(12, 28) && !quad.map.stopsShots(0, 20)
                && !quad.map.stopsShots(24, 12) && !TileSet.class.getSimpleName().isEmpty());
        // A strike at a tree across the bus is stopped by it; from right beside the bus it flies over
        TreeMonster target = (TreeMonster) aim;
        boolean[] results = strikeAcrossBus(target);
        check("a strike at a monster on the far side of the bus hits the bus", !results[0]);
        check("...but from right beside the bus it flies over it (you can strike out from cover)", results[1]);

        System.out.println("9. Items, and being hit");
        p.health = 60;
        stand.useItem(1);
        check("a First Aid Kit (1) heals 30", p.health == 90);
        int pens = p.items.redPens;
        stand.useItem(3);
        check("a Red Pen (3) doubles the next strike", stand.redPenReady && p.items.redPens == pens - 1);
        stand.redPenReady = false;
        int books = p.items.textbooks;
        stand.useItem(2);
        check("an Old Textbook (2) halves the next hit", stand.textbookReady && p.items.textbooks == books - 1);
        stand.lastHitBy = null;
        int hpHit = p.health;
        Monster hitter = null;
        for (int i = 0; i < 2000 && stand.lastHitBy == null; i++) gp.update();
        hitter = stand.lastHitBy;
        check("a tree reaches the player: hit for half (" + (hpHit - p.health) + "), knocked back, focus broken",
                hitter instanceof TreeMonster && hpHit - p.health > 0 && hpHit - p.health <= 6 && !stand.focused()
                && !stand.textbookReady);

        System.out.println("10. Felling the trees wakes Subject Zero");
        int left = w.treesLeft();
        p.health = 50;
        TreeMonster next = null;
        for (TreeMonster t : w.trees()) if (!t.defeated) { next = t; break; }
        stand.fell(next);
        check("a felled tree: the player catches their breath (+10) and is told how many are left (" + (left - 1) + ")",
                next.defeated && p.health == 60 && gp.notice().contains((left - 1) + " trees left"));
        Monster same = next;
        p.health = 0;
        gp.loseOutside();
        check("going down: back at the main entrance, the felled trees still felled", gp.currentRoom == quad
                && same.defeated && w.treesLeft() == left - 1);
        for (TreeMonster t : w.trees()) if (!t.defeated && w.treesLeft() > 1) stand.fell(t);
        check("one tree left: Subject Zero still sleeps", w.treesLeft() == 1 && zero.asleep());
        putPx(quad, zero.x, zero.y); gp.battleMonster = null; ticks(40);
        check("walking right onto it: nothing, but something is breathing", gp.battleMonster == null && zero.asleep()
                && gp.notice().contains("breathing") && stand.lastHitBy != zero);
        for (Clue c : all) p.clues.add(c.id);              // (Every clue found: the good ending, later)
        for (TreeMonster t : w.trees()) if (!t.defeated) stand.fell(t);
        check("the last tree falls: Subject Zero wakes, with a few words of its own", zero.awake() && zero.hunting()
                && gp.dialogue.said("STAY... IN... YOUR... SEAT") && gp.dialogue.said("I remember all of them"));

        System.out.println("11. Subject Zero");
        p.items.hallPasses = 20;
        put(quad, 24, 5); ticks(40);
        ticks(5);
        String zw = stand.wordOf(zero);
        check("its words are longer: \"" + zw + "\"", zw != null && zw.length() >= 5);
        before = dist(zero);
        ticks(30);
        check("it comes for the player (" + (int) before + " -> " + (int) dist(zero) + " px)", dist(zero) < before);
        int eruptHp = p.health;
        for (int i = 0; i < 400 && stand.eruptions() == 0; i++) gp.update();
        check("the ground cracks under the player: roots are coming", stand.eruptions() > 0);
        for (int i = 0; i < LastStand.ROOT_WARN + 5; i++) gp.update();
        check("stand still and they burst up under you (" + (eruptHp - p.health) + ")", eruptHp - p.health == LastStand.ROOT_DAMAGE);
        for (int i = 0; i < 400 && stand.eruptions() == 0; i++) gp.update();
        while (stand.frozen()) gp.update();
        stand.useItem(4);                                  // (Holds Subject Zero still; the roots still come)
        int dodgeHp = p.health;
        put(quad, p.x / 32 + 4, p.y / 32);                 // Out of the way
        for (int i = 0; i < LastStand.ROOT_WARN + 5; i++) gp.update();
        check("get out of the way and they miss", p.health == dodgeHp && stand.eruptions() == 0);
        put(quad, zero.x / 32 + 1, zero.y / 32 - 6);       // Close enough to see it, to strike it
        int hits = 0;
        for (int i = 0; i < 12 && stand.saplings().isEmpty() && !zero.defeated; i++) {
            if (!stand.frozen()) stand.useItem(4);         // Hold it still while the test types
            if (strike(zero)) hits++;
        }
        check("hurt to two thirds (" + hits + " strikes), it calls up two saplings through the gates",
                stand.saplings().size() == 2 && gp.notice().contains("saplings"));
        ticks(2);                                          // (They get their words as they come in)
        Hunter sapling = stand.saplings().get(0);
        for (Hunter s : stand.saplings()) if (dist(s) < dist(sapling)) sapling = s;   // The one in sight
        String sw = stand.wordOf(sapling);
        if (!stand.frozen()) stand.useItem(4);
        put(quad, sapling.x / 32, sapling.y / 32 - 5);     // Where it can be seen
        boolean felled = strike(sapling);
        check("a sapling's word is short (\"" + sw + "\") and one strike fells it", sw != null && sw.length() <= 4
                && felled && sapling.defeated && stand.saplings().size() == 1);
        stand.fell(zero);
        check("Subject Zero falls: every clue found, the good ending (REMEMBERED)", gp.ended && gp.endedRemembered
                && gp.dialogue.said("Every name is back"));

        GamePanel gp2 = new GamePanel();
        gp2.peaceful();
        gp2.world.finalBoss.defeated = false;
        gp2.world.finalBoss.wake();
        gp2.currentRoom = gp2.world.quadrangle;
        gp2.lastStand.fell(gp2.world.finalBoss);
        check("fewer clues: the other ending (FORGOTTEN)", gp2.ended && !gp2.endedRemembered
                && gp2.dialogue.said("Nobody can remember who used to sit there"));

        if (fails == 0) {
            System.out.println("ALL PASSED");
        } else {
            System.out.println(fails + " FAILED");
        }
        if (fails > 0) System.exit(1);   // Tells run-checks.bat this check failed
        System.exit(0);
    }

    static double dist(Hunter h) {
        return Math.hypot(h.centreX() - p.solidArea.getCenterX(), h.centreY() - p.solidArea.getCenterY());
    }

    // Types the monster's word and waits for the strike to land; true if it landed (did damage)
    static boolean strike(Hunter h) {
        while (stand.focus() < LastStand.FOCUS_MIN + 40) gp.update();
        if (!stand.focused()) stand.toggleFocus();
        int hp = stand.healthOf(h);
        String word = stand.wordOf(h);
        if (word == null) return false;
        for (char c : word.toCharArray()) stand.type(c);
        stand.toggleFocus();
        for (int i = 0; i < 300 && stand.strikesInFlight() > 0; i++) gp.update();
        return h.defeated || stand.healthOf(h) < hp;
    }

    // Puts a tree east of the bus and strikes it twice, everything held still by a Hall Pass: from
    // across the bus, then from right beside its front. Returns {first landed, second landed}.
    static boolean[] strikeAcrossBus(TreeMonster tree) {
        boolean[] landed = new boolean[2];
        int[][] from = {{4, 28}, {13, 28}};
        int[][] at = {{15, 28}, {3, 28}};
        for (int k = 0; k < 2; k++) {
            p.items.hallPasses = Math.max(p.items.hallPasses, 1);
            while (stand.frozen()) gp.update();
            stand.useItem(4);
            put(w.quadrangle, from[k][0], from[k][1]);
            tree.x = at[k][0] * 32;
            tree.y = at[k][1] * 32;
            tree.stunned = 0;
            landed[k] = strike(tree);
        }
        return landed;
    }
}
