package nightmare404.story;

import static nightmare404.story.Line.*;

import java.util.List;

// Everything the characters say, and every page the player reads, in one place: to change the story,
// change this file. WHEN each piece is shown is decided by Director; WHERE the clues lie, by Quests.
//
// The story so far (the monsters' incident reports in the battles tell the rest):
//   Flor stayed late in Room 404 and fell asleep. Anyone still in the school after midnight gets
//   forgotten: their names fade off every register, and nobody remembers them. The monsters are what
//   the forgotten became. Each floor's boss was someone, and the clues on its floor are pieces of who
//   they were. Remembering them weakens them. Under it all is Project REMEMBER, an experiment in the
//   labs, and its first subject, Subject Zero, who broke out of pod 2 and went to ground in the
//   quadrangle, under the trees the groundskeeper planted for every student who never went home.
//
// Keep each line short (it has to fit the box: about 3 lines of 80 letters), and keep scenes short:
// this is a horror game, and the player wants to get moving again.
public class Story {

    // ---------------------------------------------------------------- the start, in Room 404

    public static final List<Line> INTRO = List.of(
            narrate("Room 404. The lights are off. On the board, in someone else's handwriting: STAY IN YOUR SEAT."),
            flor("...I fell asleep? Where is everyone?"),
            flor("The clock says 11:59. It said 11:59 when I closed my eyes."),
            narrate("On the teacher's desk lies the janitor's logbook. Most of its pages have been torn out."),
            paper("Janitor's logbook, last entry", "11:40 PM. Footsteps on the fourth floor again. Going up to check "
                    + "Room 404. If you're reading this, don't stay late. Go home."),
            think("Too late for that. I need to get out of here: down the stairs and out the front doors."),
            tip("W A S D to walk, Shift to run. E, Space or Enter to read on. J opens your journal."),
            tip("Each floor has clues hidden in its side rooms. Every clue you find before the floor's boss fight "
                    + "takes 20% off the boss's health. The boss holds the key to the way down."));

    // ---------------------------------------------------------------- arriving on a floor (the first time)

    public static List<Line> arrive(int floor) {
        if (floor == 3) {
            return List.of(
                    narrate("Floor 3. The library's big double doors stand open, and from somewhere inside comes a whisper."),
                    think("The way down must be at the other end of the building."),
                    tip("This floor's clues: two old checkout cards, somewhere in the library."));
        }
        if (floor == 2) {
            return List.of(
                    narrate("Floor 2. A crooked LABORATORY sign. The air smells of chemicals, and of something rotten."),
                    narrate("A trail of slime and bloody footprints leads out of the Containment Lab and away down the hall."),
                    think("Something got out of there. Something big."),
                    tip("This floor's clues: two pages of a lab report, in the Chemistry and Biology labs."));
        }
        if (floor == 1) {
            return List.of(
                    narrate("The ground floor. Through the windows: the quadrangle, and beyond it, the gates."),
                    think("The front doors are in the middle of the hall. Almost there."),
                    tip("This floor's clues: a staff memo in the Faculty Room, and a register in the Records Room."));
        }
        return List.of();                      // Floor 4: the opening scene says it all (see INTRO)
    }

    // ---------------------------------------------------------------- the clues (placed by Quests)

    public static final List<Line> LOGBOOK_OCT_3 = List.of(
            paper("Janitor's logbook, Oct 3", "Kids keep staying late in 404. I send them home at ten. In the "
                    + "morning their desks are empty, and nobody remembers their names. Not even their teachers."));
    public static final List<Line> LOGBOOK_OCT_9 = List.of(
            paper("Janitor's logbook, Oct 9", "The attendance sheets are going blank: names fading right off the "
                    + "paper. I'm staying up here tonight to see who's doing it. My keys are on my belt."));

