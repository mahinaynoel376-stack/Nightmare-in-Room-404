package nightmare404.story;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nightmare404.entity.Clue;
import nightmare404.world.Floor;

// A floor's quest: find the clues in its side rooms. Every clue found BEFORE the fight takes
// WEAKEN_PER_CLUE of the boss's health away when its battle starts, and gives some supplies. That's what
// makes the side rooms worth the risk. On Floors 4, 3 and 1 the clues are needed: the Brute sleeps as a
// boulder on the stairs down until Kai reads it the janitor's pages (see SleepingBoss), the Whisperer
// can't be touched until Kai brings her the three checkout cards (see Whisperer), and the Principal
// won't see Kai until he's found the floor's three books (see Director.doorLocked). On Floor 2 they're
// not: it's the one floor where everything is optional.
//
// Story-wise, each boss is someone the school forgot, and the clues are pieces of who they were:
// remembering them weakens them.
public class Quest {
    public static final double WEAKEN_PER_CLUE = 0.20;   // 2 clues = the boss starts at 60% health...
    public static final double MOST_WEAKENING = 0.40;    // ...and that's as weak as it gets (3 clues too)

    public final Floor floor;
    public final String title;          // "The Janitor's Logbook"
    public final String things;         // What the clues are, for the tracker: "Pages"
    public final List<Clue> clues = new ArrayList<>();

    public Quest(Floor floor, String title, String things) {
        this.floor = floor;
        this.title = title;
        this.things = things;
    }

    // How many of this floor's clues are among the ones the player has found
    public int found(Set<String> playerClues) {
        int n = 0;
        for (Clue c : clues) {
            if (playerClues.contains(c.id)) n++;
        }
        return n;
    }

    public boolean complete(Set<String> playerClues) {
        return found(playerClues) == clues.size();
    }

    // How much of its health the boss has lost before the fight starts: 0.0, 0.2, 0.4
    public double weakening(Set<String> playerClues) {
        return Math.min(MOST_WEAKENING, WEAKEN_PER_CLUE * found(playerClues));
    }

    // The boss's health when its battle starts
    public int bossStartHp(Set<String> playerClues) {
        int max = floor.boss.enemy.maxHp();
        return (int) Math.round(max * (1 - weakening(playerClues)));
    }
}
