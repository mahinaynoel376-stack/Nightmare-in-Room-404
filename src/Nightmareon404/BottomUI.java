package Nightmareon404;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

// TEMPORARY bottom UI bar. It fills the last tile row of the screen (row 18, y 576-607):
// every map is 18 rows tall, so this row is never covered by the world.
// For now it only shows the player's health; the real UI design will replace it.
public class BottomUI {
    private static final Color BACKGROUND = new Color(20, 22, 30);
    private static final Color BORDER = new Color(70, 74, 90);
    private static final Color BAR_EMPTY = new Color(90, 20, 20);
    private static final Color BAR_FULL = new Color(60, 180, 75);
    private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 14);

    private static final int BAR_X = 48;         // Left edge of the health bar
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 12;

    GamePanel gp;

    public BottomUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2d) {
        int top = gp.screenHeight - gp.tileSize;   // 608 - 32 = 576
        int height = gp.tileSize;

        // Background, with a 1 px line separating it from the world
        g2d.setColor(BACKGROUND);
        g2d.fillRect(0, top, gp.screenWidth, height);
        g2d.setColor(BORDER);
        g2d.drawLine(0, top, gp.screenWidth, top);

        // "HP" label, then the bar, vertically centred in the 32 px row
        g2d.setFont(LABEL_FONT);
        g2d.setColor(Color.WHITE);
        int textY = top + height / 2 + 5;
        g2d.drawString("HP", 16, textY);

        Player player = gp.flor;
        int barY = top + (height - BAR_HEIGHT) / 2;
        int filled = (int) (BAR_WIDTH * ((double) player.health / player.maxHealth));
        g2d.setColor(BAR_EMPTY);
        g2d.fillRect(BAR_X, barY, BAR_WIDTH, BAR_HEIGHT);
        g2d.setColor(BAR_FULL);
        g2d.fillRect(BAR_X, barY, filled, BAR_HEIGHT);
        g2d.setColor(BORDER);
        g2d.drawRect(BAR_X, barY, BAR_WIDTH, BAR_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.drawString(player.health + " / " + player.maxHealth, BAR_X + BAR_WIDTH + 10, textY);
    }
}
