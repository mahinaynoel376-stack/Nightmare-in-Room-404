package nightmare404.checks;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nightmare404.battle.LastStand;
import nightmare404.entity.Clue;
import nightmare404.entity.Hunter;
import nightmare404.entity.SleepingBoss;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.entity.SubjectZero;
import nightmare404.entity.TreeMonster;
import nightmare404.main.GamePanel;
import nightmare404.story.Line;
import nightmare404.story.Quest;
import nightmare404.story.Quests;
import nightmare404.tile.TileSet;
import nightmare404.ui.Dawn;
import nightmare404.ui.DialogueBox;
import nightmare404.ui.WakeUp;
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
    static Monster started;

    static void check(String what, boolean ok) { System.out.println((ok ? "  ok   " : "  FAIL ") + what); if (!ok) fails++; }
    static void put(Room room, int col, int row) { gp.currentRoom = room; p.x = col * 32; p.y = row * 32; p.update(); }
    static void putPx(Room room, int x, int y) { gp.currentRoom = room; p.x = x; p.y = y; p.update(); }
    static void ticks(int n) { for (int i = 0; i < n && started == null; i++) gp.update(); }
    static void hold(String keys, int n) {
        started = null;
        gp.upPressed = keys.contains("U"); gp.downPressed = keys.contains("D"); gp.leftPressed = keys.contains("L"); gp.rightPressed = keys.contains("R");
        ticks(n);
        gp.upPressed = gp.downPressed = gp.leftPressed = gp.rightPressed = false;
    }
    static nightmare404.battle.BattleScreen.BattleResult won() { return new nightmare404.battle.BattleScreen.BattleResult(true, p.health, p.kits); }

    public static void main(String[] a) {
        gp = new GamePanel(); p = gp.kai; w = gp.world; stand = gp.lastStand;
        gp.battleStarter = m -> started = m;
        Floor f4 = w.floor4, f3 = w.floor3, f1 = w.floor1;

        System.out.println("1. The opening");
        ticks(1);
        check("the game opens with the intro in Room 404", gp.dialogue.said("STAY IN YOUR SEAT") && gp.currentRoom.name.equals("Room 404"));
        check("...which explains the controls and the clues", gp.dialogue.said("J opens your journal") && gp.dialogue.said("20%"));

        System.out.println("2. The dialogue box");
        DialogueBox box = new DialogueBox();
        box.autoSkip = false;
        boolean[] done = {false};
        box.open(List.of(Line.narrate("First line."), Line.kai("Second.")), () -> done[0] = true);
        check("a conversation opens", box.isOpen());
        box.update(true);
        check("a key press right as it opens doesn't skip anything", box.isOpen() && !done[0]);
        for (int i = 0; i < 20; i++) box.update(false);
        box.update(true);
        check("pressing goes on to the next line", box.isOpen() && !done[0]);
        box.update(true);
        check("pressing while it types shows the whole line, without going on", box.isOpen());
        box.update(true);
        check("pressing on the last line closes it and runs what comes next", !box.isOpen() && done[0]);
        gp.dialogue.autoSkip = false;
        gp.dialogue.open(List.of(Line.narrate("Wait.")), null);
        int x0 = p.x, y0 = p.y;
        hold("D", 40);
        check("while it's open the world waits: holding S doesn't move the player", p.x == x0 && p.y == y0 && gp.dialogue.isOpen());
        while (gp.dialogue.isOpen()) gp.dialogue.update(true);
        gp.dialogue.autoSkip = true;

        System.out.println("2b. Stepping out of Room 404: the first fight");
        nightmare404.entity.FirstBlob firstFight = w.firstFight;
        Room hall404 = f4.find("Hallway (Room 404)");
        check("it's a weak blob, the " + firstFight.enemy.name() + " (" + firstFight.enemy.maxHp() + " HP, " + nightmare404.battle.BattleScreen.turnSeconds(firstFight.enemy)
                + "s a turn), not one of the hallway's own monsters", firstFight.enemy.maxHp() <= 40 && !hall404.monsters.contains(firstFight)
                && !w.monsters.contains(firstFight));
        started = null;
        put(hall404, 8, 8); ticks(1);
        check("out in the hallway, Kai sees the state of the place, and something drips on him",
                gp.dialogue.said("reality is twisted") && gp.dialogue.said("are those.. monsters") && gp.dialogue.said("drips onto your shoulder"));
        check("the blob drops from the ceiling beside him (the world waits while it falls)", firstFight.falling()
                && Math.abs(firstFight.x - p.x) <= 28 && firstFight.y == p.y && started == null);
        int beforeDrop = p.x;
        gp.rightPressed = true;
        ticks(nightmare404.entity.FirstBlob.DROP_TICKS + 5);
        gp.rightPressed = false;
        check("it lands: the controls are explained, and the fight starts (and Kai couldn't walk off meanwhile)",
                !firstFight.falling() && gp.dialogue.said("Spell words with the letters") && started == firstFight && p.x == beforeDrop);
        gp.inBattle = false;
        gp.endBattle(firstFight, new nightmare404.battle.BattleScreen.BattleResult(false, 0, p.kits));
        check("losing it: back in Room 404, and Kai gets his breath back (the first time)", gp.currentRoom.name.equals("Room 404")
                && gp.dialogue.said("I'm back here again?") && gp.notice().contains("wake up"));
        started = null;
        ticks(1);                                          // (A moment back in Room 404...)
        put(hall404, 8, 8); ticks(1 + nightmare404.entity.FirstBlob.DROP_TICKS + 5);   // (...then out through the door again)
        check("out again: it's still up there, and drops again", started == firstFight && gp.dialogue.said("still up there"));
        gp.inBattle = false;
        gp.endBattle(firstFight, won());
        check("winning it: Kai catches his breath and decides to look for the logbook's pages", firstFight.defeated
                && gp.dialogue.said("somewhat fend them off") && gp.dialogue.said("missing pages of the janitor's logbook"));
        started = null;
        put(f4.find("Room 404"), 12, 12); ticks(1);
        put(hall404, 8, 8); ticks(10);
        check("after that, the hallway is quiet", started == null);

        System.out.println("3. Every floor has a quest, with two clues in its side rooms");
        List<Clue> all = Quests.all(w);
        check("10 clues in all", all.size() == 10);
        for (Floor f = w.top; f != null; f = f.below) {
            Quest q = f.quest;
            check("Floor " + f.number + ": " + (q == null ? "no quest" : q.title + ", " + q.clues.size() + " clues"),
                    q != null && q.clues.size() == (f.number == 3 || f.number == 1 ? 3 : 2));
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

        System.out.println("4. Picking clues up weakens the floor's boss (and on Floor 4, the pages wake it)");
        Monster brute = f4.boss;
        Room stairs4 = f4.find("West Stairs");
        putPx(stairs4, stairs4.map.stairs.downFoot.x, stairs4.map.stairs.downFoot.y); ticks(40);
        hold("U", 150);
        check("without the janitor's pages, the boulder on the stairs won't budge: no battle", started == null
                && ((SleepingBoss) brute).asleep() && gp.dialogue.said("It won't budge") && gp.notice().contains("0 of 2"));
        int bruteHp = brute.enemy.maxHp();
        check("with no clues the Brute starts at full health (" + bruteHp + ")", gp.director.foeStartHp(brute) == bruteHp);
        Room r402 = f4.find("Room 402"), r403 = f4.find("Room 403");
        for (Monster m : r402.monsters) m.defeated = true;    // Nobody in the way
        Clue page1 = r402.clues.get(0), page2 = r403.clues.get(0);
        int kits = p.kits;
        p.setCheckpoint();
        putPx(r402, page1.x, page1.y); ticks(1);
        check("walking over the page picks it up, and the player reads it", p.clues.contains(page1.id)
                && gp.dialogue.said("Kids keep staying late in 404"));
        check("...and Kai reacts to it", gp.dialogue.said("That's MY classroom"));
        check("...finds a first aid kit with it (" + p.kits + "), and is told the Brute is weaker",
                p.kits == kits + 1 && gp.notice().contains("20% weaker"));
        check("one clue: the Brute starts at 80% (" + gp.director.foeStartHp(brute) + ")",
                gp.director.foeStartHp(brute) == Math.round(bruteHp * 0.8));
        Monster mimic = r402.monsters.get(0);
        mimic.defeated = false;
        gp.endBattle(mimic, new nightmare404.battle.BattleScreen.BattleResult(false, 0, 0));
        check("giving up after a lost battle puts the clue back", !p.clues.contains(page1.id) && p.kits == kits);
        putPx(r402, page1.x, page1.y); ticks(1);
        check("picked up again, it says so in one line instead of the whole page", p.clues.contains(page1.id)
                && gp.dialogue.said("You pick up the torn logbook page again."));
        for (Monster m : r403.monsters) m.defeated = true;
        putPx(r403, page2.x, page2.y); ticks(1);
        check("both pages: the Brute starts at 60% (" + gp.director.foeStartHp(brute) + ")",
                gp.director.foeStartHp(brute) == Math.round(bruteHp * 0.6) && f4.quest.complete(p.clues));
        check("the other bosses aren't weakened by Floor 4's pages", gp.director.foeStartHp(f3.boss) == f3.boss.enemy.maxHp());

        System.out.println("5. The bosses react to what the player knows");
        Room west = f4.find("West Stairs");
        putPx(west, west.map.stairs.downFoot.x, west.map.stairs.downFoot.y); ticks(40);
        check("walking into the West Stairs: the way down is blocked, and the boulder seems to breathe",
                gp.dialogue.said("blocked. Of course they are") && gp.dialogue.said("seems to breathe"));
        hold("U", 150);
        check("with both pages, Kai reads them to the boulder; it wakes, he recognises the janitor, and the battle starts",
                started == brute && gp.dialogue.said("Let me read you your own words") && gp.dialogue.said("You're the janitor"));
        gp.inBattle = false;
        gp.endBattle(brute, won());
        check("right after the win, a few words for the janitor, and the way down is clear", gp.dialogue.said("only trying to help us")
                && gp.dialogue.said("The stairs down are clear") && gp.notice().contains("way down is clear"));
        started = null; ticks(70);
        check("it drops no key: the way down is open once it's beaten", brute.loot == null && !f4.locked(p.keys)
                && gp.dialogue.said("Down we go"));

        System.out.println("5b. Scenes along the way");
        Room libEntrance = f3.find("Library (Entrance)"), libEast = f3.find("Library (East)");
        put(libEntrance, 12, 14); ticks(1);
        check("Floor 3: walking into the library", gp.dialogue.said("hear my own heartbeat") && gp.dialogue.said("Nope nope nope"));
        put(libEast, 4, 14); ticks(1);
        check("...and into the back of it, where the whispering is", gp.dialogue.said("It's saying names"));
        Clue lastBell = null;
        for (Clue c : f3.quest.clues) if (c.id.equals("3F-3")) lastBell = c;
        putPx(libEntrance, lastBell.x, lastBell.y); ticks(1);
        check("the third checkout card: the librarian has been reading the names so they stay",
                p.clues.contains("3F-3") && gp.dialogue.said("I KEEP READING THEM SO THEY STAY"));
        Room eastStairs = f3.find("East Stairs");
        putPx(eastStairs, eastStairs.map.stairs.downFoot.x, eastStairs.map.stairs.downFoot.y); hold("U", 60);
        check("the haze over the east stairs calls Kai back to the library", gp.dialogue.said("It wants me to go back to the library"));
        gp.director.beforeBattle(f3.boss, () -> { });
        check("the Whisperer, intangible, whispers blacked-out names", gp.dialogue.said("they're all blacked out")
                && ((nightmare404.entity.Whisperer) f3.boss).intangible());
        Monster librarian = null;
        for (Monster m : libEntrance.monsters) if (m.enemy.name().equals("Librarian")) librarian = m;
        gp.director.beforeBattle(librarian, () -> { });
        check("meeting the Librarian: shushed", gp.dialogue.said("one bony finger to her lips"));
        int saidBefore = gp.dialogue.linesSaid();
        gp.director.beforeBattle(librarian, () -> { });
        check("  (only the first time)", gp.dialogue.linesSaid() == saidBefore);
        gp.endBattle(f3.boss, won());
        check("beating the Whisperer: a different kind of quiet, and the haze clears", gp.dialogue.said("different kind of quiet")
                && !eastStairs.haze.active());
        Room containment = w.floor2.find("Containment Lab");
        put(containment, 12, 14); ticks(1);
        check("Floor 2: the first lab walked into smells, and the Containment Lab has its smashed pods",
                gp.dialogue.said("don't touch anything green") && gp.dialogue.said("something's floating in it"));
        Room front = f1.find("Hallway (Principal's Office)");
        put(front, 7, 10); hold("D", 120);
        check("Floor 1: the front doors are locked, and Kai isn't happy about it", gp.currentRoom == front
                && gp.dialogue.said("Locked?! Come on") && gp.notice().equals(f1.lockedNote));
        Clue yearbook = null;
        for (Clue c : f1.quest.clues) if (c.id.equals("1F-3")) yearbook = c;
        putPx(yearbook.room, yearbook.x, yearbook.y); ticks(1);
        check("Floor 1's yearbook: Kai's own photo, half rubbed away", p.clues.contains("1F-3")
                && gp.dialogue.said("grading them out of existence"));

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
        p.keys.remove(SubjectZero.CALMING_ITEM);            // (Test Subject X left alone: the full fight outside)
        Room hall = f1.find("Hallway (Principal's Office)");
        put(hall, 7, 10); hold("D", 120);
        Room quad = w.quadrangle;
        check("through the main entrance, outside, and the scene plays", gp.currentRoom == quad
                && gp.dialogue.said("tears itself out of the earth") && gp.dialogue.said("SPACE to focus"));
        check("going outside moves the checkpoint to the main entrance", p.checkpointRoom == quad);
        SubjectZero zero = w.finalBoss;
        check("Subject Zero sleeps up the path from the south gate, and can't be struck or grab anyone", zero.asleep()
                && !zero.hunting() && zero.room == quad && zero.y / 32 == nightmare404.map.Quadrangle.ZERO_ROW
                && quad.monsters.contains(zero));
        TreeMonster first = null;
        for (int i = 0; i < 600 && first == null; i++) {
            gp.update();
            for (TreeMonster t : w.trees()) if (t.hunting()) first = t;
        }
        check("the trees wake and hunt, with no battle screen", first != null && started == null);
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
        char wrong = word.charAt(1) == 'z' ? 'q' : 'z';
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
        int books = p.items.textbooks, flying = stand.strikesInFlight();
        stand.useItem(2);
        check("an Old Textbook (2) is thrown at the nearest monster in sight", stand.strikesInFlight() == flying + 1
                && p.items.textbooks == books - 1);
        stand.lastHitBy = null;
        int hpHit = p.health;
        Monster hitter = null;
        for (int i = 0; i < 2000 && stand.lastHitBy == null; i++) gp.update();
        hitter = stand.lastHitBy;
        check("a tree reaches the player: hit (" + (hpHit - p.health) + "), knocked back, focus broken",
                hitter instanceof TreeMonster && hpHit - p.health > 0 && !stand.focused());

        System.out.println("10. Felling the trees wakes Subject Zero");
        int left = w.treesLeft();
        p.health = 50;
        TreeMonster next = null;
        for (TreeMonster t : w.trees()) if (!t.defeated) { next = t; break; }
        stand.fell(next);
        check("a felled tree: the player catches their breath (+10) and is told how many are left (" + (left - 1) + ")",
                next.defeated && p.health == 60 && gp.notice().contains((left - 1) + " trees left"));
        check("  the first one felled: \"One down..\"", gp.dialogue.said("One down"));
        Monster same = next;
        p.health = 0;
        gp.loseOutside();
        check("going down: back at the main entrance, the felled trees still felled", gp.currentRoom == quad
                && same.defeated && w.treesLeft() == left - 1);
        for (TreeMonster t : w.trees()) if (!t.defeated && w.treesLeft() > 1) stand.fell(t);
        check("one tree left: Subject Zero still sleeps", w.treesLeft() == 1 && zero.asleep());
        putPx(quad, zero.x, zero.y); started = null; ticks(40);
        check("walking right onto it: nothing, but something is breathing", started == null && zero.asleep()
                && gp.notice().contains("breathing") && stand.lastHitBy != zero);
        for (Clue c : all) p.clues.add(c.id);              // (Every clue found: the good ending, later)
        for (TreeMonster t : w.trees()) if (!t.defeated) stand.fell(t);
        check("the last tree falls: the camera goes over to Subject Zero, and it starts to rise",
                gp.watching() && zero.rising() && !zero.awake());
        int risen = 0;
        for (; risen < 600 && (!zero.awake() || gp.watching()); risen++) gp.update();
        check("  it rises out of its heap and wakes, then the camera comes back (" + risen + " frames)",
                zero.awake() && !gp.watching() && gp.cameraY() == Math.max(0, Math.min(p.y + 16 - 288, quad.map.heightPx() - 576)));
        check("the last tree falls: Subject Zero wakes, with a few words of its own", zero.awake() && zero.hunting()
                && gp.dialogue.said("STAY... IN... YOUR... SEAT") && gp.dialogue.said("I remember all of them"));
        check("  halfway through the trees, Kai saw where the roots lead; facing it, he won't sit down",
                gp.dialogue.said("Toward the south gate") && gp.dialogue.said("I'm not sitting back down"));

        System.out.println("11. Subject Zero");
        check("without Subject X's tentacle (Test Subject X left alone), it's the full, harder fight: speed "
                + SubjectZero.speed(p) + ", roots every " + stand.rootEvery() + " frames",
                SubjectZero.speed(p) == SubjectZero.HUNT_SPEED && stand.rootEvery() == LastStand.ROOT_EVERY);
        p.keys.add(SubjectZero.CALMING_ITEM);
        check("  with it, it's calmer: speed " + SubjectZero.speed(p) + ", roots every " + stand.rootEvery() + " frames",
                SubjectZero.speed(p) < SubjectZero.HUNT_SPEED && stand.rootEvery() > LastStand.ROOT_EVERY);
        p.keys.remove(SubjectZero.CALMING_ITEM);
        check("  and when it wakes, Kai holds the tentacle up to it (only if he has it)",
                nightmare404.story.Story.awakening(true, true).stream().anyMatch(l -> l.text().contains("something of your brother's"))
                && nightmare404.story.Story.awakening(true, false).stream().noneMatch(l -> l.text().contains("brother")));
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
        check("the ground cracks under the player: roots are coming", stand.eruptions() > 0 && gp.dialogue.said("Move, move, MOVE!"));
        for (int i = 0; i < LastStand.ROOT_WARN + 5; i++) gp.update();
        check("stand still and they burst up under you (" + (eruptHp - p.health) + ")", eruptHp - p.health == LastStand.ROOT_DAMAGE);
        for (int i = 0; i < 400 && stand.eruptions() == 0; i++) gp.update();
        while (stand.frozen()) gp.update();
        stand.useItem(4);                                  // (Holds Subject Zero still; the roots still come)
        int dodgeHp = p.health;
        put(quad, p.x / 32 + 4, p.y / 32);                 // Out of the way
        for (int i = 0; i < LastStand.ROOT_WARN + 5; i++) gp.update();
        check("get out of the way and they miss", p.health == dodgeHp && stand.eruptions() == 0);
        put(quad, zero.x / 32 + 1, zero.y / 32 + 5);       // Close enough to see it, to strike it (south of it: the
                                                           // Book Barricade is just north of where it has got to)
        int hits = 0;
        for (int i = 0; i < 12 && stand.saplings().isEmpty() && !zero.defeated; i++) {
            if (!stand.frozen()) stand.useItem(4);         // Hold it still while the test types
            if (strike(zero)) hits++;
        }
        check("hurt to two thirds (" + hits + " strikes), it calls up " + LastStand.SAPLINGS_PER_WAVE + " saplings through the gates",
                stand.saplings().size() == LastStand.SAPLINGS_PER_WAVE && gp.notice().contains("saplings") && gp.dialogue.said("calling them in through the gates"));
        ticks(2);                                          // (They get their words as they come in)
        Hunter sapling = stand.saplings().get(0);
        for (Hunter s : stand.saplings()) if (dist(s) < dist(sapling)) sapling = s;   // The one in sight
        String sw = stand.wordOf(sapling);
        if (!stand.frozen()) stand.useItem(4);
        put(quad, sapling.x / 32, sapling.y / 32 - 5);     // Where it can be seen
        boolean felled = strike(sapling);
        check("a sapling's word is short (\"" + sw + "\") and one strike fells it", sw != null && sw.length() <= 4
                && felled && sapling.defeated && stand.saplings().size() == LastStand.SAPLINGS_PER_WAVE - 1);
        stand.fell(zero);
        check("Subject Zero falls: the south gate opens and the sun comes up, but it isn't over yet",
                gp.dawn.on() && !gp.ended && !p.dizzy && gp.dialogue.said("I can go home")
                && gp.notice().contains("south gate is open") && !quad.map.collides(gp.dawn.gate()));
        ticks(Dawn.THOUGHT + 5);
        check("on the way, the sky turns gold", gp.dialogue.said("The sky over the south gate turns gold"));
        walkOut(gp);
        check("walking out through the south gate: the good ending (REMEMBERED)", gp.ended && gp.endedRemembered
                && gp.dialogue.said("Every name is back") && gp.dialogue.said("Not ever again"));

        GamePanel gp2 = new GamePanel();
        gp2.peaceful();
        gp2.world.finalBoss.defeated = false;
        gp2.world.finalBoss.wake();
        gp2.currentRoom = gp2.world.quadrangle;
        gp2.lastStand.fell(gp2.world.finalBoss);
        check("fewer clues: a red dawn, and Kai is dizzy", gp2.dawn.on() && !gp2.ended && gp2.kai.dizzy
                && gp2.dialogue.said("the sky over the wall is red"));
        gp2.kai.x = 24 * 32; gp2.kai.y = 20 * 32; gp2.kai.update();
        gp2.update();                                      // (Arriving outside: the scenes for that)
        int startY = gp2.kai.y;
        gp2.downPressed = true;
        for (int i = 0; i < 20; i++) gp2.update();
        gp2.downPressed = false;
        check("dizzy, he walks slower (" + (gp2.kai.y - startY) + " px in 20 frames)",
                gp2.kai.y - startY == 20 * Player.DIZZY_SPEED);
        walkOut(gp2);
        check("out through the gate: the other ending (FORGOTTEN)", gp2.ended && !gp2.endedRemembered
                && gp2.dialogue.said("Nobody can remember who used to sit there") && gp2.dialogue.said("Whose desk was that"));

        // The opening scene (skipped without a screen, so it's started here)
        GamePanel gp3 = new GamePanel();
        gp3.wakeUp.start();
        for (int i = 0; i < 100; i++) gp3.update();
        check("the game opens in the dark, Kai asleep at his desk, before anything is said",
                gp3.wakeUp.playing() && !gp3.dialogue.said("I fell asleep"));
        for (int i = 0; i < WakeUp.LENGTH; i++) gp3.update();
        check("...he wakes up and stands in the aisle, then the story starts", !gp3.wakeUp.playing()
                && gp3.dialogue.said("I fell asleep") && gp3.kai.x == 12 * 32 && gp3.kai.y == 10 * 32);
        GamePanel gp4 = new GamePanel();
        gp4.wakeUp.start();
        for (int i = 0; i < 60; i++) gp4.update();
        check("it can't be skipped (a second in, it's still playing)", gp4.wakeUp.playing());

        // Running is limited (stamina). Kai in Room 404's aisle, running left (into the wall, once he gets there)
        Player f = gp4.kai;
        gp4.sprintPressed = true;
        gp4.leftPressed = true;
        int fx = f.x;
        f.update();
        check("running: 5 px a frame, and it uses stamina", fx - f.x == Player.SPRINT_SPEED && f.stamina < Player.STAMINA_MAX);
        for (int i = 1; i < Player.STAMINA_MAX / Player.RUN_COST; i++) f.update();
        check("3 seconds of running and he's out of breath", f.stamina == 0 && f.outOfBreath);
        gp4.leftPressed = false;
        gp4.rightPressed = true;
        fx = f.x;
        f.update();
        check("...and can only walk, even holding Shift", f.x - fx == Player.WALK_SPEED);
        gp4.rightPressed = gp4.sprintPressed = false;
        for (int i = 0; i < Player.STAMINA_MAX / 2 / Player.REST_STILL; i++) f.update();
        check("standing still, he gets his breath back (half full: he can run again)", !f.outOfBreath);

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }

    // Stands Kai just above the open south gate and walks him down through it, until the game ends
    static void walkOut(GamePanel g) {
        g.kai.x = Dawn.GATE_COL * 32 + 24;                // Right in the middle of the 2-tile gap
        g.kai.y = (Dawn.GATE_ROW - 1) * 32 - 16;
        g.kai.update();
        g.downPressed = true;
        for (int i = 0; i < 400 && !g.ended; i++) g.update();
        g.downPressed = false;
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
