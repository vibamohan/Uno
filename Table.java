
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.sound.sampled.*;
import java.io.File;

public class Table extends JPanel implements MouseListener {

    private CardGame game;
    private JButton drawButton, unoButton;

    public Table() {
        setLayout(null);
        game = new CardGame();

        drawButton = new JButton("Draw");
        drawButton.setBounds(600, 500, 100, 50);
        drawButton.addActionListener(e -> {
            game.playerDrawCard();
            playSound("draw");
            repaint();
        });
        add(drawButton);

        unoButton = new JButton("UNO!");
        unoButton.setBounds(480, 500, 100, 50);
        unoButton.addActionListener(e -> {
            game.pressUNO(true);
            repaint();
        });
        add(unoButton);

        addMouseListener(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(new Color(50, 150, 50)); // green table

        if (game.isGameOver()) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.WHITE);
            if (game.getPlayerHand().isEmpty()) {
                g.drawString("You Win!", 250, 250); 
            }else {
                g.drawString("AI Wins!", 250, 250);
            }
            return;
        }

        // Top card
        Card top = game.getTopCard();
        g.setColor(top.getColorForGraphics());
        g.fillRect(350, 50, 80, 120);
        g.setColor(Color.BLACK);
        g.drawRect(350, 50, 80, 120);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(top.getType(), 365, 110);
        g.drawString(top.getColor(), 365, 130);

        // Player hand
        ArrayList<Card> hand = game.getPlayerHand();
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            g.setColor(c.getColorForGraphics());
            g.fillRect(50 + i * 70, 400, 60, 90);
            g.setColor(Color.BLACK);
            g.drawRect(50 + i * 70, 400, 60, 90);
            g.drawString(c.getType(), 55 + i * 70, 450);
        }

        // AI hand (show number of cards)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("AI Cards: " + game.getAIHand().size(), 50, 50);
    }

    private void playSound(String type) {
        try {
            String file = type.equals("draw") ? "draw.wav" : "play.wav";
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(new File(file)));
            clip.start();
        } catch (Exception e) {
            System.out.println("Sound error: " + e.getMessage());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (game.isGameOver() || game.getCurrentPlayer() != 1) {
            return;
        }

        ArrayList<Card> hand = game.getPlayerHand();
        for (int i = 0; i < hand.size(); i++) {
            int x = 50 + i * 70, y = 400, w = 60, h = 90;
            if (e.getX() >= x && e.getX() <= x + w && e.getY() >= y && e.getY() <= y + h) {
                Card clicked = hand.get(i);
                String chosenColor = clicked.getColor();
                if (chosenColor.equals("Wild")) {
                    String[] options = {"Red", "Yellow", "Green", "Blue"};
                    chosenColor = (String) JOptionPane.showInputDialog(this, "Choose color", "Wild Card",
                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (chosenColor == null) {
                        return;
                    }
                }
                game.playCard(clicked, chosenColor, true);
                playSound("play");
                repaint();
                return;
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