    public static final List<Line> CARD_ROOM_404 = List.of(
            paper("Checkout card: A History of Room 404", "Borrowed by: ______, Oct 2. ______, Oct 5. ______, Oct 8. "
                    + "Every name rubbed out. In the margin, someone has pencilled them back in, tiny and faint."));
    public static final List<Line> CARD_NIGHT_CLASS = List.of(
            paper("Checkout card: Night Class", "Borrowed by: ______. Returned: never. On the back, the same "
                    + "pencilled names, over and over, as if someone kept saying them so they wouldn't be lost."));

    public static final List<Line> REPORT_PAGE_1 = List.of(
            paper("Project REMEMBER, page 1", "What the school forgets, we keep. Two volunteers, both students "
                    + "who stayed late. Subject X: calm. Subject Zero, the first: restless, and growing."));
    public static final List<Line> REPORT_PAGE_40 = List.of(
            paper("Project REMEMBER, day 40", "Subject Zero broke the glass of pod 2 and walked out. Its tracks "
                    + "go down the stairs and out into the quadrangle, to the trees. X keeps asking us its name."));

    public static final List<Line> STAFF_MEMO = List.of(
            paper("Memo to all staff", "Students still in the building after midnight are to be struck from the "
                    + "register. Do not discuss them. Do not look for them. Do not remember them. - The Principal"));
    public static final List<Line> REGISTER = List.of(
            paper("Register, Room 404", "Every name on the list has been struck through. At the bottom, in fresh "
                    + "ink: F L O R. As you watch, the R fades away."),
            flor("That's me. I'm next."));

    // Picking a clue up again (after giving up undid finding it): no need to read it all again
    public static List<Line> clueAgain(String title) {
        return List.of(narrate("You pick up the " + title.toLowerCase() + " again."));
    }

    // ---------------------------------------------------------------- the bosses

    // Before the fight. What the player says depends on how many of the floor's clues they found.
    public static List<Line> bossBefore(int floor, int found) {
        if (floor == 4) {
            if (found == 0) {
                return List.of(narrate("Something huge has wedged itself into the stairwell. It doesn't even look at you."));
            }
            if (found == 1) {
                return List.of(narrate("A ring of keys hangs from the Brute's belt, half buried in rubble. "
                        + "Where have you seen keys like that before?"));
            }
            return List.of(flor("Those keys... You're the janitor. You stayed up here to look for them."),
                    narrate("The Brute flinches. For a moment it looks almost human."));
        }
        if (floor == 3) {
            if (found == 0) {
                return List.of(narrate("At the end of the library, something whispers. You can't make out the words."));
            }
            if (found == 1) {
                return List.of(narrate("It's whispering names. The same few names, over and over."));
            }
            return List.of(flor("Those are the names from the checkout cards. I know them now, too. "
                    + "You don't have to keep them all by yourself."), narrate("The whispering falters."));
        }
        if (floor == 2) {
            if (found == 0) {
                return List.of(narrate("Something stirs among the pods. It is very, very hungry."));
            }
            if (found == 1) {
                return List.of(narrate("Test Subject X. A scrap of a student ID is still stuck to what's left of its sleeve."));
            }
            return List.of(flor("They called you X because they forgot your name. I'm sorry."),
                    narrate("Subject X hesitates. Its mouth closes, just for a moment."));
        }
        if (floor == 1) {
            if (found == 0) {
                return List.of(say("Principal", "principal", "Your permanent record is on my desk. Sit down. This won't take long."));
            }
            if (found == 1) {
                return List.of(narrate("The Principal is striking names out of a register. One of them is yours."),
                        say("Principal", "principal", "Sit. Down."));
            }
            return List.of(flor("You struck them all out. Everyone who stayed late. You're not striking me out."),
                    narrate("The Principal's pen snaps in half."));
        }
        return List.of();
    }

