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
//
// Some conversations lead to something when they're over: a boss's battle, a notice, the ending.
// That's noted in the "...After" fields below, and done in dialogueOver when the conversation ends.
public class Director {
    private final GamePanel gp;
    private final Set<String> seen = new HashSet<>();

    // What happens when the conversation on show is over (null or false: nothing)
    private Monster battleAfter;        // This monster's battle starts
    private String noticeAfter;         // This notice shows at the bottom of the screen
    private boolean journalAfter;       // The journal opens again (after reading a clue again)
    private boolean endingAfter;        // The ending screen shows

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
            play(Story.INTRO);
            return;
        }
        Room room = gp.currentRoom;
        if (room.outdoors) {
            if (seen.add("outside")) {
                play(Story.OUTSIDE);
                return;
            }
            SubjectZero zero = gp.world.finalBoss;   // Walking right past it while it sleeps: a hint, once
            if (zero != null && zero.asleep() && near(zero, 3 * 32) && seen.add("breathing")) {
                gp.showNotice(Story.BREATHING, 240);
            }
        } else if (seen.add("floor" + room.floor.number)) {
            play(Story.arrive(room.floor.number));
        }
    }

    // The player walked into a monster. A floor's boss says something first (once for each number of
    // clues found, so it reacts to what the player knows); then the battle starts.
    public void beforeBattle(Monster m) {
        if (m.boss && m == m.room.floor.boss) {
            Floor f = m.room.floor;
            int found = 0;
            if (f.quest != null) found = f.quest.found(gp.flor.clues);
            battleAfter = m;
            if (seen.add("boss" + f.number + ":" + found)) {
                play(Story.bossBefore(f.number, found));
            } else {
                play(new ArrayList<Line>());          // Said it already: straight to the battle
            }
        } else {
            gp.startBattle(m);
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
        if (m.boss) gp.showNotice("The " + m.enemy.name() + " falls. Something glints where it stood...");
    }

    // Outside, a Hollow Oak was felled (see LastStand). When the last one goes, Subject Zero wakes.
    public void treeFelled(int heal, int left) {
        if (left > 0) {
            gp.showNotice(Story.treeFelled(heal, left), 240);
            return;
        }
        SubjectZero zero = gp.world.finalBoss;
        zero.wake();
        gp.showNotice("", 0);
        play(Story.awakening(allCluesFound()));
    }

    // Subject Zero is down: the last words, then the ending screen
    public void zeroBeaten() {
        endingAfter = true;
        play(Story.ending(allCluesFound()));
    }

    // The player walked over a clue: read it, take what's with it, and see how the boss is weakened
    public void clueFound(Clue clue) {
        Player p = gp.flor;
        p.clues.add(clue.id);
        p.addSupplies(clue.supplies);
        List<Line> lines = Story.clueAgain(clue.title);   // Read before (then given up): one line
        if (seen.add("clue:" + clue.id)) lines = clue.pages;
        Floor f = clue.room.floor;
        Quest q = f.quest;
        String note = q.things + " found: " + q.found(p.clues) + " of " + q.clues.size() + ".";
        if (f.boss != null && !f.boss.defeated) {
            note += " The " + f.boss.enemy.name() + " will start its fight "
                    + Math.round(q.weakening(p.clues) * 100) + "% weaker.";
        }
        String supplies = clue.supplies.supplies();
        noticeAfter = note;
        if (!supplies.isEmpty()) noticeAfter = note + "\nFound with it: " + supplies;
        play(lines);
    }

    // The player picked up what a boss dropped
    public void lootTaken(Monster boss) {
        noticeAfter = "Found: " + boss.loot.key() + "\n" + boss.loot.supplies();
        play(Story.bossAfter(boss.room.floor.number, boss.loot.found()));
    }

    // Reading a clue again from the journal (it opens again afterwards)
    public void reread(Clue clue) {
        journalAfter = true;
        play(clue.pages);
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

    // Shows a conversation. If there's nothing to show (or no screen, in the checks), whatever was
    // waiting for it happens straight away.
    private void play(List<Line> lines) {
        gp.dialogue.open(lines);
        if (!gp.dialogue.isOpen()) dialogueOver();
    }

    // The conversation is over (GamePanel calls this the frame the dialogue box closes): do whatever
    // was waiting for it
    public void dialogueOver() {
        if (noticeAfter != null) {
            gp.showNotice(noticeAfter, 300);
            noticeAfter = null;
        }
        if (journalAfter) {
            journalAfter = false;
            gp.journal.open();
        }
        if (endingAfter) {
            endingAfter = false;
            gp.end(allCluesFound());
        }
        if (battleAfter != null) {
            Monster m = battleAfter;
            battleAfter = null;
            gp.startBattle(m);
        }
    }
}
