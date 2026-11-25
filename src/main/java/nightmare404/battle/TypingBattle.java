package nightmare404.battle;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import javax.swing.*;

import nightmare404.battle.BattleScreen.Attack;
import nightmare404.battle.BattleScreen.BattleResult;
import nightmare404.battle.BattleScreen.EnemyDef;
import nightmare404.battle.BattleScreen.Items;
import nightmare404.battle.BattleScreen.Sprites;
import nightmare404.main.GamePanel;
import nightmare404.ui.HealthBar;

// The quadrangle's fights (its trees, see nightmare404.entity.TreeMonster): a typing race instead
// of the word grid.
//
//   A word appears with a timer under it. Type it before the timer runs out and it hits the monster:
//   harder for longer words, harder still if you were QUICK (more than half the time left) or on a
//   STREAK (words in a row without a mistake). Run out of time and the monster hits you.
//   A wrong key costs a little time and breaks the streak. As the monster weakens, its words get
//   longer and the timer shorter.
//   The number keys use what you carry: 1 First Aid Kit (heals), 2 Old Textbook (halves the next
//   hit), 3 Red Pen (doubles the next word), 4 Hall Pass (skips the word without being hit).
//
// The rules are in Duel, which draws nothing (so the checks can play a whole fight); TypingPanel
// draws it and reads the keyboard. It's opened like the word battle: TypingBattle.fight(...) shows it
// in the game's window and hands a BattleResult to game.battleOver when the player wins, or loses
// and gives up.
public class TypingBattle {
    // ---- Time (in seconds, at 60 frames a second) ----
    static final int FPS = 60;
    static final double TIME_BASE = 1.5;           // Every word gets this long...
    static final double TIME_PER_LETTER = 0.35;    // ...plus this much per letter
    static final double FASTEST = 0.70;            // A nearly dead monster's words get only 70% of that
    static final double MISTAKE_COST = 0.25;       // Time lost for each wrong key
    static final int READY_TICKS = 150;            // "3, 2, 1" before the first word
    static final int AFTER_WORD = 22, AFTER_HIT = 45;   // Pauses (in frames) to let the hits play out

    // ---- Damage ----
    static final double DAMAGE_PER_LETTER = 2.5;   // The same as the word battle
    static final double QUICK_BONUS = 1.5;         // Typed with more than half the time left
    static final double STREAK_BONUS = 0.10;       // +10% for each word in a row without a mistake...
    static final int STREAK_MAX = 5;               // ...up to +50%
    static final double RED_PEN = 2.0;             // A Red Pen doubles the next word
    static final double TEXTBOOK = 0.5;            // An Old Textbook halves the next hit

    // Shows the fight in the game's window, in place of whatever is there, and hands the result to
    // game.battleOver when the player wins, or loses and gives up. Items are used up in place.
    public static void fight(GamePanel game, JFrame frame, EnemyDef enemy, int hp, int kits, Items items) {
        TypingPanel panel = new TypingPanel(enemy, hp, kits, items, game);
        frame.setContentPane(panel);
        frame.revalidate();
        panel.start();
    }

