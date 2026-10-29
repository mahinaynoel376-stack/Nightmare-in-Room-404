package Nightmareon404;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

// Debug tool: renders the 4-step DOWN walk cycle through Player.update() and Player.draw(),
// exactly as the game draws it, into one PNG strip, and checks the head is the same in every step.
// Run: java -Djava.awt.headless=true -cp bin Nightmareon404.WalkStripDebug [output.png]
public class WalkStripDebug {
    private static final int STEPS = 4;
    private static final int HEAD_ROWS = 24; // Top of the sprite that holds the head and hair

    public static void main(String[] args) throws IOException {
        String outPath = args.length > 0 ? args[0] : "down_walk_strip.png";
        GamePanel gp = new GamePanel();
        Player player = gp.flor;
        int ts = gp.tileSize;
        int top = 0;                   // Space above the sprite (the health bar used to be drawn here)
        int frameH = top + ts * 2;     // Sprite is 1 tile wide, 2 tiles tall

        BufferedImage strip = new BufferedImage(ts * STEPS, frameH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = strip.createGraphics();

        gp.downPressed = true;
        for (int step = 0; step < STEPS; step++) {
            // One walk step lasts animationSpeed ticks; draw the frame shown on that step's first tick
            player.update();
            Graphics2D frame = (Graphics2D) g2d.create();
            // Draw relative to the player so movement and walls don't shift the frame
            frame.translate(step * ts - player.x, top - (player.y - ts));
            frame.setClip(player.x, player.y - ts - top, ts, frameH);
            player.draw(frame);
            frame.dispose();
            for (int t = 1; t < 8; t++) player.update(); // Rest of this step (animationSpeed = 8)
        }
        g2d.dispose();
        ImageIO.write(strip, "png", new File(outPath));
        System.out.println("Wrote " + new File(outPath).getAbsolutePath());

        // Compare each step's head with step 0's
        boolean allSame = true;
        for (int step = 1; step < STEPS; step++) {
            int diff = 0;
            for (int y = top; y < top + HEAD_ROWS; y++) {
                for (int x = 0; x < ts; x++) {
                    if (strip.getRGB(x, y) != strip.getRGB(step * ts + x, y)) diff++;
                }
            }
            System.out.println("Step " + step + " head pixels differing from step 0: " + diff);
            if (diff > 0) allSame = false;
        }
        System.out.println(allSame ? "PASS: head is identical in every step" : "FAIL: head changes between steps");
    }
}
