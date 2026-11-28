package nightmare404.story;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nightmare404.entity.Clue;
import nightmare404.entity.FirstBlob;
import nightmare404.entity.SleepingBoss;
import nightmare404.entity.Monster;
import nightmare404.entity.Player;
import nightmare404.entity.SubjectZero;
import nightmare404.entity.Whisperer;
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
            FirstBlob first = gp.world.firstFight;
            if (hallway && first != null && !first.defeated && first.room.floor == f) {
                scene.addAll(seen.add("firstFight") ? Story.FIRST_FIGHT : Story.FIRST_FIGHT_AGAIN);
                play(scene, () -> first.drop(gp.flor));   // It drops on her; landing starts its battle
                return;
            }
        }
        if (!scene.isEmpty()) play(scene, null);
    }

    // The player walked into a monster. A floor's boss says something first (once for each number of
    // clues found, so it reacts to what the player knows); then the battle starts.
    public void beforeBattle(Monster m, Runnable fight) {
        if (m == gp.world.firstFight) {          // The blob has landed on her: the controls, the first time
            play(seen.add("firstFightLanded") ? Story.FIRST_FIGHT_LANDED : List.of(), fight);
            return;
        }
        if (m instanceof SleepingBoss && ((SleepingBoss) m).asleep()) {
            wakeUp((SleepingBoss) m);            // Still asleep: no battle yet
            return;
        }
        if (m instanceof Whisperer && ((Whisperer) m).intangible()) {
            atTheWhisperer((Whisperer) m);       // Still intangible: no battle yet
            return;
        }
        if (m.boss && m == m.room.floor.boss) {
            Floor f = m.room.floor;
            int found = f.quest == null ? 0 : f.quest.found(gp.flor.clues);
            play(seen.add("boss" + f.number + ":" + found) ? Story.bossBefore(f.number, found) : List.of(), fight);
        } else {
            String name = m.enemy.name();       // Some monsters get a few words the first time they're met
            play(seen.add("meet:" + name) ? Story.meeting(name) : List.of(), fight);
        }
    }

    // The player walked up to a sleeping boss (see SleepingBoss). Test Subject X bursts out of its pod
    // as soon as anyone comes near. The Brute, a boulder on the stairs, won't budge without both of the
    // janitor's missing pages; with both, Flor reads the logbook out to it, and it wakes. (Then walking
    // into it starts its battle, like any boss.)
    private void wakeUp(SleepingBoss boss) {
        if (!boss.needsClues) {
            play(Story.POD, () -> boss.wake());
            return;
        }
        Quest q = boss.room.floor.quest;
        String notice = Story.boulderNotice(q.found(gp.flor.clues), q.clues.size());
        if (q.complete(gp.flor.clues)) {
            play(Story.RECITE, () -> boss.wake());
        } else if (seen.add("boulder")) {
            play(Story.BOULDER, () -> gp.showNotice(notice, 240));
        } else {
            gp.showNotice(notice, 240);
        }
    }

    // The player walked up to the Whisperer while she's intangible (see Whisperer). Without the three
    // checkout cards she just keeps whispering blanks; with all three, Flor reads her the names, and she
    // turns solid (then walking into her starts her battle).
    private void atTheWhisperer(Whisperer whisperer) {
        Quest q = whisperer.room.floor.quest;
        String notice = Story.whispererNotice(q.found(gp.flor.clues), q.clues.size());
        if (q.complete(gp.flor.clues)) {
            play(Story.NAMES, () -> whisperer.solidify());
        } else if (seen.add("whisperer")) {
            play(Story.MUMBLING, () -> gp.showNotice(notice, 240));
        } else {
            gp.showNotice(notice, 240);
        }
    }

    // The player walked up to the haze over Floor 3's way down (see Haze): it calls them back to the library
    public void hazeCalls(Floor f) {
        String notice = Story.hazeNotice(f.quest.found(gp.flor.clues), f.quest.clues.size());
        play(seen.add("haze") ? Story.HAZE : List.of(), () -> gp.showNotice(notice, 240));
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
            String notice = m.loot == null ? "The way down is clear."      // (The Brute drops nothing)
                    : "The " + m.enemy.name() + " falls. Something glints where it stood...";
            play(Story.bossDown(m.room.floor.number), () -> gp.showNotice(notice));
        } else if (m.loot != null) {             // A monster carrying something (the Lab Assistant's keycard)
            gp.showNotice("The " + m.enemy.name() + " falls. Something glints where it stood...");
        }
    }

    // The player tried a locked door. The Containment Lab needs its keycard (see Encounters): the first
    // time, a few words, and the locked note every time. The Principal's office opens once the floor's
    // three books are found: without them he won't see Flor; with them, she recites what she's learned,
    // and the door opens (she's given the "key", her appointment).
    public void doorLocked(Room room) {
        if (room.opensWhenQuestDone) {
            Quest q = room.floor.quest;
            if (q.complete(gp.flor.clues)) {
                play(Story.RECITAL, () -> {
                    gp.flor.keys.add(room.lockedWith);
                    gp.showNotice("The office door clicks open.");
                });
                return;
            }
            String notice = Story.officeNotice(q.found(gp.flor.clues), q.clues.size());
            play(seen.add("door:" + room.name) ? Story.doorLocked(room.name) : List.of(), () -> gp.showNotice(notice));
            return;
        }
        play(seen.add("door:" + room.name) ? Story.doorLocked(room.name) : List.of(), () -> gp.showNotice(room.lockedNote));
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
        play(Story.awakening(allCluesFound(), gp.flor.keys.contains(SubjectZero.CALMING_ITEM)), null);
    }

    // Hurt, Subject Zero calls up its saplings (see LastStand); Flor has something to say the first time
    public void saplingsCalled() {
        play(seen.add("saplings") ? Story.FIRST_SAPLINGS : List.of(), () -> gp.showNotice(Story.SAPLINGS, 180));
    }

    // Subject Zero's roots crack the ground around the player for the first time
    public void groundCracks() {
        if (seen.add("roots")) play(Story.FIRST_ROOTS, null);
    }

    // Subject Zero is down: a few words, then the south gate opens and the sun comes up, and the
    // player walks out (see Dawn)
    public void zeroBeaten() {
        boolean remembered = allCluesFound();
        play(Story.ending(remembered), () -> {
            gp.dawn.begin(remembered);
            gp.showNotice(Story.gateOpen(remembered), 300);
        });
    }

    // On the way to the gate: a thought, as the sun comes up
    public void dawnThought(boolean remembered) {
        play(Story.dawnThought(remembered), null);
    }

    // Out through the south gate: the last words, then the ending screen
    public void walkedOut(boolean remembered) {
        play(Story.departure(remembered), () -> gp.end(remembered));
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
    public void lootTaken(Monster m) {
        String supplies = m.loot.supplies();
        String notice = "Found: " + m.loot.key() + (supplies.isEmpty() ? "" : "\n" + supplies);
        List<Line> lines = m.boss ? Story.bossAfter(m.room.floor.number, m.loot.found())
                : Story.found(m.enemy.name(), m.loot.found());
        play(lines, () -> gp.showNotice(notice, 300));
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