    // A picture of a fight against this enemy, frames frames in, without opening a window
    // (for the debug tools and the checks)
    public static BufferedImage preview(EnemyDef enemy, int frames) {
        TypingPanel panel = new TypingPanel(enemy, BattleScreen.HERO_MAX_HP, BattleScreen.START_KITS, new Items(1, 1, 1), null);
        panel.setSize(TypingPanel.W, TypingPanel.H);
        for (int i = 0; i < frames; i++) panel.tick();
        BufferedImage image = new BufferedImage(TypingPanel.W, TypingPanel.H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        return image;
    }

    // The words, from /battle/typing_words.txt (a few stand in if it's missing)
    private static List<String> words;

    public static List<String> words() {
        if (words != null) return words;
        Set<String> list = new LinkedHashSet<>();
        try (InputStream in = BattleScreen.resource("typing_words.txt")) {
            if (in == null) {
                System.err.println("Could not find /battle/typing_words.txt");
            } else {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = r.readLine()) != null) {
                    String w = line.trim().toLowerCase();
                    if (!w.isEmpty() && !w.startsWith("#") && BattleScreen.onlyLetters(w)) list.add(w);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read typing_words.txt: " + e.getMessage());
        }
        if (list.size() < 20) {                    // The file's missing: a few words to be going on with
            String spare = "oak elm root bark leaf twig moss branch forest shadow school hollow roots thorn "
                    + "midnight detention whisper darkness nightmare overgrown";
            for (String w : spare.split(" ")) list.add(w);
        }
        words = new ArrayList<>(list);
        return words;
    }

    // =====================================================================================
    //  The rules
    // =====================================================================================

    public static class Duel {
        public enum State { READY, TYPING, PAUSE, WON, LOST }
        public enum Kind { FOE_HIT, HERO_HIT, FOE_HEALED, HERO_HEALED, MISTAKE, ITEM }
        // Something that just happened, for the screen to show (amount: damage or healing)
        public static class Event {
            private final Kind kind;
            private final int amount;
            private final String text;

            public Event(Kind kind, int amount, String text) {
                this.kind = kind;
                this.amount = amount;
                this.text = text;
            }

            public Kind kind() { return kind; }
            public int amount() { return amount; }
            public String text() { return text; }
        }

        public final EnemyDef enemy;
        public final Items items;
        public int heroHp, foeHp, kits;
        public String word = "";
        public int typed;                       // How many letters of the word are typed
        public int timeLeft, timeMax;           // In frames
        public int streak;                      // Words in a row without a mistake
        public boolean textbookReady, redPenReady;
        public State state;
        public final List<Event> events = new ArrayList<>();   // Since the screen last looked (it clears them)

        private int stateTimer;
        private final int startHp, startKits;
        private final int[] startItems;
        private final Random random;
        private final List<String> words = words();

        public Duel(EnemyDef enemy, int hp, int kits, Items items, long seed) {
            this.enemy = enemy;
            if (items == null) items = new Items();
            this.items = items;
            startHp = Math.max(1, Math.min(BattleScreen.HERO_MAX_HP, hp));
            startKits = Math.max(0, kits);
            startItems = new int[] {this.items.textbooks, this.items.redPens, this.items.hallPasses};
            random = new Random(seed);
            restart();
        }

        // Back to how the fight began (Try Again)
        public void restart() {
            heroHp = startHp;
            foeHp = enemy.maxHp();
            kits = startKits;
            items.textbooks = startItems[0];
            items.redPens = startItems[1];
            items.hallPasses = startItems[2];
            streak = 0;
            textbookReady = false;
            redPenReady = false;
            word = "";
            typed = 0;
            timeLeft = 0;
            timeMax = 0;
            state = State.READY;
            stateTimer = READY_TICKS;
            events.clear();
        }

        public BattleResult result() {
            if (state == State.WON) return new BattleResult(true, heroHp, kits);
            return new BattleResult(false, 0, kits);
        }

        // 0 = the monster is unhurt ... 1 = it's nearly dead
        public double weakness() {
            return 1 - (double) foeHp / enemy.maxHp();
        }

        public int readyCountdown() {
            if (state == State.READY) return stateTimer;
            return 0;
        }

        public void tick() {
            if (state == State.READY) {               // "3, 2, 1..." then the first word
                stateTimer--;
                if (stateTimer <= 0) nextWord();
            } else if (state == State.TYPING) {       // The word's clock runs down
                timeLeft--;
                if (timeLeft <= 0) foeAttacks();
            } else if (state == State.PAUSE) {        // A moment for the hit to show, then what's next
                stateTimer--;
                if (stateTimer > 0) return;
                if (foeHp <= 0) {
                    state = State.WON;
                } else if (heroHp <= 0) {
                    state = State.LOST;
                } else {
                    nextWord();
                }
            }
        }

        // A key was typed (only letters count)
        public void type(char c) {
            if (state != State.TYPING || !Character.isLetter(c)) return;
            if (Character.toLowerCase(c) == word.charAt(typed)) {
                typed++;
                if (typed == word.length()) wordDone();
            } else {
                streak = 0;
                timeLeft = Math.max(1, timeLeft - (int) Math.round(MISTAKE_COST * FPS));
                events.add(new Event(Kind.MISTAKE, 0, ""));
            }
        }

        public boolean useKit() {
            if (kits <= 0 || heroHp <= 0 || heroHp >= BattleScreen.HERO_MAX_HP || !fighting()) return false;
            kits--;
            int before = heroHp;
            heroHp = Math.min(BattleScreen.HERO_MAX_HP, heroHp + BattleScreen.KIT_HEAL);
            events.add(new Event(Kind.HERO_HEALED, heroHp - before, "First Aid Kit"));
            return true;
        }

        public boolean useTextbook() {
            if (items.textbooks <= 0 || textbookReady || !fighting()) return false;
            items.textbooks--;
            textbookReady = true;
            events.add(new Event(Kind.ITEM, 0, "Old Textbook: the next hit is halved."));
            return true;
        }

        public boolean useRedPen() {
            if (items.redPens <= 0 || redPenReady || !fighting()) return false;
            items.redPens--;
            redPenReady = true;
            events.add(new Event(Kind.ITEM, 0, "Red Pen: the next word hits twice as hard."));
            return true;
        }

        public boolean useHallPass() {
            if (items.hallPasses <= 0 || state != State.TYPING) return false;
            items.hallPasses--;
            events.add(new Event(Kind.ITEM, 0, "Hall Pass: excused from \"" + word + "\"."));
            nextWord();
            return true;
        }

        private boolean fighting() {
            return (state == State.READY || state == State.TYPING || state == State.PAUSE) && heroHp > 0 && foeHp > 0;
        }

        // The word's done: it hits the monster
        private void wordDone() {
            boolean quick = timeLeft * 2 > timeMax;   // More than half the time left
            double quickBonus = 1, penBonus = 1;
            String note = "";
            if (quick) {
                quickBonus = QUICK_BONUS;
                note = "QUICK!";
            }
            if (redPenReady) {
                penBonus = RED_PEN;
                note = "RED PEN!";
            }
            double damage = word.length() * DAMAGE_PER_LETTER * quickBonus
                    * (1 + STREAK_BONUS * Math.min(streak, STREAK_MAX)) * penBonus;
            int hit = Math.max(1, (int) Math.round(damage));
            redPenReady = false;
            streak++;
            foeHp = Math.max(0, foeHp - hit);
            events.add(new Event(Kind.FOE_HIT, hit, note));
            pause(AFTER_WORD);
        }

        // Time's up: the monster attacks (or mends itself)
        private void foeAttacks() {
            Attack attack = pickAttack();
            streak = 0;
            if (attack.damage() <= 0 && attack.heal() > 0) {
                int before = foeHp;
                foeHp = Math.min(enemy.maxHp(), foeHp + attack.heal());
                events.add(new Event(Kind.FOE_HEALED, foeHp - before, attack.name() + ": " + attack.desc()));
            } else {
                int damage = attack.damage();
                if (textbookReady) {
                    damage = (int) Math.round(damage * TEXTBOOK);
                    textbookReady = false;
                }
                heroHp = Math.max(0, heroHp - damage);
                events.add(new Event(Kind.HERO_HIT, damage, attack.name() + ": " + attack.desc()));
            }
            pause(AFTER_HIT);
        }

        // One of the monster's attacks, by their weights (no mending while it's unhurt)
        private Attack pickAttack() {
            List<Attack> choices = new ArrayList<>();
            for (Attack a : enemy.attacks()) {
                if (a.damage() <= 0 && foeHp >= enemy.maxHp()) continue;
                for (int i = 0; i < Math.max(1, a.weight()); i++) choices.add(a);
            }
            if (choices.isEmpty()) return new Attack("Strike", 8, 0, 0, 1, "It strikes.");
            return choices.get(random.nextInt(choices.size()));
        }

        private void pause(int frames) {
            state = State.PAUSE;
            stateTimer = frames;
        }

        // A new word: 3-6 letters while the monster is strong, up to 6-11 when it's nearly dead,
        // with less time per letter as it weakens
        private void nextWord() {
            double w = weakness();
            int shortest = 3 + (int) Math.round(w * 3);
            int longest = 6 + (int) Math.round(w * 5);
            List<String> fit = new ArrayList<>();
            for (String s : words) {
                if (s.length() >= shortest && s.length() <= longest && !s.equals(word)) fit.add(s);
            }
            if (fit.isEmpty()) fit = words;
            word = fit.get(random.nextInt(fit.size()));
            typed = 0;
            double seconds = (TIME_BASE + TIME_PER_LETTER * word.length()) * (1 - (1 - FASTEST) * w);
            timeMax = (int) Math.round(seconds * FPS);
            timeLeft = timeMax;
            state = State.TYPING;
        }
    }

    // =====================================================================================
    //  The screen
    // =====================================================================================

    // The fight listens for its own keys, mouse and timer: Swing calls keyTyped, keyPressed,
    // mousePressed, mouseMoved and actionPerformed (the other listener methods it must have do nothing).
    static class TypingPanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
        static final int W = 800, H = 600, TOP_H = 58, HORIZON = 232, STAGE_BOTTOM = 372, FLOOR_Y = 346;
        static final int HERO_X = 180, FOE_X = 600;
        static final Rectangle CONTINUE_BTN = new Rectangle(300, 330, 200, 46);
        static final Rectangle RETRY_BTN = new Rectangle(300, 312, 200, 46), GIVEUP_BTN = new Rectangle(300, 366, 200, 46);
        static final Color PALE = new Color(206, 200, 184), BLOOD = new Color(170, 18, 22), INK = new Color(34, 26, 20);
        static final Color TYPED = new Color(130, 226, 120), NEXT = Color.WHITE;
        static final String MONO = Font.MONOSPACED, SERIF = Font.SERIF;
        static final String HERO_SPRITE = "/characters/player/right_idle";   // The player, facing the monster

        final Duel duel;
        final GamePanel game;   // The game to go back to when the fight is over (null for a preview)
        final javax.swing.Timer timer = new javax.swing.Timer(16, this);   // Calls actionPerformed every 16 ms
        long ticks;
        boolean finished;
        Point mouse = new Point(-1, -1);

        // Animation
        double heroShown, foeShown;              // The health bars slide to the new value
        int heroShake, foeShake, heroLunge, foeLunge, wordShake, redFlash, foeFade;
        String banner = "";
        int bannerLife;
        final List<double[]> floats = new ArrayList<>();      // x, y, life, colour index
        final List<String> floatText = new ArrayList<>();
        final double[][] leaves = new double[22][];

        TypingPanel(EnemyDef enemy, int heroHp, int kits, Items items, GamePanel game) {
            this.game = game;
            duel = new Duel(enemy, heroHp, kits, items, System.nanoTime());
            heroShown = duel.heroHp;
            foeShown = duel.foeHp;
            Random r = new Random(7);
            for (int i = 0; i < leaves.length; i++) {
                leaves[i] = new double[] {r.nextInt(W), TOP_H + r.nextInt(STAGE_BOTTOM - TOP_H), 0.4 + r.nextDouble() * 0.8, r.nextDouble() * 6};
            }
            showBanner("The " + enemy.name() + " tears itself out of the ground!", 150);
            setPreferredSize(new Dimension(W, H));
            setBackground(Color.BLACK);
            setFocusable(true);
            setFocusTraversalKeysEnabled(false);
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        void start() {
            timer.start();
            requestFocusInWindow();
        }

        // The timer: one frame of the fight
        @Override
        public void actionPerformed(ActionEvent e) {
            tick();
            repaint();
        }

        // The player won, or lost and gave up: back to the game with the result
        void finish() {
            if (finished) return;
            finished = true;
            timer.stop();
            if (game != null) game.battleOver(duel.result());
        }

        void showBanner(String text, int life) {
            banner = text;
            bannerLife = life;
        }

        void addFloat(double x, double y, String text, int colour) {
            floats.add(new double[] {x, y, 60, colour});
            floatText.add(text);
        }

        void tick() {
            ticks++;
            Duel.State before = duel.state;
            duel.tick();
            for (Duel.Event e : duel.events) {        // Show what just happened in the duel
                Duel.Kind kind = e.kind();
                if (kind == Duel.Kind.FOE_HIT) {
                    foeShake = 18;
                    heroLunge = 16;
                    addFloat(FOE_X, 170, "-" + e.amount(), 0);
                    if (!e.text().isEmpty()) addFloat(FOE_X, 140, e.text(), 2);
                } else if (kind == Duel.Kind.HERO_HIT) {
                    foeLunge = 22;
                    heroShake = 18;
                    redFlash = 14;
                    addFloat(HERO_X, 200, "-" + e.amount(), 0);
                    showBanner(e.text(), 130);
                } else if (kind == Duel.Kind.FOE_HEALED) {
                    addFloat(FOE_X, 170, "+" + e.amount(), 1);
                    showBanner(e.text(), 130);
                } else if (kind == Duel.Kind.HERO_HEALED) {
                    addFloat(HERO_X, 200, "+" + e.amount(), 1);
                } else if (kind == Duel.Kind.MISTAKE) {
                    wordShake = 10;
                } else if (kind == Duel.Kind.ITEM) {
                    showBanner(e.text(), 110);
                }
            }
            duel.events.clear();
            if (before != Duel.State.WON && duel.state == Duel.State.WON) {
                showBanner("The " + duel.enemy.name() + " shudders, and falls still.", 400);
            }
            if (duel.state == Duel.State.WON && foeFade < 40) foeFade++;
            heroShown += (duel.heroHp - heroShown) * 0.15;
            foeShown += (duel.foeHp - foeShown) * 0.15;
            if (Math.abs(duel.heroHp - heroShown) < 0.05) heroShown = duel.heroHp;   // Close enough: land on it, so
            if (Math.abs(duel.foeHp - foeShown) < 0.05) foeShown = duel.foeHp;       // the bar shows the right segment
            if (heroShake > 0) heroShake--;
            if (foeShake > 0) foeShake--;
            if (heroLunge > 0) heroLunge--;
            if (foeLunge > 0) foeLunge--;
            if (wordShake > 0) wordShake--;
            if (redFlash > 0) redFlash--;
            if (bannerLife > 0) bannerLife--;
            for (int i = floats.size() - 1; i >= 0; i--) {
                double[] f = floats.get(i);
                f[1] -= 0.8;
                f[2]--;
                if (f[2] <= 0) {
                    floats.remove(i);
                    floatText.remove(i);
                }
            }
            for (double[] l : leaves) {
                l[1] += l[2];
                l[0] += Math.sin((ticks + l[3] * 40) / 30.0) * 0.6;
                if (l[1] > STAGE_BOTTOM) { l[1] = TOP_H; l[0] = (l[0] + 211) % W; }
            }
        }

        // A key typed: a letter for the word, or 1-4 to use an item
        @Override
        public void keyTyped(KeyEvent e) {
            char c = e.getKeyChar();
            if (duel.state == Duel.State.WON || duel.state == Duel.State.LOST) return;
            if (c == '1') {
                duel.useKit();
            } else if (c == '2') {
                duel.useTextbook();
            } else if (c == '3') {
                duel.useRedPen();
            } else if (c == '4') {
                duel.useHallPass();
            } else {
                duel.type(c);
            }
        }

        // Enter and Esc, once the fight is over
        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (duel.state == Duel.State.WON && code == KeyEvent.VK_ENTER) finish();
            if (duel.state == Duel.State.LOST && code == KeyEvent.VK_ENTER) restartFight();
            if (duel.state == Duel.State.LOST && code == KeyEvent.VK_ESCAPE) finish();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouse = e.getPoint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            Point p = e.getPoint();
            if (duel.state == Duel.State.WON && CONTINUE_BTN.contains(p)) finish();
            if (duel.state == Duel.State.LOST && RETRY_BTN.contains(p)) restartFight();
            if (duel.state == Duel.State.LOST && GIVEUP_BTN.contains(p)) finish();
        }

        @Override public void keyReleased(KeyEvent e) { }
        @Override public void mouseClicked(MouseEvent e) { }
        @Override public void mouseReleased(MouseEvent e) { }
        @Override public void mouseEntered(MouseEvent e) { }
        @Override public void mouseExited(MouseEvent e) { }
        @Override public void mouseDragged(MouseEvent e) { }

        void restartFight() {
            duel.restart();
            heroShown = duel.heroHp;
            foeShown = duel.foeHp;
            foeFade = 0;
            floats.clear();
            floatText.clear();
            showBanner("Again. Faster this time.", 120);
        }

        // ---------------------------------------------------------------- drawing

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Shape clip = g.getClip();
            g.clipRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
            QuadrangleBackdrop.paint(g, W, TOP_H, HORIZON, STAGE_BOTTOM, ticks);
            drawHero(g);
            drawFoe(g);
            drawAtmosphere(g);
            g.setClip(clip);
            drawTopBar(g);
            drawWordBoard(g);
            drawItems(g);
            drawFloats(g);
            drawBanner(g);
            if (duel.state == Duel.State.WON) drawWon(g);
            if (duel.state == Duel.State.LOST) drawLost(g);
            g.dispose();
        }

