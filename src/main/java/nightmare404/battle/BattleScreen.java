package nightmare404.battle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

    
// The word battle: spell words from a 4 x 4 grid of letters to hurt the monster, while it hurts you back.
// This is the battle engine from the Battle_Screens folder. The 12 original screens were the same code
// 12 times; only their monsters and their background differed. So the engine lives here once, each
// monster is an EnemyDef in one of the *Enemies files (FourthFloorEnemies, LibraryEnemies,
// LaboratoryEnemies, LobbyEnemies), and each background is a Backdrop (ClassroomBackdrop, LabBackdrop...).
// The game opens a battle with BattleScreen.fight(...) when the player touches a monster (see Monster).
// Run this class on its own (main) to try a fight without the rest of the game.
public class BattleScreen {

    

    // ---- The player ----
    static final String HERO_NAME = "Flor";     
    static final String HERO_SPRITE = "/characters/player/down_idle";   // The game's own player sprite    
    public static final int HERO_MAX_HP = 100;          

    // ---- First aid kits (the healing item, bottom left) ----
    public static final int START_KITS = 3;             
    static final int KIT_HEAL = 30;              

    // ---- Player damage ----
    static final double DAMAGE_PER_LETTER = 2.5; 
    static final int HEAL_TILE_HP = 10;          

    // ---- Items in the lockers (bottom left) ----
    // The player clicks an item during their turn to use one up (it doesn't cost the turn).
    // Using the same item more than once in a turn stacks, up to the MAX.
    static final double TEXTBOOK_PER_USE = 0.25;  // Old Textbook: the next enemy hit does 25% less damage
    static final double TEXTBOOK_MAX = 0.75;      //   ...stacking up to 75% less
    static final double RED_PEN_PER_USE = 0.25;   // Red Pen: the player's next attack does 25% more damage
    static final double RED_PEN_MAX = 1.00;       //   ...stacking up to +100% (double damage)
    static final int HALL_PASS_PER_USE = 50;      // Hall Pass: 50% chance to dodge the next enemy attack
    static final int HALL_PASS_MAX = 100;         //   ...stacking up to 100% (a sure dodge)

    // ---- The turn timer ----
    // The player has a limited time to make their move. When it runs out, the word they were spelling
    // is dropped and the monster takes its turn. Weak monsters give plenty of time, strong ones very
    // little: it goes by the monster's health, TURN_MOST seconds for 80 HP or less (the 4th floor's),
    // 5 seconds less for every 5 HP more, and never less than TURN_LEAST (the Principal, at 120 HP).
    // The clock only runs on the player's turn (not while the monster attacks, nor with the menu open);
    // Shuffle and the items are free, so they don't stop it.
    static final boolean TURN_TIMER = true;      // false turns it off
    static final int TURN_MOST = 45, TURN_LEAST = 15;
    static final int TICKS_PER_SECOND = 62;      // The battle's clock ticks every 16 ms

    public static int turnSeconds(EnemyDef enemy) {
        return Math.max(TURN_LEAST, Math.min(TURN_MOST, TURN_MOST - (enemy.maxHp() - 80)));
    }

    // ---- Atmosphere ----
    static final boolean LIGHT_FLICKER = true;   // the ceiling light flickers now and then; false turns it off

    

    // =====================================================================================
    //  ENEMIES  
    // =====================================================================================

  
    public record Attack(String name, int damage, int scratch, int heal, int weight, String desc) {}

  
    public record EnemyDef(String name, String sprite, int maxHp, String room, String notes, List<Attack> attacks,
                           Backdrop backdrop) {}

    // A monster: what it's called, its sprite (name.png, plus name_attack.png and name_hurt.png),
    // its health, the sign shown at the top of the battle, a note for the incident report, its attacks,
    // and the background it's fought in. The monsters themselves are in the *Enemies files.
    public record BattleResult(boolean won, int heroHp, int kitsLeft) {}

  
    public static class Items {
        public int textbooks, redPens, hallPasses;