    // Picking up what the boss dropped: what it was (the loot's own text), then what Flor makes of it
    public static List<Line> bossAfter(int floor, String found) {
        Line thought = think("The Master Key. The front doors are right there.");   // Floor 1
        if (floor == 4) thought = think("The janitor stayed late to help them. Now I've got the keys.");
        if (floor == 3) thought = think("It was holding on to their names. I'll hold on to them now.");
        if (floor == 2) thought = think("Pod 2 is still empty. Whatever Subject Zero is, it's outside.");
        return List.of(narrate(found), thought);
    }

    // ---------------------------------------------------------------- the quadrangle

    public static final List<Line> OUTSIDE = List.of(
            narrate("Cold air. The quadrangle is silent, and every gate in the fence is chained shut."),
            narrate("The slime trail crosses the grass and fades out somewhere near the south gate."),
            narrate("Then, behind you, something tears itself out of the earth."),
            flor("The trees... They're alive."),
            tip("SPACE to focus: time slows, and you can type. Type the word over a tree to strike it. "
                    + "SPACE again to move. Focus runs out, so pick your moments."),
            tip("Walls, the bus and trees stop your strikes; fences, benches and bushes don't. "
                    + "Keys 1-4 use a First Aid Kit, an Old Textbook, a Red Pen or a Hall Pass."));

    // Notices
    public static final String BREATHING = "Under the leaves by the south gate, something is breathing.";
    public static final String OUT_OF_FOCUS = "You lose your focus. Catch your breath before you try again.";
    public static final String SAPLINGS = "Subject Zero calls up its saplings through the gates!";

    public static String treeFelled(int heal, int left) {
        String treesLeft = left + " trees left.";
        if (left == 1) treesLeft = "1 tree left.";
        return "The Hollow Oak falls. You catch your breath: +" + heal + " HP.\n" + treesLeft;
    }

    // When the last tree falls
    public static List<Line> awakening(boolean rememberedEveryone) {
        Line answer = flor("Nobody's forgetting me tonight.");   // What Flor says back depends on the clues
        if (rememberedEveryone) answer = flor("I remember all of them. And I'll remember you.");
        return List.of(
                narrate("The last tree falls. Every root in the quadrangle goes still."),
                narrate("By the south gate, the heap of dead leaves sits up. Every root you cut leads back to it."),
                say("Subject Zero", "subject_zero", "...STAY... IN... YOUR... SEAT..."),
                flor("Subject Zero. The one that broke out of pod 2."),
                answer,
                tip("Subject Zero's words are longer. When the ground cracks around you, get out of the way: "
                        + "SPACE to stop focusing, then run."));
    }

    // ---------------------------------------------------------------- the end

    // After Subject Zero. Finding all 8 clues is the good ending.
    public static List<Line> ending(boolean remembered) {
        if (remembered) {
            return List.of(
                    narrate("Subject Zero comes apart into roots, dead leaves and old paper: pages from the register, "
                            + "and every name on them is still there."),
                    flor("I remember them. The janitor. The names on the cards. Subject X. All of them."),
                    narrate("The sky over the south gate turns grey, then gold. Inside, the clock in Room 404 finally ticks over."),
                    narrate("On Monday the register for Room 404 is full. Every name is back. Yours too."));
        }
        return List.of(
                narrate("Subject Zero comes apart into roots and dead leaves. The chain on the south gate falls away."),
                flor("I'm out. I'm actually out."),
                narrate("Dawn comes. You walk home and sleep through the whole day."),
                narrate("On Monday, your seat in Room 404 is gone. Nobody can remember who used to sit there."));
    }

    // The ending screen's big word, and the line under it
    public static String endingTitle(boolean remembered) {
        if (remembered) return "REMEMBERED";
        return "FORGOTTEN";
    }

    public static String endingHint(boolean remembered) {
        if (remembered) return "You found every clue, and nobody was forgotten.";
        return "Find all 8 clues to remember everyone.";
    }

    // Waking up after losing
    public static final String WAKE_INSIDE = "You wake up with a gasp, back where you came onto this floor...";
    public static final String WAKE_OUTSIDE = "You wake up with a gasp by the main entrance...";
}
