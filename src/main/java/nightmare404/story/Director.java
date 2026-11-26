package nightmare404.story;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nightmare404.entity.Clue;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.entity.SubjectZero;
import nightmare404.main.GamePanel;
import nightmare404.world.Floor;
import nightmare404.world.Room;

// Decides WHEN the story happens: GamePanel (and LastStand, outside) tells it what the player just did
// (walked somewhere new, touched a boss, won a battle, picked up a clue, felled a tree...) and it opens
// the right conversation in the dialogue box. What's said is in Story.
//
// It remembers what's already been shown (seen), so a scene plays once. Giving up after a lost battle
// never undoes that: a page picked up again shows one line instead of the whole page.
public class Director {
    private final GamePanel gp;
    private final Set<String> seen = new HashSet<>();
    private Room entered;               // The room the player was in last frame (to notice walking into one)

    public Director(GamePanel gp) {
        this.gp = gp;
    }

    // True if this scene has played: "intro", "floor3", "outside", "clue:4F-1"...
    public boolean seen(String scene) {
        return seen.contains(scene);
    }

    // Every frame the world runs: the opening scene, and arriving somewhere for the first time
    public void update() {
        if (seen.add("intro")) {
            seen.add("floor4");
            entered = gp.currentRoom;
            play(Story.INTRO, null);
            return;
        }
        Room room = gp.currentRoom;
        boolean walkedIn = room != entered;
        entered = room;
        if (room.outdoors) {
            if (seen.add("outside")) {
                play(Story.OUTSIDE, null);
                return;
            }
            SubjectZero zero = gp.world.finalBoss;   // Walking right past it while it sleeps: a hint, once
            if (zero != null && zero.asleep() && near(zero, 3 * 32) && seen.add("breathing")) {
                gp.showNotice(Story.BREATHING, 240);
            }
        } else {
            inside(room, walkedIn);
        }
    }

    // Inside the building: the first time on a floor, in one of its rooms, in its boss's room; and the
    // first fight, the first time the player steps out of Room 404 into the hallway
    private void inside(Room room, boolean walkedIn) {
        Floor f = room.floor;
        List<Line> scene = new ArrayList<>();
        if (seen.add("floor" + f.number)) scene.addAll(Story.arrive(f.number));
        if (walkedIn) {
            boolean hallway = f.indexOf(room) >= 0;          // A hallway screen or a stairwell
            if (!hallway && seen.add("room" + f.number)) scene.addAll(Story.firstRoom(f.number));
            if (f.boss != null && room == f.boss.room && !f.boss.defeated && seen.add("bossRoom" + f.number)) {
                scene.addAll(Story.bossRoom(f.number));
            }
            Monster first = gp.world.firstFight;
            if (hallway && first != null && !first.defeated && first.room.floor == f) {
                scene.addAll(seen.add("firstFight") ? Story.FIRST_FIGHT : Story.FIRST_FIGHT_AGAIN);
                play(scene, () -> gp.startBattle(first));
                return;
            }
        }
        if (!scene.isEmpty()) play(scene, null);
    }

    // The player walked into a monster. A floor's boss says something first (once for each number of
    // clues found, so it reacts to what the player knows); then the battle starts.
    public void beforeBattle(Monster m, Runnable fight) {
        if (m.boss && m == m.room.floor.boss) {
            Floor f = m.room.floor;
            int found = f.quest == null ? 0 : f.quest.found(gp.flor.clues);
            play(seen.add("boss" + f.number + ":" + found) ? Story.bossBefore(f.number, found) : List.of(), fight);
        } else {
            String name = m.enemy.name();       // Some monsters get a few words the first time they're met
            play(seen.add("meet:" + name) ? Story.meeting(name) : List.of(), fight);
        }
    }

    // How much health the monster starts its battle with: a floor's boss loses some for every clue
    // found on its floor (see Quest)
    public int foeStartHp(Monster m) {
        Floor f = m.room.floor;
        if (m == f.boss && f.quest != null) return f.quest.bossStartHp(gp.flor.clues);
        return m.enemy.maxHp();
    }

    // After a battle the player won (the monster is already marked defeated)
    public void afterWin(Monster m) {
        if (m == gp.world.firstFight) {
            play(Story.AFTER_FIRST_FIGHT, null);
        } else if (m.boss) {
            String notice = "The " + m.enemy.name() + " falls. Something glints where it stood...";
            play(Story.bossDown(m.room.floor.number), () -> gp.showNotice(notice));
        }
    }