        public Items() { }
        public Items(int textbooks, int redPens, int hallPasses) {
            this.textbooks = textbooks; this.redPens = redPens; this.hallPasses = hallPasses;
        }
    }

   
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Word Battle");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            testFight(frame);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static void testFight(JFrame frame) {
        fightInOrder(frame, List.of(FourthFloorEnemies.DESK_MIMIC, FourthFloorEnemies.CHALK_WRAITH), 0, HERO_MAX_HP, START_KITS, new Items(1, 2, 0), result -> {
            System.out.println("All fights over: " + result);
            testFight(frame);
        });
    }

       // One fight, in the game's window: shows the battle in place of whatever the window shows now,
    // and calls whenDone with the result when the player wins, or loses and gives up.
    // (The battle keeps its own copy of hp and kits; items are used up in place.)
    // The same, with the monster starting the fight already hurt: foeHp out of its maxHp. A floor's boss
    // starts weaker for every clue found on that floor (see nightmare404.story.Quest). Try Again keeps it.
    public static void fight(JFrame frame, EnemyDef enemy, int foeHp, int hp, int kits, Items items, Consumer<BattleResult> whenDone) {
        BattlePanel battle = new BattlePanel(enemy, foeHp, hp, kits, items, whenDone);
        frame.setContentPane(battle);
        frame.revalidate();
        battle.start();
    }

    public static void fight(JFrame frame, EnemyDef enemy, int hp, int kits, Items items, Consumer<BattleResult> whenDone) {
        fightInOrder(frame, List.of(enemy), 0, hp, kits, items, whenDone);
    }

    // A picture of a fight against this enemy, frames frames in, without opening a window
    // (for the debug tools and checks: see nightmare404.debug.BattleScreensDebug).
    public static BufferedImage preview(EnemyDef enemy, int frames) {
        BattlePanel battle = new BattlePanel(enemy, HERO_MAX_HP, START_KITS, new Items(1, 1, 1), null);
        battle.setSize(BattlePanel.W, BattlePanel.H);
        for (int i = 0; i < frames; i++) battle.tick();
        BufferedImage image = new BufferedImage(BattlePanel.W, BattlePanel.H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        battle.paint(g);
        g.dispose();
        return image;
    }

    // For the checks: a fight where the player does nothing for `seconds` seconds. Returns how many
    // turns the monster took meanwhile (it gets one every time the player's time runs out).
    public static int enemyTurnsWhileIdle(EnemyDef enemy, double seconds) {
        BattlePanel battle = new BattlePanel(enemy, HERO_MAX_HP, START_KITS, new Items(), null);
        for (int i = 0; i < seconds * TICKS_PER_SECOND; i++) battle.tick();
        return battle.enemyTurns;
    }

    static void fightInOrder(JFrame frame, List<EnemyDef> monsters, int index, int hp, int kits, Items items,
                             Consumer<BattleResult> whenDone) {
        BattlePanel battle = new BattlePanel(monsters.get(index), hp, kits, items, result -> {
            if (result.won() && index + 1 < monsters.size()) {
                fightInOrder(frame, monsters, index + 1, result.heroHp(), result.kitsLeft(), items, whenDone); // next monster
            } else {
                whenDone.accept(result); // lost, or beat the last monster
            }
        });
        frame.setContentPane(battle);
        frame.revalidate();
        battle.start();
    }




   
    // The battle's files are in src/main/resources/battle; a name starting with / is a full path.
    static java.io.InputStream resource(String name) {
        return BattleScreen.class.getResourceAsStream(name.startsWith("/") ? name : "/battle/" + name);
    }

    // =====================================================================================
    //  Dictionary and letters
    // =====================================================================================

    static Set<String> dictionary;
    static boolean usingFallbackWords;

    public static synchronized Set<String> dictionary() {
        if (dictionary != null) return dictionary;
        Set<String> words = new HashSet<>(250_000);
        InputStream in = resource("words.txt");
        if (in == null) System.err.println("Could not find /battle/words.txt");
        else {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String w = line.trim().toLowerCase();
                    if (w.length() >= 3 && w.chars().allMatch(c -> c >= 'a' && c <= 'z')) words.add(w);
                }
            } catch (IOException e) {
                System.err.println("Could not read words.txt: " + e.getMessage());
            }
        }
        usingFallbackWords = words.size() < 1000;
        if (usingFallbackWords) words.addAll(Arrays.asList(FALLBACK_WORDS.split(" ")));
        dictionary = words;
        return words;
    }

    /**Backup if textfile is missing. */
    static final String FALLBACK_WORDS = "and ant ape arm art ash bad bag bat bed bee big bit box boy bug bus cab can cap car cat cow cry cup cut "
            + "day den dig dog dot dry ear eat egg elf end eye fan far fat fig fin fit fly fog fox fun fur gas gem get gum gun hat hen hit hog "
            + "hop hot hug ice ink jam jar jet job jog joy key kid kit lab lap leg let lid lip log lot mad man map mat men mix mob mop mud mug "
            + "nap net new nod nut oak oar odd oil old one orb owl pan pat pen pet pie pig pin pit pot rag ram ran rat raw red rib rid rim rip rot "
            + "row rub rug run sad sat saw sea set sip sit six sky sob son sun tag tan tap tar tea ten tie tin tip toe ton top toy tub tug two "
            + "van wax web wet wig win wit yak yam yes zip zoo bark bear bell bird boat bone book cake coin cold door dust fire fish frog game "
            + "gate gold hand hero home iron king lamp leaf lion moon nest note rain ring road rock rose salt sand seed ship snow star stone "
            + "tale tree wand wind wing wolf word worm bread brain chest crown dream earth ghost giant grass heart horse light magic night "
            + "ocean paper plant queen quest river robot sword table tiger tower water witch dragon forest letter master monster planet "
            + "silver spider wizard";

    /** Letter bag weighted roughly by English frequency ('q' becomes a "Qu" tile). */
    static final String LETTER_BAG = "eeeeeeeeeeeeaaaaaaaaaiiiiiiiioooooooonnnnnnrrrrrrttttttllllssssssuuuuddddgggbbccmmppffhhvvwwyykjxqz";

    static double letterValue(char c) {
        return switch (c) {
            case 'd', 'g' -> 1.25;
            case 'b', 'c', 'm', 'p' -> 1.5;
            case 'f', 'h', 'v', 'w', 'y' -> 1.75;
            case 'k' -> 2.0;
            case 'j', 'x' -> 2.75;
            case 'q', 'z' -> 3.0;
            default -> 1.0;
        };
    }

    static boolean isVowel(char c) { return "aeiou".indexOf(c) >= 0; }

    // =====================================================================================
    //  Battle state
    // =====================================================================================

    /** Bonus tiles, made by spelling long words: GREEN heals, GOLD x1.5 damage, CRYSTAL x2 damage. */
    enum Kind { NORMAL, GREEN, GOLD, CRYSTAL }

    static class Tile {
        char ch; Kind kind = Kind.NORMAL; int scratched; boolean selected;
        double drop = 1; int sparkle; final int seed;
        Tile(char ch, int seed) { this.ch = ch; this.seed = seed; }
        String label() { return ch == 'q' ? "Qu" : String.valueOf(Character.toUpperCase(ch)); }
        String letters() { return ch == 'q' ? "qu" : String.valueOf(ch); }
    }

    static final String[] ITEM_NAMES = { "Old Textbook", "Red Pen", "Hall Pass" };

    static class Fighter {
        int hp, max;
        double shown, trail; int trailHold;          // health bar animation
        int shake, lunge, flash, attackPose;         // attackPose = frames left showing the attack sprite
        double enter, fade; boolean dying;

        void reset(int hp) {
            this.hp = hp; shown = trail = hp; trailHold = 0;
            shake = lunge = flash = attackPose = 0;
            enter = 1; fade = 0; dying = false;
        }
    }

    record Step(int duration, Runnable start) {}
    record FlyTile(String label, Kind kind, int seed, double x, double y, double size) {}

    static class FloatText {
        double x, y; final String text; final Color color; int life = 70;
        FloatText(double x, double y, String text, Color color) { this.x = x; this.y = y; this.text = text; this.color = color; }
    }

    // =====================================================================================
    //  The battle screen
    // =====================================================================================

    public static class BattlePanel extends JPanel {
        static final int W = 800, H = 600, TOP_H = 58, STAGE_BOTTOM = 290, FLOOR_TOP = 244;
        static final int TILE = 50, GAP = 4, GRID_X = 296, GRID_Y = 306;
        static final int HERO_X = 150, ENEMY_X = 650, FLOOR_Y = 276;
        static final Rectangle SCRAMBLE_BTN = new Rectangle(40, 540, 190, 46);
        static final Rectangle SHUFFLE_BTN = new Rectangle(235, 540, 56, 46);   // small button between SCRAMBLE and ATTACK
        static final Rectangle ATTACK_BTN = new Rectangle(300, 534, 200, 56);
        static final Rectangle MENU_BTN = new Rectangle(570, 540, 190, 46);
        static final Rectangle KIT_BOX = new Rectangle(18, 304, 250, 64);
        static final Rectangle RESUME_BTN = new Rectangle(300, 262, 200, 44), RESTART_BTN = new Rectangle(300, 318, 200, 44);
        static final Rectangle RETRY_BTN = new Rectangle(300, 306, 200, 46), GIVEUP_BTN = new Rectangle(300, 362, 200, 46);
        static final Rectangle CONTINUE_BTN = new Rectangle(300, 318, 200, 46);

        // colours
        static final Color INK = new Color(34, 26, 20);
        static final Color PALE = new Color(206, 200, 184);
        static final Color BLOOD = new Color(170, 18, 22);
        static final Color CHALK = new Color(214, 220, 208);
        static final String MONO = Font.MONOSPACED, SERIF = Font.SERIF;

        final EnemyDef enemy;
        final int startHp, startKits;
        final int startFoeHp;   // The monster's health when the fight begins (less than its max for a weakened boss)
        final Consumer<BattleResult> onFinish;
        final Set<String> dict = dictionary();
        final Random rng = new Random(), fx = new Random();
        final Tile[] grid = new Tile[16];
        final List<Integer> word = new ArrayList<>();
        final Fighter hero = new Fighter(), foe = new Fighter();
        int kits;
        final Items items;
        int[] startItems; // item counts when the fight began (Try Again puts them back)
        int booksActive, pensActive, passesActive; // items clicked this turn, waiting to take effect

        final ArrayDeque<Step> script = new ArrayDeque<>();
        Step step; int stepTimer;
        String banner = ""; int bannerLife, bannerMax = 1;
        final List<FloatText> floats = new ArrayList<>();
        final List<FlyTile> flying = new ArrayList<>();
        double flyT = -1;
        boolean menuOpen, lost, won, finished;
        final int turnTicks;           // How long the player's turn lasts (see turnSeconds)...
        int turnLeft;                  // ...and how much of it is left
        int enemyTurns;                // How many turns the monster has taken (for the checks)
        Point mouse = new Point(-1, -1);
        long ticks;
        javax.swing.Timer timer;

        // atmosphere
        double light = 1; int flickerTicks;
        final double[][] dust = new double[34][];

        BattlePanel(EnemyDef enemy) { this(enemy, HERO_MAX_HP, START_KITS, new Items(1, 1, 1), null); }

        BattlePanel(EnemyDef enemy, int heroHp, int kits, Items items, Consumer<BattleResult> onFinish) {
            this(enemy, enemy.maxHp(), heroHp, kits, items, onFinish);
        }

        BattlePanel(EnemyDef enemy, int foeHp, int heroHp, int kits, Items items, Consumer<BattleResult> onFinish) {
            this.enemy = enemy;
            this.startFoeHp = Math.max(1, Math.min(enemy.maxHp(), foeHp));
            this.turnTicks = turnSeconds(enemy) * TICKS_PER_SECOND;
            this.startHp = Math.max(1, Math.min(HERO_MAX_HP, heroHp));
            this.startKits = Math.max(0, kits);
            this.onFinish = onFinish;
            this.items = items != null ? items : new Items();
            startItems = new int[] { this.items.textbooks, this.items.redPens, this.items.hallPasses };
            setPreferredSize(new Dimension(W, H));
            setBackground(Color.BLACK);
            setFocusable(true);
            for (int i = 0; i < dust.length; i++)
                dust[i] = new double[] { fx.nextInt(W), TOP_H + fx.nextInt(STAGE_BOTTOM - TOP_H), 1.5 + fx.nextDouble() * 2, 0.05 + fx.nextDouble() * 0.15, fx.nextDouble() * 6 };
            installInput();
            resetFight();
        }

        /** Starts the animation timer. Call once after adding the panel to a window. */
        void start() {
            if (timer == null) timer = new javax.swing.Timer(16, e -> { tick(); repaint(); });
            timer.start();
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        }

        void resetFight() {
            script.clear(); step = null; flying.clear(); flyT = -1; floats.clear(); bannerLife = 0;
            menuOpen = lost = won = false;
            hero.max = HERO_MAX_HP; hero.reset(startHp);
            foe.max = enemy.maxHp(); foe.reset(startFoeHp);
            kits = startKits;
            if (startItems != null) { items.textbooks = startItems[0]; items.redPens = startItems[1]; items.hallPasses = startItems[2]; }
            booksActive = pensActive = passesActive = 0;
            turnLeft = turnTicks;
            word.clear();
            for (int i = 0; i < 16; i++) newTile(i);
            fixVowels(allIndices());
            then(0, () -> showBanner("The " + enemy.name() + " won't let you leave.", 110));
            if (usingFallbackWords) then(0, () -> showBanner("words.txt not found - using a small word list", 160));
            then(50, null);
        }

        void finish() {
            if (finished) return;
            finished = true;
            if (timer != null) timer.stop();
            if (onFinish != null) onFinish.accept(new BattleResult(won, won ? hero.hp : 0, kits));
        }

        // ---------------------------------------------------------------- script / timing

        void then(int duration, Runnable start) { script.add(new Step(duration, start)); }
        boolean busy() { return step != null || !script.isEmpty(); }
        boolean canAct() { return !busy() && !menuOpen && !lost && !won; }
        void showBanner(String text, int life) { banner = text; bannerLife = life; bannerMax = life; }
        void addFloat(double x, double y, String text, Color c) { floats.add(new FloatText(x, y, text, c)); }

        void hurt(Fighter f, int dmg) {
            f.hp = Math.max(0, f.hp - dmg);
            f.shake = 26; f.flash = 14; f.trailHold = 30;
        }

        void heal(Fighter f, int amount, double x, double y) {
            int h = Math.min(amount, f.max - f.hp);
            if (h <= 0) return;
            f.hp += h;
            addFloat(x, y, "+" + h, new Color(130, 210, 120));
        }

        void tick() {
            ticks++;
            if (menuOpen) return;
            if (step != null && --stepTimer <= 0) step = null;
            while (step == null && !script.isEmpty()) {
                step = script.poll();
                stepTimer = step.duration();
                if (step.start() != null) step.start().run();
                if (stepTimer <= 0) step = null;
            }
            if (TURN_TIMER && canAct() && --turnLeft <= 0) timeUp();   // The player's clock runs on their turn
            for (Fighter f : new Fighter[] { hero, foe }) {
                if (f.shake > 0) f.shake--;
                if (f.lunge > 0) f.lunge--;
                if (f.flash > 0) f.flash--;
                if (f.attackPose > 0) f.attackPose--;
                f.enter = Math.max(0, f.enter - 1 / 40.0);
                if (f.dying) f.fade = Math.min(1, f.fade + 1 / 50.0);
                double rate = Math.max(0.3, f.max / 110.0);
                f.shown = f.shown > f.hp ? Math.max(f.hp, f.shown - rate * 2) : Math.min(f.hp, f.shown + rate);
                if (f.trail < f.shown) f.trail = f.shown;
                else if (f.trailHold > 0) f.trailHold--;
                else f.trail = Math.max(f.shown, f.trail - rate * 0.7);
            }
            for (Tile t : grid) { t.drop = Math.max(0, t.drop - 0.07); if (t.sparkle > 0) t.sparkle--; }
            if (flyT >= 0 && flyT < 1) flyT = Math.min(1, flyT + 1 / 30.0);
            if (bannerLife > 0) bannerLife--;
            for (Iterator<FloatText> it = floats.iterator(); it.hasNext(); ) {
                FloatText f = it.next();
                f.y -= 0.7;
                if (--f.life <= 0) it.remove();
            }
            // ceiling light flicker: dims gently every few frames, then comes back
            if (!LIGHT_FLICKER) light = 1;
            else if (flickerTicks > 0) {
                flickerTicks--;
                if (flickerTicks % 5 == 0) light = fx.nextBoolean() ? 0.45 + fx.nextDouble() * 0.3 : 1;
                if (flickerTicks == 0) light = 1;
            } else if (fx.nextInt(480) == 0) flickerTicks = 20 + fx.nextInt(40);
            for (double[] d : dust) {
                d[0] += Math.sin(ticks / 70.0 + d[4]) * 0.25;
                d[1] += d[3];
                if (d[1] > STAGE_BOTTOM) { d[1] = TOP_H; d[0] = fx.nextInt(W); }
            }
        }

        // ---------------------------------------------------------------- letter grid

        List<Integer> allIndices() { List<Integer> l = new ArrayList<>(); for (int i = 0; i < 16; i++) l.add(i); return l; }

        void newTile(int i) {
            char c = LETTER_BAG.charAt(rng.nextInt(LETTER_BAG.length()));
            // keep hard letters rare: at most one of q/z/x/j on the board
            if ("qzxj".indexOf(c) >= 0) for (Tile t : grid) if (t != null && t != grid[i] && "qzxj".indexOf(t.ch) >= 0) { c = "etaoinsr".charAt(rng.nextInt(8)); break; }
            grid[i] = new Tile(c, fx.nextInt());
        }

        /** Make sure the board has at least 5 vowels, changing only the given (new) tiles. */
        void fixVowels(List<Integer> candidates) {
            List<Integer> c = new ArrayList<>(candidates);
            Collections.shuffle(c, rng);
            int vowels = 0;
            for (Tile t : grid) if (isVowel(t.ch)) vowels++;
            for (int i : c) {
                if (vowels >= 5) break;
                Tile t = grid[i];
                if (!isVowel(t.ch) && t.kind == Kind.NORMAL && t.scratched == 0) { t.ch = "aaeeeiioou".charAt(rng.nextInt(10)); vowels++; }
            }
        }

        Rectangle tileRect(int i) { return new Rectangle(GRID_X + (i % 4) * (TILE + GAP), GRID_Y + (i / 4) * (TILE + GAP), TILE, TILE); }

        double wordTileSize() { return Math.min(46, 560.0 / Math.max(1, word.size()) - 4); }

        Rectangle2D wordTileRect(int pos) {
            double s = wordTileSize(), sp = s + 4, total = word.size() * sp - 4;
            return new Rectangle2D.Double(400 - total / 2 + pos * sp, 92, s, s);
        }

        void select(int i) {
            Tile t = grid[i];
            if (t.selected || t.scratched > 0 || word.size() >= 16) return;
            t.selected = true;
            word.add(i);
        }

        void unselect(int pos) { grid[word.remove(pos)].selected = false; }
        void clearWord() { for (int i : word) grid[i].selected = false; word.clear(); }

        String currentWord() {
            StringBuilder sb = new StringBuilder();
            for (int i : word) sb.append(grid[i].letters());
            return sb.toString();
        }

        boolean wordValid() { String w = currentWord(); return w.length() >= 3 && dict.contains(w); }

        /** Damage of the current word, in HP. */
        int damage() {
            double sum = 0; boolean gold = false, crystal = false;
            for (int i : word) {
                sum += letterValue(grid[i].ch);
                gold |= grid[i].kind == Kind.GOLD;
                crystal |= grid[i].kind == Kind.CRYSTAL;
            }
            int len = currentWord().length();
            double mult = len <= 3 ? 1.0 : len == 4 ? 1.25 : len == 5 ? 1.6 : len == 6 ? 2.0 : len == 7 ? 2.5 : len == 8 ? 3.0 : 3.5;
            double d = sum * mult * DAMAGE_PER_LETTER;
            if (crystal) d *= 2; else if (gold) d *= 1.5;
            d *= 1 + boostNow(); // Red Pens used this turn
            return Math.max(1, (int) Math.round(d));
        }

        // ---------------------------------------------------------------- player actions

        void attack() {
            if (!canAct() || !wordValid()) return;
            int dmg = damage();
            pensActive = 0; // the Red Pen boost is spent on this attack
            int greens = 0;
            for (int i : word) if (grid[i].kind == Kind.GREEN) greens++;
            int len = currentWord().length();
            flying.clear();
            for (int p = 0; p < word.size(); p++) {
                Rectangle2D r = wordTileRect(p);
                Tile t = grid[word.get(p)];
                flying.add(new FlyTile(t.label(), t.kind, t.seed, r.getX(), r.getY(), r.getWidth()));
            }
            List<Integer> used = new ArrayList<>(word);
            word.clear();
            hero.lunge = 18;
            hero.attackPose = 40;
            flyT = 0;
            int healAmount = greens * HEAL_TILE_HP;
            then(32, null);
            then(0, () -> impact(used, len, dmg, healAmount));
        }

        void impact(List<Integer> used, int len, int dmg, int healAmount) {
            flying.clear(); flyT = -1;
            hurt(foe, dmg);
            addFloat(ENEMY_X - 20, 150, "-" + dmg, new Color(235, 50, 40));
            if (healAmount > 0) heal(hero, healAmount, HERO_X - 20, 140);
            for (int i : used) newTile(i);
            int bonus = used.get(rng.nextInt(used.size()));
            Kind k = len >= 7 ? Kind.CRYSTAL : len == 6 ? Kind.GOLD : len == 5 ? Kind.GREEN : Kind.NORMAL;
            if (k != Kind.NORMAL) { grid[bonus].kind = k; grid[bonus].sparkle = 60; }
            fixVowels(used);
            if (foe.hp <= 0) enemyDefeated(); else enemyTurn();
        }

        /** Shuffle */
        void shuffle() {
            if (!canAct()) return;
            List<Integer> spots = new ArrayList<>();
            for (int i = 0; i < 16; i++) if (!grid[i].selected) spots.add(i);   // letters already in your word stay in it
            if (spots.size() < 2) return;
            List<Tile> tiles = new ArrayList<>();
            for (int i : spots) tiles.add(grid[i]);
            List<Tile> before = new ArrayList<>(tiles);
            for (int tries = 0; tries < 5 && tiles.equals(before); tries++) Collections.shuffle(tiles, fx);
            for (int k = 0; k < spots.size(); k++) {
                grid[spots.get(k)] = tiles.get(k);
                tiles.get(k).drop = 0.5;   // little drop-in animation
            }
        }

        void scramble() {
            if (!canAct()) return;
            clearWord();
            List<Integer> changed = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                if (grid[i].scratched > 0) continue;
                Kind k = grid[i].kind;
                newTile(i);
                grid[i].kind = k;
                changed.add(i);
            }
            fixVowels(changed);
            showBanner("Your letters shift and rearrange...", 60);
            enemyTurn();
        }

        // The player's time ran out: the word they were spelling is dropped, and the monster attacks
        void timeUp() {
            clearWord();
            showBanner("Too slow! The " + enemy.name() + " doesn't wait.", 60);
            addFloat(HERO_X - 45, 120, "Time's up!", new Color(235, 170, 60));
            enemyTurn();
        }

        void useKit() {
            if (!canAct()) return;
            if (kits == 0) { showBanner("No first aid kits left.", 70); return; }
            if (hero.hp == hero.max) { showBanner("You're not hurt.", 70); return; }
            kits--;
            heal(hero, KIT_HEAL, HERO_X - 20, 140);
            showBanner(HERO_NAME + " patches up with a first aid kit.", 70);
        }

        // ---------------------------------------------------------------- enemy

        Attack pickAttack() {
            int total = enemy.attacks().stream().mapToInt(Attack::weight).sum(), roll = rng.nextInt(Math.max(1, total));
            for (Attack a : enemy.attacks()) { roll -= a.weight(); if (roll < 0) return a; }
            return enemy.attacks().get(0);
        }

        void enemyTurn() {
            enemyTurns++;
            Attack a = pickAttack();
            then(28, null);
            then(18, () -> {
                for (Tile t : grid) if (t.scratched > 0) t.scratched--;
                showBanner("The " + enemy.name() + " uses " + a.name() + "!", 80);
                foe.lunge = 18;
                foe.attackPose = 40;
            });
            then(40, () -> enemyHits(a));
            then(0, () -> { if (hero.hp <= 0) heroDefeated(); else turnLeft = turnTicks; });   // The player's turn again: a fresh clock
        }

        int enemyDamage(Attack a) { return a.damage() <= 0 ? 0 : Math.max(1, (int) Math.round(a.damage() * (1 - blockNow()))); }

        void enemyHits(Attack a) {
            int dodge = dodgeNow();            // Hall Passes used this turn
            int dmg = enemyDamage(a);          // already reduced by Textbooks used this turn
            int blocked = booksActive > 0 ? Math.max(0, a.damage() - dmg) : 0;
            booksActive = passesActive = 0;    // they're spent on this attack
            if (rng.nextInt(100) < dodge) {
                addFloat(HERO_X - 50, 140, "Dodged!", PALE);
                return;
            }
            if (dmg > 0) {
                hurt(hero, dmg);
                addFloat(HERO_X - 20, 140, "-" + dmg, new Color(235, 50, 40));
                if (blocked > 0) addFloat(HERO_X - 80, 110, "Textbook blocked " + blocked, PALE);
            }
            if (a.scratch() > 0) {
                List<Integer> c = new ArrayList<>();
                for (int i = 0; i < 16; i++) if (grid[i].scratched == 0) c.add(i);
                Collections.shuffle(c, rng);
                for (int j = 0; j < Math.min(a.scratch(), c.size()); j++) grid[c.get(j)].scratched = 2;
            }
            if (a.heal() > 0) heal(foe, a.heal(), ENEMY_X - 20, 150);
        }

        void enemyDefeated() {
            then(20, () -> foe.dying = true);
            then(60, () -> showBanner("The " + enemy.name() + " collapses into the dark...", 120));
            then(60, null);
            then(0, () -> won = true);
        }

        void heroDefeated() {
            hero.dying = true;
            showBanner("Everything goes dark...", 120);
            then(80, null);
            then(0, () -> lost = true);
        }

        // ---------------------------------------------------------------- input

        void installInput() {
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    int code = e.getKeyCode();
                    if (menuOpen) { if (code == KeyEvent.VK_ESCAPE) menuOpen = false; return; }
                    if (won) { if (code == KeyEvent.VK_ENTER) finish(); return; }
                    if (lost) { if (code == KeyEvent.VK_ENTER) resetFight(); return; }
                    if (code == KeyEvent.VK_ESCAPE) { if (!word.isEmpty()) clearWord(); else menuOpen = true; return; }
                    if (!canAct()) return;
                    if (code == KeyEvent.VK_ENTER) attack();
                    else if (code == KeyEvent.VK_SPACE) shuffle();   // Space bar = shuffle
                    else if (code == KeyEvent.VK_BACK_SPACE) { if (!word.isEmpty()) unselect(word.size() - 1); }
                    else {
                        char c = Character.toLowerCase(e.getKeyChar());
                        if (c < 'a' || c > 'z') return;
                        if (c == 'u' && !word.isEmpty() && grid[word.get(word.size() - 1)].ch == 'q') return; // "Qu" already has the u
                        for (int i = 0; i < 16; i++) if (grid[i].ch == c && !grid[i].selected && grid[i].scratched == 0) { select(i); return; }
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) { mouse = e.getPoint(); }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) { mouse = new Point(-1, -1); }
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    Point p = e.getPoint();
                    if (won) { if (CONTINUE_BTN.contains(p)) finish(); return; }
                    if (lost) { if (RETRY_BTN.contains(p)) resetFight(); else if (GIVEUP_BTN.contains(p)) finish(); return; }
                    if (menuOpen) { if (RESUME_BTN.contains(p)) menuOpen = false; else if (RESTART_BTN.contains(p)) resetFight(); return; }
                    if (MENU_BTN.contains(p)) { menuOpen = true; return; }
                    if (!canAct()) return;
                    for (int i = 0; i < 3; i++) if (itemRect(i).contains(p)) { useItem(i); return; }
                    for (int i = 0; i < 16; i++) if (tileRect(i).contains(p)) {
                        if (grid[i].selected) unselect(word.indexOf(i)); else select(i);
                        return;
                    }
                    for (int pos = 0; pos < word.size(); pos++) if (wordTileRect(pos).contains(p)) { unselect(pos); return; }
                    if (ATTACK_BTN.contains(p)) attack();
                    else if (SCRAMBLE_BTN.contains(p)) scramble();
                    else if (SHUFFLE_BTN.contains(p)) shuffle();
                    else if (KIT_BOX.contains(p)) useKit();
                }
            });
        }

        // ---------------------------------------------------------------- rendering

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            drawRoom(g);
            drawFighter(g, hero, true);
            drawFighter(g, foe, false);
            drawAtmosphere(g);
            drawTurnTimer(g);
            drawTopBar(g);
            drawWord(g);
            drawBanner(g);
            drawFloats(g);
            drawBottom(g);
            drawGrid(g);
            drawButtons(g);
            drawTooltip(g);
            if (won) drawWon(g);
            if (lost) drawLost(g);
            if (menuOpen) drawMenu(g);
            g.dispose();
        }

        static Ellipse2D el(double x, double y, double w, double h) { return new Ellipse2D.Double(x, y, w, h); }

        /** The room behind the fight: drawn by the monster's Backdrop (ClassroomBackdrop, LabBackdrop...). */
        void drawRoom(Graphics2D g) {
            Shape oldClip = g.getClip();
            g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
            enemy.backdrop().draw(g, this);
            g.setClip(oldClip);
        }

        static double px(double vpx, double vpy, double xBottom, double y) { return vpx + (xBottom - vpx) * (y - vpy) / (STAGE_BOTTOM - vpy); }

        static void chalk(Graphics2D g, String text, double x, double y, double deg, int size, int alpha) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(Math.toRadians(deg), x, y);
            g2.setFont(new Font(MONO, Font.BOLD, size));
            g2.setColor(new Color(214, 220, 208, alpha));
            g2.drawString(text, (float) x, (float) y);
            g2.dispose();
        }

        /** Light, darkness, vignette, dust, the flickering tube, and a red pulse when the player is nearly dead. */
        void drawAtmosphere(Graphics2D g) {
            Shape oldClip = g.getClip();
            g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(400, 70), 320f, new float[] { 0f, 1f },
                    new Color[] { new Color(200, 235, 205, (int) (44 * light)), new Color(200, 235, 205, 0) }));
            g.fillRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
            int dark = (int) ((1 - light) * 150);
            if (dark > 0) { g.setColor(new Color(0, 0, 0, dark)); g.fillRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H); }
            g.setPaint(new RadialGradientPaint(new Point2D.Double(400, 180), 470f, new float[] { 0.4f, 1f },
                    new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 215) }));
            g.fillRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
            for (double[] d : dust) {
                g.setColor(new Color(200, 210, 190, (int) (35 + 30 * Math.sin(ticks / 30.0 + d[4]))));
                g.fill(el(d[0], d[1], d[2], d[2]));
            }
            // fluorescent light fixture
            g.setColor(new Color(30, 30, 28));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(352, TOP_H, 352, 64); g.drawLine(448, TOP_H, 448, 64);
            g.setColor(new Color(42, 44, 42));
            g.fillRect(330, 63, 140, 7);
            int tube = (int) (90 + 150 * light);
            g.setColor(new Color(Math.min(255, tube - 10), Math.min(255, tube + 5), Math.min(255, tube - 10)));
            g.fillRect(336, 69, 128, 3);
            if (hero.hp > 0 && hero.hp <= hero.max * 0.3) {
                double beat = Math.pow(Math.max(0, Math.sin(ticks / 9.0)), 4);
                g.setPaint(new RadialGradientPaint(new Point2D.Double(400, 175), 470f, new float[] { 0.45f, 1f },
                        new Color[] { new Color(140, 0, 0, 0), new Color(140, 0, 0, (int) (40 + 110 * beat)) }));
                g.fillRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
            }
            g.setClip(oldClip);
        }

        /** The turn timer, on the floor between the fighters just above the letters: a fuse that burns
         *  down, green, then amber, then red and flashing for the last 5 seconds. Only on the player's turn. */
        void drawTurnTimer(Graphics2D g) {
            if (!TURN_TIMER || won || lost || busy()) return;
            double left = Math.max(0, (double) turnLeft / turnTicks);
            int secs = (turnLeft + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
            boolean hurry = secs <= 5;
            int bw = 250, bx = 400 - bw / 2 + 10, by = STAGE_BOTTOM - 22, bh = 10;
            g.setColor(new Color(0, 0, 0, 175));
            g.fillRoundRect(bx - 34, by - 9, bw + 86, 28, 12, 12);
            // A little clock face
            double cx = bx - 18, cy = by + 5;
            g.setColor(hurry ? new Color(220, 60, 50) : PALE);
            g.setStroke(new BasicStroke(2f));
            g.draw(el(cx - 8, cy - 8, 16, 16));
            double a = -Math.PI / 2 + 2 * Math.PI * (1 - left);
            g.draw(new Line2D.Double(cx, cy, cx + Math.cos(a) * 6, cy + Math.sin(a) * 6));
            g.setStroke(new BasicStroke(1f));
            // The fuse
            g.setColor(new Color(40, 34, 30));
            g.fillRect(bx, by, bw, bh);
            Color c = left > 0.5 ? new Color(110, 190, 90) : left > 0.25 ? new Color(226, 170, 60) : new Color(216, 52, 42);
            if (hurry && (ticks / 8) % 2 == 0) c = new Color(255, 120, 100);
            int w = (int) Math.round(bw * left);
            g.setColor(c);
            g.fillRect(bx, by, w, bh);
            g.setColor(new Color(255, 255, 255, 45));
            g.fillRect(bx, by, w, bh / 3);
            if (w > 0) {                                     // The spark at the burning end
                g.setColor(new Color(255, 230, 150, 120 + (int) (100 * Math.abs(Math.sin(ticks / 4.0)))));
                g.fill(el(bx + w - 4, by - 2, 8, bh + 4));
            }
            g.setFont(new Font(MONO, Font.BOLD, hurry ? 17 : 15));
            shadowText(g, secs + "s", bx + bw + 10, by + 10, hurry ? new Color(255, 110, 90) : PALE);
        }

        void drawFighter(Graphics2D g, Fighter f, boolean isHero) {
            if (f.fade >= 1) return;
            double x = isHero ? HERO_X : ENEMY_X, y = FLOOR_Y;
            double e = f.enter * f.enter * (3 - 2 * f.enter);
            double dx = isHero ? -e * 320 : e * 380;
            if (f.lunge > 0) { double t = Math.sin(Math.PI * f.lunge / 18.0) * 40; dx += isHero ? t : -t; }
            if (f.shake > 0) dx += (f.shake / 2) % 2 == 0 ? 6 : -6;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive((float) (1 - f.fade)));
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fill(el(x + dx - 55, y - 8, 110, 16));
            boolean blink = f.flash > 0 && (f.flash / 3) % 2 == 1;
            if (!blink) {
                // Pick the pose: name_attack.png while attacking, name_hurt.png while hit, otherwise name.png.
                String key = isHero ? HERO_SPRITE : enemy.sprite();
                BufferedImage img = null;
                if (f.attackPose > 0) img = Sprites.get(key + "_attack", false);
                else if (f.shake > 0) img = Sprites.get(key + "_hurt", false);
                if (img == null) img = Sprites.get(key, true);
                if (img != null) Sprites.drawFit(g2, img, x + dx, y, isHero ? 150 : 170);
                else { // sprite missing: dark box with a "?"
                    g2.setColor(new Color(50, 50, 52));
                    g2.fillRect((int) (x + dx) - 50, (int) y - 120, 100, 120);
                    g2.setColor(new Color(150, 20, 20));
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 60));
                    g2.drawString("?", (int) (x + dx) - 17, (int) y - 38);
                }
            }
            g2.dispose();
        }

        void drawTopBar(Graphics2D g) {
            g.setPaint(new GradientPaint(0, 0, new Color(26, 26, 24), 0, TOP_H, new Color(10, 10, 10)));
            g.fillRect(0, 0, W, TOP_H);
            g.setColor(new Color(70, 18, 18));
            g.fillRect(0, TOP_H - 2, W, 2);
            g.setColor(new Color(70, 68, 64));
            for (int x : new int[] { 6, 246, 554, 790 }) { g.fill(el(x, 8, 5, 5)); g.fill(el(x, 44, 5, 5)); }

            g.setFont(new Font(MONO, Font.BOLD, 16));
            shadowText(g, HERO_NAME.toUpperCase(), 18, 22, PALE);
            healthBar(g, 18, 30, 222, 18, hero, true);

            String name = enemy.name().toUpperCase();
            g.setFont(new Font(MONO, Font.BOLD, 16));
            shadowText(g, name, 782 - g.getFontMetrics().stringWidth(name), 22, PALE);
            healthBar(g, 560, 30, 222, 18, foe, false);

            // the door sign of the room the fight is in
            if (!enemy.room().isBlank()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.rotate(Math.toRadians(-2.5), 400, 29);
                g2.setPaint(new GradientPaint(0, 9, new Color(150, 128, 82), 0, 49, new Color(92, 76, 44)));
                g2.fillRoundRect(306, 9, 188, 40, 6, 6);
                g2.setColor(new Color(40, 32, 18));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(306, 9, 188, 40, 6, 6);
                g2.setColor(new Color(60, 48, 26));
                g2.fill(el(312, 26, 5, 5)); g2.fill(el(483, 26, 5, 5));
                g2.setFont(new Font(SERIF, Font.BOLD, 21));
                String room = enemy.room();
                g2.setColor(new Color(190, 170, 120, 120));
                g2.drawString(room, 400 - g2.getFontMetrics().stringWidth(room) / 2 + 1, 37);
                g2.setColor(new Color(34, 26, 14));
                g2.drawString(room, 400 - g2.getFontMetrics().stringWidth(room) / 2, 36);
                g2.setColor(new Color(40, 30, 16, 150));
                g2.setStroke(new BasicStroke(1));
                g2.drawLine(452, 9, 446, 24); g2.drawLine(446, 24, 452, 32);
                g2.dispose();
            }
        }

        /** Health bar: the red part drops quickly, the pale "trail" behind it shows the damage just taken. */
        void healthBar(Graphics2D g, int x, int y, int w, int h, Fighter f, boolean isHero) {
            boolean low = isHero && f.hp > 0 && f.hp <= f.max * 0.3;
            g.setColor(new Color(6, 4, 4));
            g.fillRect(x - 3, y - 3, w + 6, h + 6);
            double beat = low ? Math.pow(Math.max(0, Math.sin(ticks / 9.0)), 4) : 0;
            g.setColor(low ? new Color(110 + (int) (120 * beat), 20, 20) : new Color(84, 80, 74));
            g.setStroke(new BasicStroke(2));
            g.drawRect(x - 3, y - 3, w + 6, h + 6);
            g.setColor(new Color(30, 10, 12));
            g.fillRect(x, y, w, h);
            int tw = (int) Math.round(w * Math.max(0, f.trail) / f.max), sw = (int) Math.round(w * Math.max(0, f.shown) / f.max);
            g.setColor(new Color(214, 184, 150));
            g.fillRect(x, y, tw, h);
            Color top = isHero ? new Color(214, 40, 38) : new Color(150, 36, 110);
            Color bottom = isHero ? new Color(112, 8, 12) : new Color(66, 8, 52);
            g.setPaint(new GradientPaint(0, y, top, 0, y + h, bottom));
            g.fillRect(x, y, sw, h);
            g.setColor(new Color(255, 255, 255, 40));
            g.fillRect(x, y + 1, sw, 3);
            g.setColor(new Color(0, 0, 0, 70)); // scratches
            g.setStroke(new BasicStroke(1));
            for (int i = 0; i < 6; i++) { int sx = x + 18 + i * 37 + (i * 13) % 11; g.drawLine(sx, y, sx - 6, y + h); }
            g.setFont(new Font(MONO, Font.BOLD, 12));
            String s = (int) Math.ceil(f.shown) + " / " + f.max;
            int tx = x + w / 2 - g.getFontMetrics().stringWidth(s) / 2, ty = y + h - 4;
            g.setColor(new Color(0, 0, 0, 220)); // dark outline so the number reads over any colour
            for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++) if (ox != 0 || oy != 0) g.drawString(s, tx + ox, ty + oy);
            g.setColor(Color.WHITE);
            g.drawString(s, tx, ty);
        }

        void drawWord(Graphics2D g) {
            if (flyT >= 0) {
                double t = flyT * flyT;
                for (FlyTile f : flying) {
                    double s = f.size() * (1 - 0.6 * t);
                    double px = f.x() + (ENEMY_X - s / 2 - f.x()) * t;
                    double py = f.y() + (150 - f.y()) * t - Math.sin(Math.PI * flyT) * 50;
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, 1 - Math.max(0, (flyT - 0.75) / 0.25))));
                    drawTile(g2, px, py, s, f.label(), f.kind(), 0, false, 0, f.seed());
                    g2.dispose();
                }
                return;
            }
            if (word.isEmpty()) {
                if (canAct()) {
                    g.setFont(new Font(MONO, Font.BOLD, 14));
                    g.setColor(new Color(214, 220, 208, 120));
                    String hint = "click letters or type to spell a word";
                    g.drawString(hint, 400 - g.getFontMetrics().stringWidth(hint) / 2, 128);
                }
                return;
            }
            for (int p = 0; p < word.size(); p++) {
                Rectangle2D r = wordTileRect(p);
                Tile t = grid[word.get(p)];
                drawTile(g, r.getX(), r.getY(), r.getWidth(), t.label(), t.kind, 0, r.contains(mouse), 0, t.seed);
            }
            if (currentWord().length() < 3) return;
            g.setFont(new Font(MONO, Font.BOLD, 15));
            String s = wordValid() ? "DAMAGE: " + damage() : "not a word";
            shadowText(g, s, 400 - g.getFontMetrics().stringWidth(s) / 2, 160, wordValid() ? CHALK : new Color(200, 100, 90));
        }

        void drawBanner(Graphics2D g) {
            if (bannerLife <= 0 || banner.isEmpty()) return;
            float a = (float) Math.min(1, Math.min(bannerLife / 15.0, (bannerMax - bannerLife + 1) / 8.0));
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(a));
            if (bannerMax - bannerLife < 12) g2.translate(fx.nextInt(5) - 2, fx.nextInt(3) - 1); // jolt when it appears
            g2.setFont(new Font(MONO, Font.BOLD, 17));
            int w = g2.getFontMetrics().stringWidth(banner) + 40;
            Rectangle2D box = new Rectangle2D.Double(400 - w / 2.0, 192, w, 36);
            g2.setColor(new Color(8, 6, 6, 225));
            g2.fill(box);
            g2.setColor(new Color(130, 18, 18));
            g2.setStroke(new BasicStroke(2));
            g2.draw(box);
            shadowText(g2, banner, 400 - (w - 40) / 2, 216, PALE);
            g2.dispose();
        }

        void drawFloats(Graphics2D g) {
            g.setFont(new Font(MONO, Font.BOLD, 28));
            for (FloatText f : floats) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, f.life / 20.0)));
                shadowText(g2, f.text, (int) f.x, (int) f.y, f.color);
                g2.dispose();
            }
        }

        void drawBottom(Graphics2D g) {
            // grimy tiled wall
            g.setColor(new Color(24, 28, 26));
            g.fillRect(0, STAGE_BOTTOM, W, H - STAGE_BOTTOM);
            Random r = new Random(13);
            for (int y = STAGE_BOTTOM; y < H; y += 24) for (int x = 0; x < W; x += 24) {
                if (r.nextInt(7) == 0) { g.setColor(new Color(16, 18, 17)); g.fillRect(x, y, 24, 24); }
            }
            g.setColor(new Color(12, 14, 13));
            for (int y = STAGE_BOTTOM; y < H; y += 24) g.drawLine(0, y, W, y);
            for (int x = 0; x < W; x += 24) g.drawLine(x, STAGE_BOTTOM, x, H);
            g.setPaint(new GradientPaint(0, STAGE_BOTTOM, new Color(0, 0, 0, 40), 0, H, new Color(20, 8, 4, 150)));
            g.fillRect(0, STAGE_BOTTOM, W, H - STAGE_BOTTOM);
            g.setColor(new Color(64, 40, 28));
            g.fillRect(0, STAGE_BOTTOM, W, 4);

            // left: first aid kits + locker items
            metalPanel(g, 8, 296, 270, 232);
            boolean kitHover = KIT_BOX.contains(mouse) && canAct() && kits > 0;
            g.setColor(kitHover ? new Color(110, 24, 24, 120) : new Color(0, 0, 0, 110));
            g.fillRect(KIT_BOX.x, KIT_BOX.y, KIT_BOX.width, KIT_BOX.height);
            Art.firstAid(g, 50, 336, kits > 0);
            g.setFont(new Font(MONO, Font.BOLD, 20));
            shadowText(g, "x" + kits, 80, 344, PALE);
            g.setFont(new Font(MONO, Font.BOLD, 13));
            shadowText(g, "First Aid Kit", 128, 331, new Color(214, 70, 60));
            g.setFont(new Font(MONO, Font.PLAIN, 11));
            shadowText(g, "click to heal " + KIT_HEAL + " HP", 128, 349, new Color(180, 174, 160));
            for (int i = 0; i < 3; i++) {
                Rectangle cub = itemRect(i);
                boolean hover = cub.contains(mouse);
                g.setColor(new Color(10, 12, 12));
                g.fillRect(cub.x, cub.y, cub.width, cub.height);
                g.setColor(hover ? new Color(150, 30, 30) : new Color(70, 72, 70));
                g.setStroke(new BasicStroke(2));
                g.drawRect(cub.x, cub.y, cub.width, cub.height);
                String active = i == 0 && booksActive > 0 ? "-" + pct(blockNow()) + "%"
                              : i == 1 && pensActive > 0 ? "+" + pct(boostNow()) + "%"
                              : i == 2 && passesActive > 0 ? dodgeNow() + "%" : null;
                if (active != null) { // used this turn: glowing outline and its current strength
                    g.setColor(new Color(230, 40, 40, (int) (150 + 100 * Math.sin(ticks / 6.0))));
                    g.setStroke(new BasicStroke(3));
                    g.drawRect(cub.x - 2, cub.y - 2, cub.width + 4, cub.height + 4);
                    g.setFont(new Font(MONO, Font.BOLD, 13));
                    shadowText(g, active, cub.x + 5, cub.y + 24, new Color(240, 70, 60));
                }
                g.setColor(new Color(50, 52, 50));
                for (int v = 0; v < 4; v++) g.fillRect(cub.x + 16, cub.y + 8 + v * 5, cub.width - 32, 2); // locker vents
                int count = itemCount(i);
                Graphics2D gi = (Graphics2D) g.create();
                if (count == 0) gi.setComposite(AlphaComposite.SrcOver.derive(0.22f)); // not found yet: faded
                Art.item(gi, i, cub.x + cub.width / 2.0, cub.y + 62);
                gi.dispose();
                if (count > 0) {
                    g.setFont(new Font(MONO, Font.BOLD, 14));
                    String n = "x" + count;
                    shadowText(g, n, cub.x + cub.width - 6 - g.getFontMetrics().stringWidth(n), cub.y + 24, PALE);
                }
                g.setFont(new Font(MONO, Font.BOLD, 12));
                String[] parts = ITEM_NAMES[i].split(" ");
                for (int l = 0; l < parts.length; l++)
                    shadowText(g, parts[l], cub.x + cub.width / 2 - g.getFontMetrics().stringWidth(parts[l]) / 2, cub.y + 108 + l * 14,
                            count > 0 ? PALE : new Color(96, 94, 90));
            }

            // centre: chalkboard frame for the letters
            g.setColor(new Color(52, 38, 28));
            g.fillRect(284, 296, 232, 232);
            g.setColor(new Color(20, 14, 10));
            g.setStroke(new BasicStroke(2));
            g.drawRect(284, 296, 232, 232);
            g.setPaint(new GradientPaint(0, 302, new Color(28, 40, 35), 0, 522, new Color(17, 25, 22)));
            g.fillRect(290, 302, 220, 220);

            // right: incident report on lined school paper
            metalPanel(g, 522, 296, 270, 232);
            g.setPaint(new GradientPaint(0, 306, new Color(200, 188, 152), 0, 518, new Color(160, 146, 110)));
            g.fillRect(532, 306, 250, 212);
            g.setColor(new Color(90, 110, 140, 60));
            g.setStroke(new BasicStroke(1));
            for (int y = 338; y < 518; y += 16) g.drawLine(532, y, 782, y);
            g.setColor(new Color(170, 40, 40, 70));
            g.drawLine(556, 306, 556, 518);
            Random sr = new Random(enemy.name().hashCode());
            for (int i = 0; i < 4; i++) { // stains
                g.setColor(new Color(90, 60, 30, 30 + sr.nextInt(30)));
                g.fill(el(540 + sr.nextInt(210), 320 + sr.nextInt(170), 20 + sr.nextInt(30), 14 + sr.nextInt(20)));
            }
            g.setColor(new Color(150, 150, 156));
            g.setStroke(new BasicStroke(2.2f));
            g.draw(new RoundRectangle2D.Double(546, 300, 12, 30, 10, 10));
            g.draw(new RoundRectangle2D.Double(549, 304, 7, 22, 7, 7));
            g.setFont(new Font(MONO, Font.BOLD, 13));
            g.setColor(INK);
            g.drawString("INCIDENT REPORT", 566, 326);
            Graphics2D st = (Graphics2D) g.create();
            st.rotate(Math.toRadians(-8), 740, 322);
            st.setColor(new Color(160, 20, 20, 170));
            st.setStroke(new BasicStroke(2));
            st.drawRect(712, 310, 58, 18);
            st.setFont(new Font(MONO, Font.BOLD, 12));
            st.drawString("DANGER", 719, 324);
            st.dispose();
            for (int i = 0; i < enemy.attacks().size(); i++) {
                Attack a = enemy.attacks().get(i);
                Rectangle ar = attackRect(i);
                Art.attackIcon(g, 562, ar.y + 2, a);
                g.setFont(new Font(MONO, Font.BOLD, 14));
                g.setColor(ar.contains(mouse) ? BLOOD : INK);
                g.drawString(a.name(), 592, ar.y + 18);
            }
            int fy = attackRect(enemy.attacks().size()).y + 8;
            g.setFont(new Font(SERIF, Font.ITALIC, 12));
            g.setColor(new Color(64, 46, 36));
            int ly = fy + 10;
            for (String para : enemy.notes().split("\n"))
                for (String line : wrap(g.getFontMetrics(), para, 214)) {
                    if (ly > 514) break;
                    g.drawString(line, 562, ly);
                    ly += 14;
                }
        }

        int itemCount(int i) { return i == 0 ? items.textbooks : i == 1 ? items.redPens : items.hallPasses; }

        static long pct(double fraction) { return Math.round(fraction * 100); }

        // Strength of the items clicked this turn (more clicks = stronger, up to the MAX in SETTINGS)
        double blockNow() { return Math.min(TEXTBOOK_MAX, booksActive * TEXTBOOK_PER_USE); }
        double boostNow() { return Math.min(RED_PEN_MAX, pensActive * RED_PEN_PER_USE); }
        int dodgeNow()    { return Math.min(HALL_PASS_MAX, passesActive * HALL_PASS_PER_USE); }

        /** Clicking a locker item uses one up. It takes effect on the next attack (yours for the pen, the enemy's for the others). */
        void useItem(int i) {
            if (itemCount(i) <= 0) { showBanner("You don't have a " + ITEM_NAMES[i] + ".", 70); return; }
            boolean maxed = i == 0 ? blockNow() >= TEXTBOOK_MAX : i == 1 ? boostNow() >= RED_PEN_MAX : dodgeNow() >= HALL_PASS_MAX;
            if (maxed) { showBanner("Using another won't help any more this turn.", 70); return; }
            switch (i) {
                case 0 -> { items.textbooks--; booksActive++; showBanner("Textbook raised: the next hit does " + pct(blockNow()) + "% less damage.", 100); }
                case 1 -> { items.redPens--; pensActive++; showBanner("Red Pen ready: your next attack does +" + pct(boostNow()) + "% damage.", 100); }
                default -> { items.hallPasses--; passesActive++; showBanner("Hall Pass ready: " + dodgeNow() + "% chance to dodge the next attack.", 100); }
            }
        }

        String itemTooltip(int i) {
            String effect = i == 0 ? "Click to use one: the next enemy hit does " + pct(TEXTBOOK_PER_USE) + "% less damage (stacks up to " + pct(TEXTBOOK_MAX) + "%)."
                          : i == 1 ? "Click to use one: your next attack does " + pct(RED_PEN_PER_USE) + "% more damage (stacks up to +" + pct(RED_PEN_MAX) + "%)."
                          : "Click to use one: " + HALL_PASS_PER_USE + "% chance to dodge the next enemy attack (stacks up to " + HALL_PASS_MAX + "%).";
            String active = i == 0 && booksActive > 0 ? " Active now: " + pct(blockNow()) + "% less."
                          : i == 1 && pensActive > 0 ? " Active now: +" + pct(boostNow()) + "%."
                          : i == 2 && passesActive > 0 ? " Active now: " + dodgeNow() + "% dodge." : "";
            return (itemCount(i) == 0 ? "None left. " : "") + effect + active + " Doesn't cost your turn.";
        }

        Rectangle itemRect(int i) { return new Rectangle(20 + i * 84, 378, 78, 142); }
        Rectangle attackRect(int i) { return new Rectangle(556, 332 + i * 26, 222, 26); }

        void metalPanel(Graphics2D g, int x, int y, int w, int h) {
            g.setPaint(new GradientPaint(x, y, new Color(74, 76, 74), x, y + h, new Color(40, 40, 40)));
            g.fillRect(x, y, w, h);
            g.setColor(new Color(16, 16, 16));
            g.fillRect(x + 6, y + 6, w - 12, h - 12);
            g.setColor(new Color(110, 60, 34, 120)); // rust
            Random r = new Random(x * 31 + y);
            for (int i = 0; i < 6; i++) g.fill(el(x + r.nextInt(w - 10), (r.nextBoolean() ? y : y + h - 6) + r.nextInt(3), 8 + r.nextInt(14), 4));
            g.setColor(new Color(110, 110, 106));
            for (int[] p : new int[][] { { x + 2, y + 2 }, { x + w - 6, y + 2 }, { x + 2, y + h - 6 }, { x + w - 6, y + h - 6 } }) g.fill(el(p[0], p[1], 4, 4));
            g.setColor(new Color(10, 10, 10));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(x, y, w, h);
        }

        void drawGrid(Graphics2D g) {
            for (int i = 0; i < 16; i++) {
                Rectangle r = tileRect(i);
                Tile t = grid[i];
                g.setColor(new Color(10, 14, 12));
                g.fillRect(r.x, r.y, r.width, r.height);
                if (t.selected) continue;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.clipRect(GRID_X - 4, GRID_Y - 4, 4 * (TILE + GAP) + 4, 4 * (TILE + GAP) + 4);
                g2.setComposite(AlphaComposite.SrcOver.derive((float) (1 - t.drop)));
                boolean hover = r.contains(mouse) && canAct() && t.scratched == 0;
                drawTile(g2, r.x, r.y - t.drop * 40, TILE, t.label(), t.kind, t.scratched, hover, t.sparkle, t.seed);
                g2.dispose();
            }
        }

        /** A letter tile: an old, stained paper card. */
        void drawTile(Graphics2D g, double x, double y, double s, String label, Kind kind, int scratched, boolean hover, int sparkle, int seed) {
            Rectangle2D r = new Rectangle2D.Double(x, y, s, s);
            Color top, bottom;
            if (scratched > 0) { top = new Color(150, 140, 116); bottom = new Color(108, 98, 80); }
            else switch (kind) {
                case GREEN -> { top = new Color(190, 212, 166); bottom = new Color(124, 158, 106); }
                case GOLD -> { top = new Color(232, 198, 118); bottom = new Color(184, 138, 56); }
                case CRYSTAL -> { top = new Color(192, 210, 224); bottom = new Color(124, 150, 176); }
                default -> { top = new Color(224, 214, 182); bottom = new Color(182, 166, 128); }
            }
            if (sparkle > 0) {
                double gr = s * 0.2 + (60 - sparkle) * 0.3;
                g.setColor(new Color(240, 220, 170, Math.min(160, sparkle * 3)));
                g.fill(new Rectangle2D.Double(x - gr / 2, y - gr / 2, s + gr, s + gr));
            }
            g.setColor(new Color(0, 0, 0, 140));
            g.fill(new Rectangle2D.Double(x + 2, y + 3, s, s));
            g.setPaint(new GradientPaint((float) x, (float) y, top, (float) x, (float) (y + s), bottom));
            g.fill(r);
            Random sr = new Random(seed); // stains and a folded corner, different on every card
            for (int i = 0; i < 2; i++) {
                double d = s * (0.12 + sr.nextDouble() * 0.2);
                g.setColor(new Color(110, 76, 40, 30 + sr.nextInt(40)));
                g.fill(el(x + sr.nextDouble() * (s - d), y + sr.nextDouble() * (s - d), d, d * 0.8));
            }
            if (sr.nextInt(3) == 0) {
                double c = s * 0.2;
                Path2D fold = new Path2D.Double();
                fold.moveTo(x + s - c, y); fold.lineTo(x + s, y + c); fold.lineTo(x + s - c, y + c); fold.closePath();
                g.setColor(new Color(150, 136, 104));
                g.fill(fold);
                g.setColor(new Color(10, 14, 12));
                Path2D gone = new Path2D.Double();
                gone.moveTo(x + s - c, y); gone.lineTo(x + s, y); gone.lineTo(x + s, y + c); gone.closePath();
                g.fill(gone);
            }
            g.setColor(hover ? new Color(200, 30, 30) : new Color(70, 56, 40));
            g.setStroke(new BasicStroke(hover ? 3 : 1.5f));
            g.draw(r);

            g.setFont(new Font(SERIF, Font.BOLD, (int) (s * (label.length() > 1 ? 0.5 : 0.64))));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(scratched > 0 ? new Color(60, 54, 46) : INK);
            g.drawString(label, (float) (x + (s - fm.stringWidth(label)) / 2), (float) (y + s / 2 + fm.getAscent() * 0.36));

            if (scratched > 0) { // scribbled out in black, with a red turns-left counter
                g.setColor(new Color(16, 12, 10, 220));
                g.setStroke(new BasicStroke((float) (s * 0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D scr = new Path2D.Double();
                scr.moveTo(x + s * 0.15, y + s * 0.35);
                for (int i = 0; i < 5; i++) scr.lineTo(x + s * (0.25 + i * 0.13), y + s * (i % 2 == 0 ? 0.68 : 0.3));
                g.draw(scr);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (s * 0.3)));
                g.setColor(new Color(200, 20, 20));
                g.drawString(String.valueOf(scratched), (float) (x + s * 0.74), (float) (y + s * 0.95));
            } else if (kind != Kind.NORMAL) {
                double gx = x + s * 0.82, gy = y + s * 0.18, gs = s * 0.1;
                g.setStroke(new BasicStroke(1));
                if (kind == Kind.GREEN) {
                    g.setColor(new Color(30, 110, 40));
                    g.fill(new Rectangle2D.Double(gx - gs, gy - gs * 0.35, gs * 2, gs * 0.7));
                    g.fill(new Rectangle2D.Double(gx - gs * 0.35, gy - gs, gs * 0.7, gs * 2));
                } else {
                    Path2D d = new Path2D.Double();
                    d.moveTo(gx, gy - gs); d.lineTo(gx + gs, gy); d.lineTo(gx, gy + gs); d.lineTo(gx - gs, gy); d.closePath();
                    g.setColor(kind == Kind.GOLD ? new Color(150, 90, 10) : new Color(40, 80, 150));
                    g.fill(d);
                }
            }
        }

        /** The SHUFFLE button */
        void drawShuffleButton(Graphics2D g, boolean act) {
            Rectangle r = SHUFFLE_BTN;
            button(g, r, "", new Color(78, 80, 78), new Color(38, 40, 40), PALE, 20, act && r.contains(mouse), !act);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(act ? PALE : new Color(110, 106, 100));
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double x0 = r.x + 12, x1 = r.x + r.width - 16, y0 = r.y + 15, y1 = r.y + r.height - 15, mid = (x0 + x1) / 2;
            g2.draw(new CubicCurve2D.Double(x0, y0, mid, y0, mid, y1, x1, y1));
            g2.draw(new CubicCurve2D.Double(x0, y1, mid, y1, mid, y0, x1, y0));
            for (double ay : new double[] { y0, y1 }) {
                Path2D head = new Path2D.Double();
                head.moveTo(x1 + 5, ay); head.lineTo(x1 - 2, ay - 5); head.lineTo(x1 - 2, ay + 5); head.closePath();
                g2.fill(head);
            }
            g2.dispose();
        }

        void drawButtons(Graphics2D g) {
            boolean act = canAct();
            Color metalTop = new Color(78, 80, 78), metalBottom = new Color(38, 40, 40);
            button(g, SCRAMBLE_BTN, "SCRAMBLE", metalTop, metalBottom, PALE, 20, act && SCRAMBLE_BTN.contains(mouse), !act);
            boolean valid = act && wordValid();
            if (valid) {
                g.setColor(new Color(200, 20, 20, (int) (60 + 50 * Math.sin(ticks / 8.0))));
                g.fill(new Rectangle2D.Double(ATTACK_BTN.x - 6, ATTACK_BTN.y - 6, ATTACK_BTN.width + 12, ATTACK_BTN.height + 12));
            }
            button(g, ATTACK_BTN, "ATTACK", valid ? new Color(176, 26, 24) : new Color(58, 56, 54), valid ? new Color(92, 8, 10) : new Color(32, 30, 28),
                    valid ? Color.WHITE : new Color(110, 106, 100), 26, valid && ATTACK_BTN.contains(mouse), false);
            button(g, MENU_BTN, "MENU", metalTop, metalBottom, PALE, 20, MENU_BTN.contains(mouse), false);
            drawShuffleButton(g, act);
        }

        void button(Graphics2D g, Rectangle r, String text, Color top, Color bottom, Color textColor, int size, boolean hover, boolean dim) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(r.x + 3, r.y + 4, r.width, r.height);
            g.setPaint(new GradientPaint(0, r.y, hover ? top.brighter() : top, 0, r.y + r.height, bottom));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(new Color(255, 255, 255, 30));
            g.fillRect(r.x + 2, r.y + 2, r.width - 4, 3);
            g.setColor(new Color(12, 10, 10));
            g.setStroke(new BasicStroke(3));
            g.drawRect(r.x, r.y, r.width, r.height);
            g.setColor(new Color(130, 126, 120));
            for (int[] p : new int[][] { { r.x + 5, r.y + 5 }, { r.x + r.width - 9, r.y + 5 }, { r.x + 5, r.y + r.height - 9 }, { r.x + r.width - 9, r.y + r.height - 9 } })
                g.fill(el(p[0], p[1], 4, 4));
            g.setFont(new Font(MONO, Font.BOLD, size));
            FontMetrics fm = g.getFontMetrics();
            shadowText(g, text, r.x + (r.width - fm.stringWidth(text)) / 2, r.y + (r.height + fm.getAscent()) / 2 - 4, textColor);
            if (dim) { g.setColor(new Color(0, 0, 0, 120)); g.fillRect(r.x, r.y, r.width, r.height); }
        }

        void drawTooltip(Graphics2D g) {
            if (menuOpen || lost || won) return;
            String title = null, body = null;
            for (int i = 0; i < 3; i++) if (itemRect(i).contains(mouse)) { title = ITEM_NAMES[i] + (itemCount(i) > 0 ? " x" + itemCount(i) : ""); body = itemTooltip(i); }
            for (int i = 0; i < enemy.attacks().size(); i++) if (attackRect(i).contains(mouse)) {
                Attack a = enemy.attacks().get(i);
                title = a.name();
                body = a.desc();
                if (a.damage() > 0) body += " Deals " + enemyDamage(a) + " damage.";
                if (a.scratch() > 0) body += " Scratches out " + a.scratch() + " of your letters for 2 turns.";
                if (a.heal() > 0) body += " Heals itself " + a.heal() + " HP.";
            }
            if (KIT_BOX.contains(mouse)) { title = "First Aid Kit"; body = "Restores " + KIT_HEAL + " HP. Using one does not cost your turn."; }
            if (SCRAMBLE_BTN.contains(mouse)) { title = "Scramble"; body = "Swap all your letters for new ones. Costs your turn: the enemy attacks."; }
            if (SHUFFLE_BTN.contains(mouse)) { title = "Shuffle"; body = "Move your letters into a new order to help you spot words. Same letters, and it doesn't cost your turn. (Space bar)"; }
            if (title == null) return;
            g.setFont(new Font(SERIF, Font.PLAIN, 13));
            List<String> lines = wrap(g.getFontMetrics(), body, 210);
            int w = 230, h = 32 + lines.size() * 16;
            int x = Math.min(mouse.x + 14, W - w - 6), y = Math.max(TOP_H + 4, mouse.y - h - 10);
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(x + 3, y + 3, w, h);
            g.setColor(new Color(214, 204, 174));
            g.fillRect(x, y, w, h);
            g.setColor(new Color(60, 44, 30));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(x, y, w, h);
            g.setFont(new Font(MONO, Font.BOLD, 13));
            g.setColor(BLOOD);
            g.drawString(title, x + 10, y + 19);
            g.setFont(new Font(SERIF, Font.PLAIN, 13));
            g.setColor(INK);
            for (int i = 0; i < lines.size(); i++) g.drawString(lines.get(i), x + 10, y + 36 + i * 16);
        }

        void overlay(Graphics2D g, int x, int y, int w, int h, String title, Color titleColor) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, W, H);
            g.setPaint(new GradientPaint(0, y, new Color(206, 194, 160), 0, y + h, new Color(156, 142, 108)));
            g.fillRect(x, y, w, h);
            Random r = new Random(title.hashCode());
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(90, 60, 30, 25 + r.nextInt(30)));
                g.fill(el(x + r.nextInt(w - 40), y + r.nextInt(h - 30), 24 + r.nextInt(40), 16 + r.nextInt(24)));
            }
            g.setColor(new Color(30, 22, 16));
            g.setStroke(new BasicStroke(3));
            g.drawRect(x, y, w, h);
            g.setFont(new Font(SERIF, Font.BOLD, 30));
            g.setColor(titleColor);
            g.drawString(title, 400 - g.getFontMetrics().stringWidth(title) / 2, y + 46);
        }

        void subtitle(Graphics2D g, String s, int y) {
            g.setFont(new Font(SERIF, Font.ITALIC, 15));
            g.setColor(INK);
            g.drawString(s, 400 - g.getFontMetrics().stringWidth(s) / 2, y);
        }

        void darkButton(Graphics2D g, Rectangle r, String text, boolean red) {
            button(g, r, text, red ? new Color(160, 24, 22) : new Color(78, 80, 78), red ? new Color(84, 8, 10) : new Color(38, 40, 40),
                    red ? Color.WHITE : PALE, 19, r.contains(mouse), false);
        }

        void drawMenu(Graphics2D g) {
            overlay(g, 250, 190, 300, 196, "PAUSED", INK);
            darkButton(g, RESUME_BTN, "RESUME", true);
            darkButton(g, RESTART_BTN, "RESTART FIGHT", false);
        }

        void drawLost(Graphics2D g) {
            overlay(g, 180, 200, 440, 226, "YOU DIDN'T MAKE IT", BLOOD);
            subtitle(g, "The " + enemy.name() + " dragged you into the dark...", 284);
            darkButton(g, RETRY_BTN, "TRY AGAIN", true);
            darkButton(g, GIVEUP_BTN, "GIVE UP", false);
        }

        void drawWon(Graphics2D g) {
            overlay(g, 230, 210, 340, 178, "YOU SURVIVED", INK);
            subtitle(g, "The " + enemy.name() + " is gone... for now.", 290);
            darkButton(g, CONTINUE_BTN, "CONTINUE", true);
        }

        static void shadowText(Graphics2D g, String s, int x, int y, Color c) {
            g.setColor(new Color(0, 0, 0, 190));
            g.drawString(s, x + 1, y + 2);
            g.setColor(c);
            g.drawString(s, x, y);
        }

        static List<String> wrap(FontMetrics fm, String text, int width) {
            List<String> out = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String w : text.split(" ")) {
                String trial = line.length() == 0 ? w : line + " " + w;
                if (fm.stringWidth(trial) > width && line.length() > 0) { out.add(line.toString()); line = new StringBuilder(w); }
                else line = new StringBuilder(trial);
            }
            if (line.length() > 0) out.add(line.toString());
            return out;
        }
    }

    // =====================================================================================
    //  Sprites
    // =====================================================================================

    public static class Sprites {
        static final Map<String, Optional<BufferedImage>> CACHE = new HashMap<>();

        /** required = false for the optional poses, so a missing pose isn't reported as a problem. */
        public static BufferedImage get(String key, boolean required) {
            return CACHE.computeIfAbsent(key, k -> {
                try (InputStream in = resource(k + ".png")) {
                    if (in != null) return Optional.ofNullable(javax.imageio.ImageIO.read(in));
                    if (required) System.err.println("Could not find sprite " + k + ".png");
                } catch (IOException e) {
                    System.err.println("Could not load sprite " + k + ": " + e.getMessage());
                }
                return Optional.empty();
            }).orElse(null);
        }

        /** Draws the image bottom-centred on (x, feetY), fitted into a box maxH tall and 1.3x as wide. */
        static void drawFit(Graphics2D g, BufferedImage img, double x, double feetY, double maxH) {
            double fit = Math.min(maxH / img.getHeight(), maxH * 1.3 / img.getWidth());
            double w = img.getWidth() * fit, h = img.getHeight() * fit;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, img.getHeight() <= 128
                    ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(img, (int) Math.round(x - w / 2), (int) Math.round(feetY - h), (int) Math.round(w), (int) Math.round(h), null);
            g2.dispose();
        }
    }

    // =====================================================================================
    //  Small drawings for the UI: first aid kit, locker items, attack icons
    // =====================================================================================

    static class Art {
        static final Color OUT = new Color(20, 14, 12);

        static Graphics2D at(Graphics2D g, double x, double y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            return g2;
        }

        static void shape(Graphics2D g, Shape s, Paint fill) { g.setPaint(fill); g.fill(s); g.setColor(OUT); g.draw(s); }

        static void firstAid(Graphics2D g, double cx, double cy, boolean full) {
            Graphics2D g2 = at(g, cx, cy);
            g2.setColor(OUT);
            g2.draw(new Arc2D.Double(-9, -24, 18, 14, 0, 180, Arc2D.OPEN));
            Color box = full ? new Color(206, 200, 184) : new Color(90, 88, 84);
            shape(g2, new RoundRectangle2D.Double(-22, -16, 44, 32, 6, 6), new GradientPaint(0, -16, box, 0, 16, box.darker()));
            g2.setColor(full ? new Color(176, 20, 22) : new Color(70, 40, 40));
            g2.fill(new Rectangle2D.Double(-4, -10, 8, 20));
            g2.fill(new Rectangle2D.Double(-10, -4, 20, 8));
            g2.setColor(new Color(110, 76, 40, 70));
            g2.fill(BattlePanel.el(8, 4, 10, 7));
            g2.dispose();
        }

        /** 0 = Old Textbook, 1 = Red Pen, 2 = Hall Pass */
        static void item(Graphics2D g, int which, double cx, double cy) {
            Graphics2D g2 = at(g, cx, cy);
            switch (which) {
                case 0 -> {
                    g2.rotate(Math.toRadians(-8));
                    shape(g2, new Rectangle2D.Double(-18, -24, 40, 50), new Color(214, 204, 172));
                    shape(g2, new Rectangle2D.Double(-22, -26, 38, 50), new GradientPaint(-22, 0, new Color(40, 66, 48), 16, 0, new Color(26, 44, 32)));
                    g2.setColor(new Color(16, 28, 20));
                    g2.fill(new Rectangle2D.Double(-22, -26, 7, 50));
                    g2.setColor(new Color(170, 150, 90));
                    g2.fill(new Rectangle2D.Double(-10, -14, 20, 4));
                    g2.fill(new Rectangle2D.Double(-8, -6, 16, 2));
                    g2.setColor(new Color(110, 60, 30, 90));
                    g2.fill(BattlePanel.el(0, 6, 12, 10));
                }
                case 1 -> {
                    g2.rotate(Math.toRadians(-45));
                    shape(g2, new RoundRectangle2D.Double(-24, -5, 40, 10, 4, 4), new Color(176, 22, 26));
                    shape(g2, new RoundRectangle2D.Double(10, -6, 16, 12, 4, 4), new Color(120, 12, 16));
                    shape(g2, new Rectangle2D.Double(12, -9, 12, 3), new Color(160, 160, 166));
                    Path2D tip = new Path2D.Double();
                    tip.moveTo(-24, -5); tip.lineTo(-34, 0); tip.lineTo(-24, 5); tip.closePath();
                    shape(g2, tip, new Color(206, 196, 170));
                    g2.setColor(new Color(176, 22, 26));
                    g2.fill(BattlePanel.el(-36, -1.5, 3, 3));
                }
                default -> {
                    g2.setColor(new Color(170, 160, 130));
                    g2.draw(new QuadCurve2D.Double(-6, -16, 0, -34, 6, -16));
                    shape(g2, new RoundRectangle2D.Double(-26, -16, 52, 30, 6, 6), new GradientPaint(0, -16, new Color(140, 102, 64), 0, 14, new Color(92, 64, 38)));
                    g2.setColor(new Color(10, 8, 8));
                    g2.fill(BattlePanel.el(-3, -13, 6, 6));
                    g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
                    g2.setColor(new Color(36, 24, 14));
                    g2.drawString("PASS", -16, 9);
                }
            }
            g2.dispose();
        }

        static void attackIcon(Graphics2D g, int x, int y, Attack a) {
            Graphics2D g2 = at(g, x, y);
            shape(g2, new Rectangle2D.Double(0, 0, 22, 22), new Color(34, 26, 22));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (a.scratch() > 0) {
                g2.setColor(new Color(210, 204, 190));
                Path2D p = new Path2D.Double();
                p.moveTo(4, 8); p.lineTo(8, 16); p.lineTo(12, 6); p.lineTo(16, 16); p.lineTo(19, 8);
                g2.draw(p);
            } else if (a.heal() > 0) {
                g2.setColor(new Color(120, 190, 100));
                g2.draw(new Line2D.Double(11, 5, 11, 17)); g2.draw(new Line2D.Double(5, 11, 17, 11));
            } else {
                g2.setColor(new Color(200, 30, 30));
                for (int i = 0; i < 3; i++) g2.draw(new Line2D.Double(5 + i * 5, 18, 9 + i * 5, 4));
            }
            g2.dispose();
        }
    }
}