        void drawHero(Graphics2D g) {
            double x = HERO_X;
            if (heroLunge > 0) x += Math.sin(Math.PI * heroLunge / 16.0) * 30;
            if (heroShake > 0) {                      // Just hit: shaking, 6 px one way then the other
                if ((heroShake / 2) % 2 == 0) {
                    x += 6;
                } else {
                    x -= 6;
                }
            }
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new Ellipse2D.Double(x - 40, FLOOR_Y - 8, 80, 14));
            BufferedImage img = Sprites.get(HERO_SPRITE, true);
            if (img != null && !(heroShake > 0 && (heroShake / 3) % 2 == 1)) Sprites.drawFit(g, img, x, FLOOR_Y, 150);
        }

        void drawFoe(Graphics2D g) {
            if (foeFade >= 40) return;
            double x = FOE_X, y = FLOOR_Y + Math.abs(Math.sin(ticks / 14.0)) * -3;
            if (foeLunge > 0) x -= Math.sin(Math.PI * foeLunge / 22.0) * 70;
            if (foeShake > 0) {
                if ((foeShake / 2) % 2 == 0) {
                    x += 7;
                } else {
                    x -= 7;
                }
            }
            String key = duel.enemy.sprite();
            boolean windingUp = duel.state == Duel.State.TYPING && duel.timeLeft * 4 < duel.timeMax;
            BufferedImage img = null;
            if (foeLunge > 0 || windingUp) img = Sprites.get(key + "_attack", false);
            else if (foeShake > 0) img = Sprites.get(key + "_hurt", false);
            if (img == null) img = Sprites.get(key, true);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive((float) (1 - foeFade / 40.0)));
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fill(new Ellipse2D.Double(x - 70, FLOOR_Y - 10, 140, 18));
            if (img != null) Sprites.drawFit(g2, img, x, y, 250);
            g2.dispose();
        }

        // Falling leaves, the dark at the edges, and red when the player is hit or nearly dead
        void drawAtmosphere(Graphics2D g) {
            for (double[] l : leaves) {
                g.setColor(new Color(40, 110, 50, 170));
                g.fill(new Ellipse2D.Double(l[0], l[1], 5, 3));
            }
            g.setPaint(new RadialGradientPaint(new Point2D.Double(400, 230), 470f, new float[] {0.45f, 1f},
                    new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 200)}));
            g.fillRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
            int pulse = 0;
            if (duel.heroHp > 0 && duel.heroHp <= BattleScreen.HERO_MAX_HP * 0.3) {
                pulse = (int) (40 + 100 * Math.pow(Math.max(0, Math.sin(ticks / 9.0)), 4));
            }
            if (redFlash > 0) pulse = Math.max(pulse, redFlash * 10);
            if (pulse > 0) {
                g.setPaint(new RadialGradientPaint(new Point2D.Double(400, 220), 470f, new float[] {0.4f, 1f},
                        new Color[] {new Color(140, 0, 0, 0), new Color(140, 0, 0, Math.min(255, pulse))}));
                g.fillRect(0, TOP_H, W, STAGE_BOTTOM - TOP_H);
            }
        }

        void drawTopBar(Graphics2D g) {
            g.setPaint(new GradientPaint(0, 0, new Color(24, 22, 26), 0, TOP_H, new Color(10, 10, 12)));
            g.fillRect(0, 0, W, TOP_H);
            g.setColor(new Color(70, 20, 20));
            g.fillRect(0, TOP_H - 2, W, 2);
            g.setFont(new Font(MONO, Font.BOLD, 16));
            shadow(g, BattleScreen.HERO_NAME, 18, 20, PALE);
            // The health bars (see HealthBar), the same as the word battle's
            int scale = BattleScreen.BattlePanel.BAR_SCALE;
            if (duel.heroHp > 0 && duel.heroHp <= BattleScreen.HERO_MAX_HP * 0.3) {
                BattleScreen.BattlePanel.lowHealthGlow(g, 18, 24, HealthBar.PLAYER, ticks);
            }
            HealthBar.PLAYER.drawWithNumbers(g, heroShown, duel.heroHp, BattleScreen.HERO_MAX_HP, 18, 24, scale, false);
            String name = duel.enemy.name();
            shadow(g, name, W - 18 - g.getFontMetrics().stringWidth(name), 20, PALE);
            HealthBar.MONSTER.drawWithNumbers(g, foeShown, duel.foeHp, duel.enemy.maxHp(),
                    W - 18 - HealthBar.MONSTER.width(scale), 24, scale, true);
            // The place, on a sign, like the word battle's door signs
            g.setPaint(new GradientPaint(0, 9, new Color(150, 128, 82), 0, 49, new Color(92, 76, 44)));
            g.fillRoundRect(306, 9, 188, 40, 6, 6);
            g.setColor(new Color(40, 32, 18));
            g.drawRoundRect(306, 9, 188, 40, 6, 6);
            g.setFont(new Font(SERIF, Font.BOLD, 21));
            String room = duel.enemy.room();
            g.setColor(new Color(34, 26, 14));
            g.drawString(room, 400 - g.getFontMetrics().stringWidth(room) / 2, 36);
        }

        // The word to type (typed letters green, the next one white and underlined), its timer, and
        // what's waiting to take effect
        void drawWordBoard(Graphics2D g) {
            int bx = 110, by = 384, bw = 580, bh = 140;
            g.setColor(new Color(12, 14, 12));
            g.fillRect(0, STAGE_BOTTOM, W, H - STAGE_BOTTOM);
            g.setColor(new Color(26, 30, 26));
            g.fillRoundRect(bx, by, bw, bh, 16, 16);
            g.setColor(new Color(64, 76, 60));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(bx, by, bw, bh, 16, 16);
            g.setStroke(new BasicStroke(1));

            if (duel.state == Duel.State.READY) {
                int left = duel.readyCountdown();
                String text = "TYPE!";                 // "3", "2", "1", then "TYPE!"
                Color color = TYPED;
                if (left > 45) {
                    text = String.valueOf((left - 46) / 35 + 1);
                    color = PALE;
                }
                g.setFont(new Font(MONO, Font.BOLD, 54));
                shadow(g, text, 400 - g.getFontMetrics().stringWidth(text) / 2, by + 78, color);
                g.setFont(new Font(SERIF, Font.ITALIC, 15));
                String hint = "Type each word before its time runs out.";
                shadow(g, hint, 400 - g.getFontMetrics().stringWidth(hint) / 2, by + 118, PALE);
                return;
            }
            String word = duel.word;
            if (word.isEmpty()) return;
            g.setFont(new Font(MONO, Font.BOLD, 46));
            FontMetrics fm = g.getFontMetrics();
            int x = 400 - fm.stringWidth(word) / 2;
            if (wordShake > 0) {                      // A wrong key: the word shakes
                if ((wordShake / 2) % 2 == 0) {
                    x += 8;
                } else {
                    x -= 8;
                }
            }
            int y = by + 66;
            boolean typing = duel.state == Duel.State.TYPING;
            for (int i = 0; i < word.length(); i++) {
                String ch = String.valueOf(word.charAt(i));
                Color c = PALE;                        // Still to type
                if (i == duel.typed) c = NEXT;         // The next letter
                if (i < duel.typed || !typing) c = TYPED;   // Typed already
                if (wordShake > 0 && i == duel.typed) c = new Color(240, 80, 70);
                shadow(g, ch, x, y, c);
                if (typing && i == duel.typed) g.fillRect(x, y + 6, fm.charWidth('m') - 4, 3);
                x += fm.charWidth('m');
            }
            // The timer: green, then amber, then red and pulsing
            double left = 0;                           // How much of the word's time is left: 1 = all of it
            if (typing && duel.timeMax > 0) left = (double) duel.timeLeft / duel.timeMax;
            Color tc = new Color(210, 50, 40);
            if (left > 0.25) tc = new Color(220, 170, 60);
            if (left > 0.5) tc = new Color(90, 190, 90);
            int tw = 460, tx = 400 - tw / 2, ty = by + 90;
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(tx - 2, ty - 2, tw + 4, 14);
            g.setColor(tc);
            g.fillRect(tx, ty, (int) Math.round(tw * left), 10);
            // Streak and the items waiting to take effect
            g.setFont(new Font(MONO, Font.BOLD, 14));
            List<String> notes = new ArrayList<>();
            if (duel.streak > 1) notes.add("STREAK x" + duel.streak + " (+" + (int) Math.round(100 * STREAK_BONUS * Math.min(duel.streak, STREAK_MAX)) + "%)");
            if (duel.redPenReady) notes.add("RED PEN READY: x2");
            if (duel.textbookReady) notes.add("TEXTBOOK UP: next hit halved");
            String line = String.join("   ", notes);
            shadow(g, line, 400 - g.getFontMetrics().stringWidth(line) / 2, by + 126, new Color(230, 200, 110));
        }

        // What the player carries, and the number key for each
        void drawItems(Graphics2D g) {
            String[] names = {"First Aid Kit", "Old Textbook", "Red Pen", "Hall Pass"};
            int[] counts = {duel.kits, duel.items.textbooks, duel.items.redPens, duel.items.hallPasses};
            int w = 180, gap = 10, x0 = (W - (4 * w + 3 * gap)) / 2, y = 538;
            for (int i = 0; i < 4; i++) {
                int x = x0 + i * (w + gap);
                // Greyed out when there are none left
                Color fill = new Color(24, 24, 24), edge = new Color(50, 50, 50);
                Color keyColor = new Color(90, 90, 90), textColor = new Color(100, 100, 100);
                if (counts[i] > 0) {
                    fill = new Color(40, 44, 38);
                    edge = new Color(110, 120, 96);
                    keyColor = new Color(230, 200, 110);
                    textColor = PALE;
                }
                g.setColor(fill);
                g.fillRoundRect(x, y, w, 46, 10, 10);
                g.setColor(edge);
                g.drawRoundRect(x, y, w, 46, 10, 10);
                g.setFont(new Font(MONO, Font.BOLD, 20));
                shadow(g, String.valueOf(i + 1), x + 12, y + 30, keyColor);
                g.setFont(new Font(MONO, Font.BOLD, 13));
                shadow(g, names[i], x + 36, y + 20, textColor);
                shadow(g, "x" + counts[i], x + 36, y + 37, textColor);
            }
        }

        void drawFloats(Graphics2D g) {
            g.setFont(new Font(MONO, Font.BOLD, 24));
            Color[] colours = {new Color(240, 90, 70), new Color(110, 220, 110), new Color(240, 210, 110)};
            for (int i = 0; i < floats.size(); i++) {
                double[] f = floats.get(i);
                String t = floatText.get(i);
                Color c = colours[(int) f[3]];
                int alpha = (int) Math.min(255, f[2] * 8);
                shadow(g, t, (int) f[0] - g.getFontMetrics().stringWidth(t) / 2, (int) f[1],
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            }
        }

        void drawBanner(Graphics2D g) {
            if (bannerLife <= 0 || banner.isEmpty()) return;
            g.setFont(new Font(SERIF, Font.ITALIC, 17));
            int w = g.getFontMetrics().stringWidth(banner) + 30;
            int alpha = Math.min(220, bannerLife * 8);
            g.setColor(new Color(0, 0, 0, alpha));
            g.fillRoundRect(400 - w / 2, TOP_H + 10, w, 30, 10, 10);
            g.setColor(new Color(PALE.getRed(), PALE.getGreen(), PALE.getBlue(), Math.min(255, bannerLife * 10)));
            g.drawString(banner, 400 - w / 2 + 15, TOP_H + 31);
        }

        void drawWon(Graphics2D g) {
            if (foeFade < 40) return;                        // Let it fade first
            overlay(g, 220, 220, 360, 180, "IT FALLS STILL", INK);
            subtitle(g, "The " + duel.enemy.name() + " is just a tree again... for now.", 296);
            button(g, CONTINUE_BTN, "CONTINUE  [ENTER]", true);
        }

        void drawLost(Graphics2D g) {
            overlay(g, 180, 200, 440, 230, "YOU DIDN'T MAKE IT", BLOOD);
            subtitle(g, "The roots dragged you down into the dark...", 284);
            button(g, RETRY_BTN, "TRY AGAIN  [ENTER]", true);
            button(g, GIVEUP_BTN, "GIVE UP  [ESC]", false);
        }

        void overlay(Graphics2D g, int x, int y, int w, int h, String title, Color titleColor) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, W, H);
            g.setPaint(new GradientPaint(0, y, new Color(206, 194, 160), 0, y + h, new Color(156, 142, 108)));
            g.fillRect(x, y, w, h);
            g.setColor(new Color(30, 22, 16));
            g.setStroke(new BasicStroke(3));
            g.drawRect(x, y, w, h);
            g.setStroke(new BasicStroke(1));
            g.setFont(new Font(SERIF, Font.BOLD, 30));
            g.setColor(titleColor);
            g.drawString(title, 400 - g.getFontMetrics().stringWidth(title) / 2, y + 46);
        }

        void subtitle(Graphics2D g, String s, int y) {
            g.setFont(new Font(SERIF, Font.ITALIC, 15));
            g.setColor(INK);
            g.drawString(s, 400 - g.getFontMetrics().stringWidth(s) / 2, y);
        }

        void button(Graphics2D g, Rectangle r, String text, boolean red) {
            boolean over = r.contains(mouse);
            if (red && over) g.setColor(new Color(190, 34, 30));      // Red, lighter under the mouse...
            if (red && !over) g.setColor(new Color(160, 24, 22));
            if (!red && over) g.setColor(new Color(96, 98, 96));      // ...or grey
            if (!red && !over) g.setColor(new Color(78, 80, 78));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g.setColor(new Color(20, 14, 12));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g.setStroke(new BasicStroke(1));
            g.setFont(new Font(MONO, Font.BOLD, 15));
            if (red) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(PALE);
            }
            g.drawString(text, r.x + r.width / 2 - g.getFontMetrics().stringWidth(text) / 2, r.y + r.height / 2 + 5);
        }

        static void shadow(Graphics2D g, String s, int x, int y, Color c) {
            g.setColor(new Color(0, 0, 0, Math.min(190, c.getAlpha())));
            g.drawString(s, x + 1, y + 2);
            g.setColor(c);
            g.drawString(s, x, y);
        }
    }
}
