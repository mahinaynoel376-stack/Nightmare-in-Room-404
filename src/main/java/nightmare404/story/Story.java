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

    // ---------------------------------------------------------------- the first fight, in the hallway

    // Stepping out of Room 404 into the hallway for the first time: something drops on Flor, and her
    // first battle starts (an easy one, with the controls explained: see Director)
    public static final List<Line> FIRST_FIGHT = List.of(
            flor("The whole place is a mess.. and reality is twisted."),
            flor("And what is that, are those.. monsters?!"),
            narrate("Something drops from the ceiling and lunges at you!"),
            tip("A fight! Spell words with the letters on the grid: click them, or type them. ENTER (or ATTACK) "
                    + "strikes. Longer words hit much harder."),
            tip("Each turn has a clock: when it runs out, the monster strikes first. SCRAMBLE swaps your letters "
                    + "but costs your turn; shuffling and using your items are free."));

    // Lost that first fight and came back out: it's still there
    public static final List<Line> FIRST_FIGHT_AGAIN = List.of(
            narrate("The crawler is still out here, waiting. It lunges again!"));

    public static final List<Line> AFTER_FIRST_FIGHT = List.of(
            flor("Wow, that was insane.. *panting* At least I can somewhat fend them off.."),
            flor("Let me find the missing pages of the janitor's logbook. Maybe that'll help me understand "
                    + "what's going on here."));

    // ---------------------------------------------------------------- arriving on a floor (the first time)

    public static List<Line> arrive(int floor) {
        return switch (floor) {
            case 3 -> List.of(
                    narrate("Floor 3. The library's big double doors stand open, and from somewhere inside comes a whisper."),
                    think("The way down must be at the other end of the building."),
                    tip("This floor's clues: two old checkout cards, somewhere in the library."));
            case 2 -> List.of(
                    narrate("Floor 2. A crooked LABORATORY sign. The air smells of chemicals, and of something rotten."),
                    narrate("A trail of slime and bloody footprints leads out of the Containment Lab and away down the hall."),
                    think("Something got out of there. Something big."),
                    tip("This floor's clues: two pages of a lab report, in the Chemistry and Biology labs."));
            case 1 -> List.of(
                    narrate("The ground floor. Through the windows: the quadrangle, and beyond it, the gates."),
                    flor("I can see outside! The quad, the gates.. I'm so close!"),
                    flor("So why does it feel like the building doesn't want me to leave?"),
                    think("The front doors are in the middle of the hall. Almost there."),
                    tip("This floor's clues: a staff memo in the Faculty Room, and a register in the Records Room."));
            default -> List.of();
        };
    }

    // Walking into one of the floor's rooms for the first time (not the hallway)
    public static List<Line> firstRoom(int floor) {
        return switch (floor) {
            case 3 -> List.of(
                    flor("It's so quiet in here I can hear my own heartbeat."),
                    flor("*whispering* Okay.. just don't make a sound."),
                    narrate("Somewhere in the dark, a book falls off a shelf."),
                    flor("..Nope. Nope nope nope."));
            case 2 -> List.of(
                    flor("Ugh, that smell.. like chemicals and something that died in here."),
                    flor("Who does experiments in a high school lab in the middle of the night?"),
                    flor("Rule one: don't touch anything green. Rule two: don't touch anything glowing."));
            default -> List.of();
        };
    }

    // Walking into the boss's room for the first time, while it's still there
    public static List<Line> bossRoom(int floor) {
        return switch (floor) {
            case 4 -> List.of(
                    flor("The stairs down.. blocked. Of course they are."),
                    flor("Okay, think. The janitor had the keys. If I find him, I find the way down."),
                    narrate("For a moment, the boulder on the stairs seems to breathe."),
                    flor("..Please don't be what I think it is."));
            case 3 -> List.of(
                    flor("The whispering's coming from back there. It's getting louder.."),
                    flor("It sounds like.. names? It's saying names."));
            case 2 -> List.of(
                    flor("The pods are all smashed. There's glass everywhere."),
                    flor("Pod 2 is empty.. but something's still in here. I can hear it breathing."));
            default -> List.of();
        };
    }

    // Floor 4's way down is blocked by a boulder: the Brute, asleep (see Golem). The first time the player
    // walks up to it without both of the janitor's missing pages:
    public static final List<Line> BOULDER = List.of(
            narrate("A huge boulder sits in the middle of the stairs down, wedged between the rails."),
            narrate("Half buried in the rock: a ring of keys."),
            flor("It won't budge.. not even a little."),
            flor("Those keys.. they look like the janitor's. Maybe his missing pages say what happened up here."),
            tip("Find the janitor's two missing pages on this floor, then come back and read them to the boulder."));

    // Every time, after that
    public static String boulderNotice(int found, int total) {
        return "The boulder won't budge. Logbook pages found: " + found + " of " + total + ".";
    }

    // With both pages: Flor reads the janitor's logbook out to the boulder, and it starts to wake
    public static final List<Line> RECITE = List.of(
            flor("I found your pages. All of them. Let me read you your own words."),
            flor("\"Oct 3. Kids keep staying late in 404. In the morning, nobody remembers their names.\""),
            flor("\"Oct 9. I'm staying up here tonight to see who's doing it. My keys are on my belt.\""),
            flor("\"11:40 PM. Footsteps on the fourth floor again.. If you're reading this, don't stay late. Go home.\""),
            narrate("The boulder shudders. Its cracks glow orange, and something inside the rock groans awake."));

    // Trying the floor's way out without its key, the first time (then the locked note shows)
    public static List<Line> lockedOut(int floor) {
        return switch (floor) {
            case 1 -> List.of(
                    flor("*pulls on the handles* Locked?! Come on.. come ON!"),
                    flor("The Principal's office is right down the hall. Of course HE has the key."));
            default -> List.of();
        };
    }

    // The first fight with one of these monsters: a few words first (keyed by the monster's name)
    public static List<Line> meeting(String monster) {
        return switch (monster) {
            case "Hall Monitor" -> List.of(
                    flor("A hall monitor? At midnight?! I don't have a pass!"),
                    flor("..Wait, I actually do have one. *digs through backpack*"),
                    narrate("The Hall Monitor's arm stretches way, way too far."),
                    flor("Okay, NOT a normal hall monitor!"));
            case "Librarian" -> List.of(
                    flor("Excuse me, ma'am, do you know the way to-"),
                    narrate("The Librarian turns, one bony finger to her lips."),
                    flor("..Sorry! Sorry. I'll be quiet. *backs away slowly*"));
            case "Specimen 7" -> List.of(
                    flor("Is that.. a frog?! Why is it THAT big?!"),
                    flor("The toe tag says seven. Okay. I'm not counting the other six."));
            case "The Mascot" -> List.of(
                    flor("Our school mascot..? Why is it waving at me like that?"),
                    flor("Nobody's in that suit. Nobody's in that suit and it's STILL waving."));
            default -> List.of();
        };
    }

    // ---------------------------------------------------------------- the clues (placed by Quests)

    public static final List<Line> LOGBOOK_OCT_3 = List.of(
            paper("Janitor's logbook, Oct 3", "Kids keep staying late in 404. I send them home at ten. In the "
                    + "morning their desks are empty, and nobody remembers their names. Not even their teachers."),
            flor("Oct 3.. kids staying late in 404? That's MY classroom."),
            flor("Nobody remembers their names.. not even the teachers? That can't be right."),
            flor("*looks back at the empty desks* Whose seats were these.."));
    public static final List<Line> LOGBOOK_OCT_9 = List.of(
            paper("Janitor's logbook, Oct 9", "The attendance sheets are going blank: names fading right off the "
                    + "paper. I'm staying up here tonight to see who's doing it. My keys are on my belt."),
            flor("So the janitor stayed up here to catch whoever was doing it.."),
            flor("\"My keys are on my belt.\" Then where is he now?"),
            flor("I've got a really bad feeling about those stairs."));

    public static final List<Line> CARD_ROOM_404 = List.of(
            paper("Checkout card: A History of Room 404", "Borrowed by: ______, Oct 2. ______, Oct 5. ______, Oct 8. "
                    + "Every name rubbed out. In the margin, someone has pencilled them back in, tiny and faint."),
            flor("A History of Room 404? Since when does my classroom have a history book?"),
            flor("Somebody wrote the names back in.. like they didn't want them to disappear."));
    public static final List<Line> CARD_NIGHT_CLASS = List.of(
            paper("Checkout card: Night Class", "Borrowed by: ______. Returned: never. On the back, the same "
                    + "pencilled names, over and over, as if someone kept saying them so they wouldn't be lost."),
            flor("Night Class.. returned: never."),
            flor("Someone kept saying these names so they wouldn't be lost. *reads them under her breath*"),
            flor("There. Now I'll remember them too."));

    public static final List<Line> REPORT_PAGE_1 = List.of(
            paper("Project REMEMBER, page 1", "What the school forgets, we keep. Two volunteers, both students "
                    + "who stayed late. Subject X: calm. Subject Zero, the first: restless, and growing."),
            flor("Project REMEMBER.. \"What the school forgets, we keep.\""),
            flor("Two volunteers. Students who stayed late. ..Like me."),
            flor("They turned kids into experiments?!"));
    public static final List<Line> REPORT_PAGE_40 = List.of(
            paper("Project REMEMBER, day 40", "Subject Zero broke the glass of pod 2 and walked out. Its tracks "
                    + "go down the stairs and out into the quadrangle, to the trees. X keeps asking us its name."),
            flor("It broke out and went outside.. to the trees?"),
            flor("\"X keeps asking us its name.\" *swallows* That's the saddest thing I've ever read."));

    public static final List<Line> STAFF_MEMO = List.of(
            paper("Memo to all staff", "Students still in the building after midnight are to be struck from the "
                    + "register. Do not discuss them. Do not look for them. Do not remember them. - The Principal"),
            flor("\"Do not remember them.\" The Principal ORDERED it?!"),
            flor("They didn't just forget those students.. they made themselves forget."));
    public static final List<Line> REGISTER = List.of(
            paper("Register, Room 404", "Every name on the list has been struck through. At the bottom, in fresh "
                    + "ink: F L O R. As you watch, the R fades away."),
            flor("That's me. I'm next."),
            flor("No no no.. I'm not fading. I'm right here. My name is Flor!"),
            flor("*grips the register tight* I'm getting out of here before the L goes too."));

    // Picking a clue up again (after giving up undid finding it): no need to read it all again
    public static List<Line> clueAgain(String title) {
        return List.of(narrate("You pick up the " + title.toLowerCase() + " again."));
    }

    // ---------------------------------------------------------------- the bosses

    // Before the fight. What the player says depends on how many of the floor's clues they found.
    public static List<Line> bossBefore(int floor, int found) {
        return switch (floor) {
            case 4 -> List.of(                   // (Its battle only comes after both pages are read to it)
                    flor("Those keys... You're the janitor. You stayed up here to look for them."),
                    narrate("The Brute flinches. For a moment it looks almost human."));
            case 3 -> switch (found) {
                case 0 -> List.of(narrate("At the end of the library, something whispers. You can't make out the words."));
                case 1 -> List.of(narrate("It's whispering names. The same few names, over and over."));
                default -> List.of(flor("Those are the names from the checkout cards. I know them now, too. "
                        + "You don't have to keep them all by yourself."), narrate("The whispering falters."));
            };
            case 2 -> switch (found) {
                case 0 -> List.of(narrate("Something stirs among the pods. It is very, very hungry."));
                case 1 -> List.of(narrate("Test Subject X. A scrap of a student ID is still stuck to what's left of its sleeve."));
                default -> List.of(flor("They called you X because they forgot your name. I'm sorry."),
                        narrate("Subject X hesitates. Its mouth closes, just for a moment."));
            };
            case 1 -> switch (found) {
                case 0 -> List.of(say("Principal", "principal", "Your permanent record is on my desk. Sit down. This won't take long."));
                case 1 -> List.of(narrate("The Principal is striking names out of a register. One of them is yours."),
                        say("Principal", "principal", "Sit. Down."));
                default -> List.of(flor("You struck them all out. Everyone who stayed late. You're not striking me out."),
                        narrate("The Principal's pen snaps in half."));
            };
            default -> List.of();
        };
    }

    // Right after beating the boss (before its loot is picked up)
    public static List<Line> bossDown(int floor) {
        return switch (floor) {
            case 4 -> List.of(
                    flor("*panting* It just.. fell apart. Like it was made out of the stairs."),
                    flor("I'm sorry. I think you were only trying to help us."));
            case 3 -> List.of(flor("It's quiet again. But a different kind of quiet.. lighter."));
            case 2 -> List.of(flor("*catching her breath* It didn't want to hurt me at first.. it just looked lost."));
            case 1 -> List.of(flor("*panting* Detention's over, sir."));
            default -> List.of();
        };
    }

    // Picking up what the boss dropped: what it was (the loot's own text), then what Flor makes of it
    public static List<Line> bossAfter(int floor, String found) {
        List<Line> after = switch (floor) {
            case 4 -> List.of(
                    think("The janitor stayed late to help them. Now I've got the keys."),
                    flor("Okay. Down we go. Don't look back, Flor."));
            case 3 -> List.of(
                    think("It was holding on to their names. I'll hold on to them now."),
                    flor("A key hidden inside a book. Who even does that?"),
                    flor("The east stairs.. the OTHER end of the building. Great. *sigh*"));
            case 2 -> List.of(
                    think("Pod 2 is still empty. Whatever Subject Zero is, it's outside."),
                    flor("LOCKDOWN OVERRIDE. Finally, something in this school that works for me."),
                    flor("One more floor. Just one more."));
            default -> List.of(
                    think("The Master Key. The front doors are right there."),
                    flor("The Master Key.. this is it. Time to get out of here."));
        };
        List<Line> lines = new java.util.ArrayList<>();
        lines.add(narrate(found));
        lines.addAll(after);
        return lines;
    }

    // ---------------------------------------------------------------- the quadrangle

    public static final List<Line> OUTSIDE = List.of(
            narrate("Cold air. The quadrangle is silent, and every gate in the fence is chained shut."),
            narrate("The slime trail crosses the grass and fades out somewhere near the south gate."),
            flor("Fresh air.. I almost forgot what it felt like."),
            flor("Every gate's chained.. there has to be another way out of here."),
            narrate("Then, behind you, something tears itself out of the earth."),
            flor("The trees... They're alive."),
            flor("Oh come ON! Can't ANYTHING in this school just stay normal?!"),
            tip("SPACE to focus: time slows, and you can type. Type the word over a tree to strike it. "
                    + "SPACE again to move. Focus runs out, so pick your moments."),
            tip("Walls, the bus and trees stop your strikes; fences, benches and bushes don't. "
                    + "Keys 1-4 use a First Aid Kit, an Old Textbook, a Red Pen or a Hall Pass."));

    // The first tree felled, and half of them
    public static final List<Line> FIRST_TREE = List.of(
            flor("*panting* One down.. they're slow, but there's so many of them."),
            flor("Keep moving, Flor. Type fast. Don't stop."));
    public static final List<Line> HALF_THE_TREES = List.of(
            flor("Every root I cut.. they all lead the same way. Toward the south gate."),
            flor("Something down there is pulling the strings."));

    // Subject Zero, hurt, calls up its saplings for the first time; the ground cracks for the first time
    public static final List<Line> FIRST_SAPLINGS = List.of(
            flor("More of them?! It's calling them in through the gates!"),
            flor("Focus.. focus.. one word at a time."));
    public static final List<Line> FIRST_ROOTS = List.of(
            narrate("The ground cracks open under your feet!"),
            flor("Move, move, MOVE!"));

    // Notices
    public static final String BREATHING = "Under the leaves by the south gate, something is breathing.";
    public static final String OUT_OF_FOCUS = "You lose your focus. Catch your breath before you try again.";
    public static final String SAPLINGS = "Subject Zero calls up its saplings through the gates!";

    public static String treeFelled(int heal, int left) {
        return "The Hollow Oak falls. You catch your breath: +" + heal + " HP.\n"
                + (left == 1 ? "1 tree left." : left + " trees left.");
    }

    // When the last tree falls
    public static List<Line> awakening(boolean rememberedEveryone) {
        return List.of(
                narrate("The last tree falls. Every root in the quadrangle goes still."),
                narrate("By the south gate, the heap of dead leaves sits up. Every root you cut leads back to it."),
                say("Subject Zero", "subject_zero", "...STAY... IN... YOUR... SEAT..."),
                flor("Subject Zero. The one that broke out of pod 2."),
                rememberedEveryone ? flor("I remember all of them. And I'll remember you.")
                        : flor("Nobody's forgetting me tonight."),
                flor("So you're the first one.. the one everybody forgot the longest."),
                flor("*grips her pen* I'm not sitting back down. Not for you, not for anyone."),
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
                    narrate("On Monday the register for Room 404 is full. Every name is back. Yours too."),
                    flor("The janitor. The names on the cards. Subject X.. I'll say them every day if I have to."),
                    flor("Nobody gets forgotten. Not tonight. Not ever again."));
        }
        return List.of(
                narrate("Subject Zero comes apart into roots and dead leaves. The chain on the south gate falls away."),
                flor("I'm out. I'm actually out."),
                narrate("Dawn comes. You walk home and sleep through the whole day."),
                narrate("On Monday, your seat in Room 404 is gone. Nobody can remember who used to sit there."),
                flor("Did I.. forget something? Someone?"),
                flor("*stares at the empty seat in Room 404* Whose desk was that.."));
    }

    // The ending screen's big word, and the line under it
    public static String endingTitle(boolean remembered) {
        return remembered ? "REMEMBERED" : "FORGOTTEN";
    }

    public static String endingHint(boolean remembered) {
        return remembered ? "You found every clue, and nobody was forgotten."
                : "Find all 8 clues to remember everyone.";
    }

    // Waking up after losing, the first time (the notice below shows every time)
    public static final List<Line> FIRST_WAKE = List.of(
            flor("*gasps* ..I'm back here again? That felt so real."),
            flor("Okay. Deep breath. Try again, smarter this time."));

    // Waking up after losing
    public static final String WAKE_INSIDE = "You wake up with a gasp, back where you came onto this floor...";
    public static final String WAKE_OUTSIDE = "You wake up with a gasp by the main entrance...";
}