    // The player tried the floor's way out (or down) without its key. The first time, Flor may say
    // something about it; the locked note shows every time.
    public void lockedOut(Floor f) {
        play(seen.add("locked" + f.number) ? Story.lockedOut(f.number) : List.of(), () -> gp.showNotice(f.lockedNote));
    }

    // The player lost a battle and gave up (or went down outside): the first time, a few words
    public void wokeUp(boolean outside) {
        String notice = outside ? Story.WAKE_OUTSIDE : Story.WAKE_INSIDE;
        play(seen.add("woke") ? Story.FIRST_WAKE : List.of(), () -> gp.showNotice(notice));
    }

    // Outside, a Hollow Oak was felled (see LastStand). When the last one goes, Subject Zero wakes.
    public void treeFelled(int heal, int left) {
        if (left > 0) {
            int trees = gp.world.trees().size();
            List<Line> words = left == trees - 1 && seen.add("firstTree") ? Story.FIRST_TREE
                    : left == trees / 2 && seen.add("halfTrees") ? Story.HALF_THE_TREES : List.of();
            play(words, () -> gp.showNotice(Story.treeFelled(heal, left), 240));
            return;
        }
        SubjectZero zero = gp.world.finalBoss;
        zero.wake();
        gp.showNotice("", 0);
        play(Story.awakening(allCluesFound()), null);
    }

    // Hurt, Subject Zero calls up its saplings (see LastStand); Flor has something to say the first time
    public void saplingsCalled() {
        play(seen.add("saplings") ? Story.FIRST_SAPLINGS : List.of(), () -> gp.showNotice(Story.SAPLINGS, 180));
    }

    // Subject Zero's roots crack the ground around the player for the first time
    public void groundCracks() {
        if (seen.add("roots")) play(Story.FIRST_ROOTS, null);
    }

    // Subject Zero is down: the last words, then the ending screen
    public void zeroBeaten() {
        boolean remembered = allCluesFound();
        play(Story.ending(remembered), () -> gp.end(remembered));
    }

    // The player walked over a clue: read it, take what's with it, and see how the boss is weakened
    public void clueFound(Clue clue) {
        Player p = gp.flor;
        p.clues.add(clue.id);
        p.addSupplies(clue.supplies);
        List<Line> lines = new ArrayList<>(seen.add("clue:" + clue.id) ? clue.pages : Story.clueAgain(clue.title));
        Floor f = clue.room.floor;
        Quest q = f.quest;
        String note = q.things + " found: " + q.found(p.clues) + " of " + q.clues.size() + ".";
        if (f.boss != null && !f.boss.defeated) {
            note += " The " + f.boss.enemy.name() + " will start its fight "
                    + Math.round(q.weakening(p.clues) * 100) + "% weaker.";
        }
        String supplies = clue.supplies.supplies();
        String notice = supplies.isEmpty() ? note : note + "\nFound with it: " + supplies;
        play(lines, () -> gp.showNotice(notice, 300));
    }

    // The player picked up what a boss dropped
    public void lootTaken(Monster boss) {
        String notice = "Found: " + boss.loot.key() + "\n" + boss.loot.supplies();
        play(Story.bossAfter(boss.room.floor.number, boss.loot.found()), () -> gp.showNotice(notice, 300));
    }

    // Reading a clue again from the journal
    public void reread(Clue clue, Runnable then) {
        play(clue.pages, then);
    }

    public int cluesFound() {
        int n = 0;
        for (Clue c : Quests.all(gp.world)) {
            if (gp.flor.clues.contains(c.id)) n++;
        }
        return n;
    }

    public int cluesInAll() {
        return Quests.all(gp.world).size();
    }

    public boolean allCluesFound() {
        return cluesFound() == cluesInAll();
    }

    private boolean near(Monster m, int distance) {
        return Math.hypot(gp.flor.solidArea.getCenterX() - m.solidArea.getCenterX(),
                gp.flor.solidArea.getCenterY() - m.solidArea.getCenterY()) < distance;
    }

    private void play(List<Line> lines, Runnable then) {
        gp.dialogue.open(lines, then);
    }
}
