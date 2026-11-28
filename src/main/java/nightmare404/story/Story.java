package nightmare404.story;

import static nightmare404.story.Line.*;

import java.util.List;

// Everything the characters say, and every page the player reads, in one place: to change the story,
// change this file. WHEN each piece is shown is decided by Director; WHERE the clues lie, by Quests.
//
// The story so far (the monsters' incident reports in the battles tell the rest):
//   Kai stayed late in Room 404 and fell asleep. Anyone still in the school after midnight gets
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
            kai("...I fell asleep? Where is everyone?"),
            kai("The clock says 11:59. It said 11:59 when I closed my eyes."),
            narrate("On the teacher's desk lies the janitor's logbook. Most of its pages have been torn out."),
            paper("Janitor's logbook, last entry", "11:40 PM. Footsteps on the fourth floor again. Going up to check "
                    + "Room 404. If you're reading this, don't stay late. Go home."),
            think("Too late for that. I need to get out of here: down the stairs and out the front doors."),
            tip("W A S D to walk, Shift to run (only for a few seconds: then you're out of breath and have to walk). "
                    + "E, Space or Enter to read on. J opens your journal."),
            tip("Each floor has clues hidden in its side rooms. Every clue you find before the floor's boss fight "
                    + "takes 20% off the boss's health. The boss stands between you and the way down."));

    // ---------------------------------------------------------------- the first fight, in the hallway

    // Stepping out of Room 404 into the hallway for the first time: something drops on Kai, and his
    // first battle starts (an easy one, with the controls explained: see Director)
    public static final List<Line> FIRST_FIGHT = List.of(
            kai("The whole place is a mess.. and reality is twisted."),
            kai("And what is that, are those.. monsters?!"),
            narrate("Something cold drips onto your shoulder. You look up."));

    // Lost that first fight and came back out: it's still up there
    public static final List<Line> FIRST_FIGHT_AGAIN = List.of(
            narrate("Something drips from the ceiling again. It's still up there, waiting."));

    // The blob has dropped on him (see FirstBlob): the first time, how to fight
    public static final List<Line> FIRST_FIGHT_LANDED = List.of(
            narrate("A blob of black goo splats onto the floor beside you, and lunges!"),
            tip("A fight! Spell words with the letters on the grid: click them, or type them. ENTER (or ATTACK) "
                    + "strikes. Longer words hit much harder."),
            tip("Each turn has a clock: when it runs out, the monster strikes first. SCRAMBLE swaps your letters "
                    + "but costs your turn; shuffling and using your items are free."));

    public static final List<Line> AFTER_FIRST_FIGHT = List.of(
            kai("Wow, that was insane.. *panting* At least I can somewhat fend them off.."),
            kai("Let me find the missing pages of the janitor's logbook. Maybe that'll help me understand "
                    + "what's going on here."));

    // ---------------------------------------------------------------- arriving on a floor (the first time)

    public static List<Line> arrive(int floor) {
        return switch (floor) {
            case 3 -> List.of(
                    narrate("Floor 3. The library's big double doors stand open, and from somewhere inside comes a whisper."),
                    think("The way down must be at the other end of the building."),
                    tip("This floor's clues: three old checkout cards, somewhere in the library."));
            case 2 -> List.of(
                    narrate("Floor 2. A crooked LABORATORY sign. The air smells of chemicals, and of something rotten."),
                    narrate("A trail of slime and bloody footprints leads out of the Containment Lab and away down the hall."),
                    think("Something got out of there. Something big."),
                    tip("This floor's clues: two pages of a lab report, in the Chemistry and Biology labs."));
            case 1 -> List.of(
                    narrate("The ground floor. Through the windows: the quadrangle, and beyond it, the gates."),
                    kai("I can see outside! The quad, the gates.. I'm so close!"),
                    kai("So why does it feel like the building doesn't want me to leave?"),
                    think("The front doors are in the middle of the hall. Almost there."),
                    tip("This floor's clues: three books. A staff handbook in the Faculty Room, a register and a "
                            + "yearbook in the Records Room."));
            default -> List.of();
        };
    }

    // Walking into one of the floor's rooms for the first time (not the hallway)
    public static List<Line> firstRoom(int floor) {
        return switch (floor) {
            case 3 -> List.of(
                    kai("It's so quiet in here I can hear my own heartbeat."),
                    kai("*whispering* Okay.. just don't make a sound."),
                    narrate("Somewhere in the dark, a book falls off a shelf."),
                    kai("..Nope. Nope nope nope."));
            case 2 -> List.of(
                    kai("Ugh, that smell.. like chemicals and something that died in here."),
                    kai("Who does experiments in a high school lab in the middle of the night?"),
                    kai("Rule one: don't touch anything green. Rule two: don't touch anything glowing."));
            default -> List.of();
        };
    }

    // Walking into the boss's room for the first time, while it's still there
    public static List<Line> bossRoom(int floor) {
        return switch (floor) {
            case 4 -> List.of(
                    kai("The stairs down.. blocked. Of course they are."),
                    kai("Okay, think. The janitor had the keys. If I find him, I find the way down."),
                    narrate("For a moment, the boulder on the stairs seems to breathe."),
                    kai("..Please don't be what I think it is."));
            case 3 -> List.of(
                    kai("The whispering's coming from back there. It's getting louder.."),
                    kai("It sounds like.. names? It's saying names."));
            case 2 -> List.of(
                    kai("Every pod is smashed but one. There's glass everywhere."),
                    kai("Pod 2 is empty.. but that last pod.. something's floating in it."));
            default -> List.of();
        };
    }

    // Floor 2: the Containment Lab is locked (its keycard is on the Lab Assistant). The first time the
    // player tries its door:
    public static List<Line> doorLocked(String room) {
        if (room.equals("Principal's Office")) return PRINCIPAL_WONT_SEE_YOU;
        if (!room.equals("Containment Lab")) return List.of();
        return List.of(
                narrate("The Containment Lab's door won't open. A keycard reader beside it blinks red: LAB STAFF ONLY."),
                kai("Lab staff.. that thing in the lab coat, back in the Chemistry Lab?"),
                tip("You don't have to go in: the stairs down are open. But whatever's inside might help you later."));
    }

    // Floor 1: the Principal's office is locked until Kai has found the floor's three books (see
    // Director.doorLocked). The first time he tries the door, the intercom:
    public static final List<Line> PRINCIPAL_WONT_SEE_YOU = List.of(
            narrate("The Principal's door is locked. Beside it, an old intercom crackles into life."),
            say("Principal", "principal", "I do not see students without an appointment. Come back when you have "
                    + "something worth my time."),
            kai("..Did he just call me dumb?"),
            tip("Find the three books on this floor, then come back and show the Principal you've done your homework."));

    // Every time, after that
    public static String officeNotice(int found, int total) {
        return "The Principal will not see you. Books found: " + found + " of " + total + ".";
    }

    // With all three books: Kai tells him what he's learned, and the door opens
    public static final List<Line> RECITAL = List.of(
            kai("I've done my homework. All of it."),
            kai("Rule 12: no student stays after midnight. Your memo: strike them from the register."),
            kai("Your yearbook: scratch them out. I know exactly what you did. Is that smart enough for you?"),
            narrate("The intercom goes silent. Then the office door clicks open."),
            say("Principal", "principal", "...Sit. Down."));

    // Picking up what an ordinary monster dropped: what it was, then what Kai makes of it
    public static List<Line> found(String monster, String found) {
        if (!monster.equals("Lab Assistant")) return List.of(narrate(found));
        return List.of(
                narrate(found),
                kai("A keycard.. CONTAINMENT. That's where all that slime came from."),
                kai("Do I really want to open that door?"));
    }

    // Test Subject X, still sealed in the last whole pod (see SleepingBoss). Walking up to it wakes it:
    public static final List<Line> POD = List.of(
            narrate("Something floats in the last whole pod, curled up in green liquid. A label on the glass: SUBJECT X."),
            kai("It's.. asleep. Okay. Nobody panic. I'll just back away sl-"),
            narrate("Its mouth opens. The glass cracks."));

    // Floor 4's way down is blocked by a boulder: the Brute, asleep (see SleepingBoss). The first time the player
    // walks up to it without both of the janitor's missing pages:
    public static final List<Line> BOULDER = List.of(
            narrate("A huge boulder sits in the middle of the stairs down, wedged between the rails."),
            narrate("Half buried in the rock: a ring of keys."),
            kai("It won't budge.. not even a little."),
            kai("Those keys.. they look like the janitor's. Maybe his missing pages say what happened up here."),
            tip("Find the janitor's two missing pages on this floor, then come back and read them to the boulder."));

    // Every time, after that
    public static String boulderNotice(int found, int total) {
        return "The boulder won't budge. Logbook pages found: " + found + " of " + total + ".";
    }

    // With both pages: Kai reads the janitor's logbook out to the boulder, and it starts to wake
    public static final List<Line> RECITE = List.of(
            kai("I found your pages. All of them. Let me read you your own words."),
            kai("\"Oct 3. Kids keep staying late in 404. In the morning, nobody remembers their names.\""),
            kai("\"Oct 9. I'm staying up here tonight to see who's doing it. My keys are on my belt.\""),
            kai("\"11:40 PM. Footsteps on the fourth floor again.. If you're reading this, don't stay late. Go home.\""),
            narrate("The boulder shudders. Its cracks glow orange, and something inside the rock groans awake."));

    // Floor 3's way down (the east stairs) is lost in a haze until the Whisperer is beaten (see Haze).
    // The first time the player walks up to it:
    public static final List<Line> HAZE = List.of(
            narrate("A thick grey haze hangs over the stairs down. It's freezing, and it pushes you back."),
            narrate("From somewhere back in the library: whispering. It sounds like it's calling you."),
            kai("..It wants me to go back to the library?"));

    // Every time, after that
    public static String hazeNotice(int found, int total) {
        return "The haze won't let you through. Back in the library, something keeps whispering. "
                + "Checkout cards found: " + found + " of " + total + ".";
    }

    // The Whisperer, still intangible (see Whisperer), the first time the player walks up to her
    public static final List<Line> MUMBLING = List.of(
            narrate("The Whisperer drifts between the last shelves, barely there. Your hand passes right through her."),
            say("Whisperer", "whisperer", "...______, Oct 2... ______, Oct 5... don't let them go... ______..."),
            kai("She's reading out names.. but they're all blacked out. She can't remember them either."),
            kai("The head librarian.. still reading them, so they won't be lost for good."),
            tip("Find the three checkout cards in the library. Their names will fill in her blanks."));

    // Every time, after that
    public static String whispererNotice(int found, int total) {
        return "She keeps whispering names you can't make out. Checkout cards found: " + found + " of " + total + ".";
    }

    // With all three cards: Kai reads the names out to her, and she turns solid
    public static final List<Line> NAMES = List.of(
            kai("I found the cards. All three. You don't have to read them alone any more."),
            kai("Oct 2, Oct 5, Oct 8. Night Class. The Last Bell. I'll read every name with you."),
            narrate("Kai reads the names out, one by one. The whispering falters, then stops."),
            narrate("The Whisperer turns to face you, and for the first time, she looks solid."));

    // Trying the floor's way out without its key, the first time (then the locked note shows)
    public static List<Line> lockedOut(int floor) {
        return switch (floor) {
            case 1 -> List.of(
                    kai("*pulls on the handles* Locked?! Come on.. come ON!"),
                    kai("The Principal's office is right down the hall. Of course HE has the key."));
            default -> List.of();
        };
    }

    // The first fight with one of these monsters: a few words first (keyed by the monster's name)
    public static List<Line> meeting(String monster) {
        return switch (monster) {
            case "Hall Monitor" -> List.of(
                    kai("A hall monitor? At midnight?! I don't have a pass!"),
                    kai("..Wait, I actually do have one. *digs through backpack*"),
                    narrate("The Hall Monitor's arm stretches way, way too far."),
                    kai("Okay, NOT a normal hall monitor!"));
            case "Librarian" -> List.of(
                    kai("Excuse me, ma'am, do you know the way to-"),
                    narrate("The Librarian turns, one bony finger to her lips."),
                    kai("..Sorry! Sorry. I'll be quiet. *backs away slowly*"));
            case "Specimen 7" -> List.of(
                    kai("Is that.. a frog?! Why is it THAT big?!"),
                    kai("The toe tag says seven. Okay. I'm not counting the other six."));
            case "The Mascot" -> List.of(
                    kai("Our school mascot..? Why is it waving at me like that?"),
                    kai("Nobody's in that suit. Nobody's in that suit and it's STILL waving."));
            default -> List.of();
        };
    }

    // ---------------------------------------------------------------- the clues (placed by Quests)

    public static final List<Line> LOGBOOK_OCT_3 = List.of(
            paper("Janitor's logbook, Oct 3", "Kids keep staying late in 404. I send them home at ten. In the "
                    + "morning their desks are empty, and nobody remembers their names. Not even their teachers."),
            kai("Oct 3.. kids staying late in 404? That's MY classroom."),
            kai("Nobody remembers their names.. not even the teachers? That can't be right."),
            kai("*looks back at the empty desks* Whose seats were these.."));
    public static final List<Line> LOGBOOK_OCT_9 = List.of(
            paper("Janitor's logbook, Oct 9", "The attendance sheets are going blank: names fading right off the "
                    + "paper. I'm staying up here tonight to see who's doing it. My keys are on my belt."),
            kai("So the janitor stayed up here to catch whoever was doing it.."),
            kai("\"My keys are on my belt.\" Then where is he now?"),
            kai("I've got a really bad feeling about those stairs."));

    public static final List<Line> CARD_ROOM_404 = List.of(
            paper("Checkout card: A History of Room 404", "Borrowed by: ______, Oct 2. ______, Oct 5. ______, Oct 8. "
                    + "Every name rubbed out. In the margin, someone has pencilled them back in, tiny and faint."),
            kai("A History of Room 404? Since when does my classroom have a history book?"),
            kai("Somebody wrote the names back in.. like they didn't want them to disappear."));
    public static final List<Line> CARD_NIGHT_CLASS = List.of(
            paper("Checkout card: Night Class", "Borrowed by: ______. Returned: never. On the back, the same "
                    + "pencilled names, over and over, as if someone kept saying them so they wouldn't be lost."),
            kai("Night Class.. returned: never."),
            kai("Someone kept saying these names so they wouldn't be lost. *reads them under his breath*"),
            kai("There. Now I'll remember them too."));

    public static final List<Line> CARD_LAST_BELL = List.of(
            paper("Checkout card: The Last Bell", "Borrowed by: ______, ______ and ______, the night of the last bell. "
                    + "Returned: never. In the margin, in shaky pencil: I KEEP READING THEM SO THEY STAY."),
            kai("\"I keep reading them so they stay..\" The librarian wrote this."),
            kai("She's been reading their names out loud this whole time. So nobody forgets them."));

    public static final List<Line> REPORT_PAGE_1 = List.of(
            paper("Project REMEMBER, page 1", "What the school forgets, we keep. Two volunteers, both students "
                    + "who stayed late. Subject X: calm. Subject Zero, the first: restless, and growing."),
            kai("Project REMEMBER.. \"What the school forgets, we keep.\""),
            kai("Two volunteers. Students who stayed late. ..Like me."),
            kai("They turned kids into experiments?!"));
    public static final List<Line> REPORT_PAGE_40 = List.of(
            paper("Project REMEMBER, day 40", "Subject Zero broke the glass of pod 2 and walked out. Its tracks "
                    + "go down the stairs and out into the quadrangle, to the trees. X keeps asking us its name."),
            kai("It broke out and went outside.. to the trees?"),
            kai("\"X keeps asking us its name.\" *swallows* That's the saddest thing I've ever read."));

    public static final List<Line> STAFF_HANDBOOK = List.of(
            paper("Staff Handbook, Rule 12", "No student is to remain in the building after midnight. Students who do "
                    + "will be dealt with by the Principal, personally."),
            paper("Memo to all staff (tucked inside)", "Students still in the building after midnight are to be struck "
                    + "from the register. Do not discuss them. Do not look for them. Do not remember them. - The Principal"),
            kai("\"Do not remember them.\" The Principal ORDERED it?!"),
            kai("They didn't just forget those students.. they made themselves forget."));
    public static final List<Line> REGISTER = List.of(
            paper("Register, Room 404", "Every name on the list has been struck through. At the bottom, in fresh "
                    + "ink: F L O R. As you watch, the R fades away."),
            kai("That's me. I'm next."),
            kai("No no no.. I'm not fading. I'm right here. My name is Kai!"),
            kai("*grips the register tight* I'm getting out of here before the L goes too."));

    public static final List<Line> YEARBOOK = List.of(
            paper("Yearbook: Room 404", "Every face in the class photo has been scratched out with a pen. One page is "
                    + "half torn out. On it: a photo of you, half rubbed away. KAI, CLASS OF ____."),
            kai("That's.. me. Half of me."),
            kai("He's not just forgetting them.. he's grading them out of existence."));

    // Picking a clue up again (after giving up undid finding it): no need to read it all again
    public static List<Line> clueAgain(String title) {
        return List.of(narrate("You pick up the " + title.toLowerCase() + " again."));
    }

    // ---------------------------------------------------------------- the bosses

    // Before the fight. What the player says depends on how many of the floor's clues they found.
    public static List<Line> bossBefore(int floor, int found) {
        return switch (floor) {
            case 4 -> List.of(                   // (Its battle only comes after both pages are read to it)
                    kai("Those keys... You're the janitor. You stayed up here to look for them."),
                    narrate("The Brute flinches. For a moment it looks almost human."));
            case 3 -> List.of(                   // (Her battle only comes after the cards' names are read to her)
                    kai("I know their names now, too. You can let them rest."),
                    narrate("The Whisperer shrieks, and every shelf in the library rattles."));
            case 2 -> switch (found) {
                case 0 -> List.of(narrate("Something stirs among the pods. It is very, very hungry."));
                case 1 -> List.of(narrate("Test Subject X. A scrap of a student ID is still stuck to what's left of its sleeve."));
                default -> List.of(kai("They called you X because they forgot your name. I'm sorry."),
                        narrate("Subject X hesitates. Its mouth closes, just for a moment."));
            };
            case 1 -> List.of(                   // (He only sees him once he's found all three books)
                    say("Principal", "principal", "Your permanent record is on my desk. Since you're so clever, let's discuss it."),
                    kai("You struck them all out. Everyone who stayed late. You're not striking me out."),
                    narrate("The Principal's pen snaps in half."));
            default -> List.of();
        };
    }

    // Right after beating the boss (before its loot is picked up)
    public static List<Line> bossDown(int floor) {
        return switch (floor) {
            case 4 -> List.of(                   // (It drops nothing: beating it clears the way)
                    kai("*panting* It just.. fell apart. Like it was made out of the stairs."),
                    kai("I'm sorry. I think you were only trying to help us."),
                    narrate("The rubble settles and rolls aside. The stairs down are clear."),
                    kai("Okay. Down we go. Don't look back, Kai."));
            case 3 -> List.of(
                    kai("It's quiet again. But a different kind of quiet.. lighter."),
                    narrate("Far off at the east end of the floor, the haze over the stairs thins and drifts away."));
            case 2 -> List.of(kai("*catching his breath* It didn't want to hurt me at first.. it just looked lost."));
            case 1 -> List.of(kai("*panting* Detention's over, sir."));
            default -> List.of();
        };
    }

    // Picking up what the boss dropped: what it was (the loot's own text), then what Kai makes of it
    public static List<Line> bossAfter(int floor, String found) {
        List<Line> after = switch (floor) {
            case 3 -> List.of(
                    think("She was holding on to their names. I'll hold on to them now."),
                    kai("Every name.. finally written down where nobody can rub it out."),
                    kai("The east stairs.. the OTHER end of the building. Great. *sigh*"));
            case 2 -> List.of(
                    think("Pod 2 is still empty. Whatever Subject Zero is, it's outside."),
                    kai("Its brother.. Subject Zero is X's brother?"),
                    kai("If some part of it still remembers him.. maybe this will calm it down."));
            default -> List.of(
                    think("The Master Key. The front doors are right there."),
                    kai("The Master Key.. this is it. Time to get out of here."));
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
            kai("Fresh air.. I almost forgot what it felt like."),
            kai("Every gate's chained.. there has to be another way out of here."),
            narrate("Then, behind you, something tears itself out of the earth."),
            kai("The trees... They're alive."),
            kai("Oh come ON! Can't ANYTHING in this school just stay normal?!"),
            tip("SPACE to focus: time slows, and you can type. Type the word over a tree to strike it. "
                    + "SPACE again to move. Focus runs out, so pick your moments."),
            tip("Walls, the bus and trees stop your strikes; fences, benches and bushes don't. "
                    + "Keys 1-4 use a First Aid Kit, an Old Textbook, a Red Pen or a Hall Pass."));

    // The first tree felled, and half of them
    public static final List<Line> FIRST_TREE = List.of(
            kai("*panting* One down.. they're slow, but there's so many of them."),
            kai("Keep moving, Kai. Type fast. Don't stop."));
    public static final List<Line> HALF_THE_TREES = List.of(
            kai("Every root I cut.. they all lead the same way. Toward the south gate."),
            kai("Something down there is pulling the strings."));

    // Subject Zero, hurt, calls up its saplings for the first time; the ground cracks for the first time
    public static final List<Line> FIRST_SAPLINGS = List.of(
            kai("More of them?! It's calling them in through the gates!"),
            kai("Focus.. focus.. one word at a time."));
    public static final List<Line> FIRST_ROOTS = List.of(
            narrate("The ground cracks open under your feet!"),
            kai("Move, move, MOVE!"));

    // Notices
    public static final String BREATHING = "Under the leaves by the south gate, something is breathing.";
    public static final String OUT_OF_FOCUS = "You lose your focus. Catch your breath before you try again.";
    public static final String SAPLINGS = "Subject Zero calls up its saplings through the gates!";

    public static String treeFelled(int heal, int left) {
        return "The Hollow Oak falls. You catch your breath: +" + heal + " HP.\n"
                + (left == 1 ? "1 tree left." : left + " trees left.");
    }

    // When the last tree falls
    // calmed: the player has Subject X's tentacle (see SubjectZero.CALMING_ITEM)
    public static List<Line> awakening(boolean rememberedEveryone, boolean calmed) {
        List<Line> lines = new java.util.ArrayList<>(List.of(
                narrate("The last tree falls. Every root in the quadrangle goes still."),
                narrate("By the south gate, the heap of dead leaves sits up. Every root you cut leads back to it."),
                say("Subject Zero", "subject_zero", "...STAY... IN... YOUR... SEAT..."),
                kai("Subject Zero. The one that broke out of pod 2."),
                rememberedEveryone ? kai("I remember all of them. And I'll remember you.")
                        : kai("Nobody's forgetting me tonight."),
                kai("So you're the first one.. the one everybody forgot the longest."),
                kai("*grips his pen* I'm not sitting back down. Not for you, not for anyone.")));
        if (calmed) {
            lines.add(kai("Wait.. I've got something of your brother's."));
            lines.add(narrate("Kai holds up Subject X's tentacle. Subject Zero freezes, and every root in the "
                    + "quadrangle goes slack."));
            lines.add(narrate("It still comes for you. But slower now, as if some part of it is trying to remember."));
        }
        lines.add(tip("Subject Zero's words are longer. When the ground cracks around you, get out of the way: "
                + "SPACE to stop focusing, then run."));
        return lines;
    }

    // ---------------------------------------------------------------- the end

    // After Subject Zero. Finding every clue is the good ending. It comes in three parts: these words
    // as it falls; then the south gate opens and the player walks out as the sun comes up (a thought on
    // the way, dawnThought); then, through the gate, the last words (departure) and the ending screen.
    public static List<Line> ending(boolean remembered) {
        if (remembered) {
            return List.of(
                    narrate("Subject Zero comes apart into roots, dead leaves and old paper: pages from the register, "
                            + "and every name on them is still there."),
                    kai("I remember them. The janitor. The names on the cards. Subject X. All of them."),
                    narrate("Somewhere a padlock drops. The chain on the south gate falls away, and over the wall "
                            + "the sky is turning grey."),
                    think("It's over. I can go home."));
        }
        return List.of(
                narrate("Subject Zero comes apart into roots and dead leaves. The chain on the south gate falls away."),
                kai("I'm out. I'm actually out."),
                narrate("Dawn comes, but the sky over the wall is red. Your head is pounding, and the ground "
                        + "won't keep still."),
                think("Just.. get to the gate."));
    }

    // The notice as the south gate opens
    public static String gateOpen(boolean remembered) {
        return remembered ? "The south gate is open. Walk out into the morning."
                : "The south gate is open. Your head is spinning.. get to the gate.";
    }

    // Halfway to the gate, as the sun comes up
    public static List<Line> dawnThought(boolean remembered) {
        if (remembered) {
            return List.of(narrate("The sky over the south gate turns gold. The stumps, the dead leaves, the broken "
                    + "fence: in the light, it's just a courtyard again."));
        }
        return List.of(kai("*sways* Why does it feel like I'm leaving someone behind.."));
    }

    // Out through the south gate: the last words before the ending screen
    public static List<Line> departure(boolean remembered) {
        if (remembered) {
            return List.of(
                    narrate("You step out through the south gate into the morning. Behind you, the clock in "
                            + "Room 404 finally ticks over."),
                    narrate("On Monday the register for Room 404 is full. Every name is back. Yours too."),
                    kai("The janitor. The names on the cards. Subject X.. I'll say them every day if I have to."),
                    kai("Nobody gets forgotten. Not tonight. Not ever again."));
        }
        return List.of(
                narrate("You stumble out through the south gate. You don't remember the walk home. "
                        + "You sleep through the whole day."),
                narrate("On Monday, your seat in Room 404 is gone. Nobody can remember who used to sit there."),
                kai("Did I.. forget something? Someone?"),
                kai("*stares at the empty seat in Room 404* Whose desk was that.."));
    }

    // The ending screen's big word, and the line under it
    public static String endingTitle(boolean remembered) {
        return remembered ? "REMEMBERED" : "FORGOTTEN";
    }

    public static String endingHint(boolean remembered) {
        return remembered ? "You found every clue, and nobody was forgotten."
                : "Find every clue to remember everyone.";
    }

    // Waking up after losing, the first time (the notice below shows every time)
    public static final List<Line> FIRST_WAKE = List.of(
            kai("*gasps* ..I'm back here again? That felt so real."),
            kai("Okay. Deep breath. Try again, smarter this time."));

    // Waking up after losing
    public static final String WAKE_INSIDE = "You wake up with a gasp, back where you came onto this floor...";
    public static final String WAKE_OUTSIDE = "You wake up with a gasp by the main entrance...";
}
