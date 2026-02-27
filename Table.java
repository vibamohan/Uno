
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.*;
import java.io.File;

public class Table extends JPanel implements MouseListener {

    private CardGame game;
    private JButton drawButton, unoButton;
    private GameMode gameMode;
    private NetworkManager networkManager;
    private boolean isPlayer1;
    private Thread networkListenerThread;

    public Table(CardGame game, GameMode mode, NetworkManager nm, boolean isPlayer1) {
        setLayout(null);
        this.game = game;
        this.gameMode = mode;
        this.networkManager = nm;
        this.isPlayer1 = isPlayer1;

        drawButton = new JButton("Draw");
        drawButton.setBounds(600, 500, 100, 50);
        drawButton.addActionListener(e -> {
            if (canCurrentPlayerMove()) {
                game.playerDrawCard(isPlayer1 ? 1 : 2);
                if (gameMode != GameMode.LOCAL_VS_AI && networkManager != null && networkManager.isConnected()) {
                    try {
                        networkManager.sendDrawCard();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                playSound("draw");
                repaint();
                startNetworkListener();
            }
        });
        add(drawButton);

        unoButton = new JButton("UNO!");
        unoButton.setBounds(480, 500, 100, 50);
        unoButton.addActionListener(e -> {
            game.pressUNO(isPlayer1 ? 1 : 2);
            repaint();
        });
        add(unoButton);

        addMouseListener(this);

        // Start network listener if in network mode
        if (gameMode != GameMode.LOCAL_VS_AI && networkManager != null && networkManager.isConnected()) {
            startNetworkListener();
        }
    }

    private boolean canCurrentPlayerMove() {
        int currentPlayer = game.getCurrentPlayer();
        if (gameMode == GameMode.LOCAL_VS_AI) {
            return currentPlayer == 1;
        } else {
            // Network mode - check if it's this player's turn
            return (isPlayer1 && currentPlayer == 1) || (!isPlayer1 && currentPlayer == 2);
        }
    }

    private void startNetworkListener() {
        if (networkListenerThread != null && networkListenerThread.isAlive()) {
            return;
        }

        networkListenerThread = new Thread(() -> {
            try {
                while (!game.isGameOver() && networkManager != null && networkManager.isConnected()) {
                    try {
                        NetworkManager.GameMove move = networkManager.receiveMove();
                        if (move != null) {
                            handleNetworkMove(move);
                            SwingUtilities.invokeLater(() -> repaint());
                        }
                    } catch (EOFException e) {
                        // Connection closed normally
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Network protocol error: " + e.getClass().getSimpleName());
                        break;
                    }
                }
            } catch (IOException e) {
                // Connection lost or closed
                System.out.println("Network disconnected.");
            }
        });
        networkListenerThread.setDaemon(true);
        networkListenerThread.start();
    }

    private void handleNetworkMove(NetworkManager.GameMove move) {
        int otherPlayer = isPlayer1 ? 2 : 1;

        if (move.cardColor.equals("DRAW")) {
            // Remote opponent drew a card. Apply without switching turn (already switched by sender)
            game.applyRemoteDraw(otherPlayer);
        } else {
            Card card = new Card(move.cardColor, move.cardType);
            ArrayList<Card> otherHand = isPlayer1 ? game.getPlayer2Hand() : game.getPlayer1Hand();
            if (otherHand.contains(card) || move.cardColor.equals("Wild")) {
                // Find and remove the card
                for (Card c : otherHand) {
                    if (c.getColor().equals(move.cardColor) && c.getType().equals(move.cardType)) {
                        // Use applyRemoteMove instead of playCard to avoid double turn-switch
                        game.applyRemoteMove(c, move.chosenColor, otherPlayer);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(new Color(50, 150, 50)); // green table

        if (game.isGameOver()) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.WHITE);
            ArrayList<Card> p1Hand = game.getPlayer1Hand();
            ArrayList<Card> p2Hand = game.getPlayer2Hand();
            if (p1Hand.isEmpty()) {
                g.drawString(game.getPlayer1Name() + " Wins!", 150, 250);
            } else {
                g.drawString(game.getPlayer2Name() + " Wins!", 150, 250);
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

        // Player hand (always at bottom, Player 1 if local, Player 1 if network server, Player 2 if network client)
        ArrayList<Card> myHand = isPlayer1 ? game.getPlayer1Hand() : game.getPlayer2Hand();
        for (int i = 0; i < myHand.size(); i++) {
            Card c = myHand.get(i);
            g.setColor(c.getColorForGraphics());
            g.fillRect(50 + i * 70, 400, 60, 90);
            g.setColor(Color.BLACK);
            g.drawRect(50 + i * 70, 400, 60, 90);
            g.drawString(c.getType(), 55 + i * 70, 450);
        }

        // Opponent hand (show number of cards)
        ArrayList<Card> opponentHand = isPlayer1 ? game.getPlayer2Hand() : game.getPlayer1Hand();
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String opponentName = isPlayer1 ? game.getPlayer2Name() : game.getPlayer1Name();
        g.drawString(opponentName + ": " + opponentHand.size() + " cards", 50, 50);

        // Show current turn
        g.setFont(new Font("Arial", Font.BOLD, 16));
        int currentPlayer = game.getCurrentPlayer();
        String currentName = (currentPlayer == 1) ? game.getPlayer1Name() : game.getPlayer2Name();
        g.drawString("Turn: " + currentName, 400, 250);

        // Show if it's your turn
        if (gameMode != GameMode.LOCAL_VS_AI && !canCurrentPlayerMove()) {
            g.setColor(new Color(255, 200, 0));
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Waiting for opponent...", 200, 150);
        }
    }

    private void playSound(String type) {
        try {
            String file = type.equals("draw") ? "draw.wav" : "play.wav";
            File soundFile = new File(file);
            if (!soundFile.exists()) {
                return; // Silently skip if sound file doesn't exist
            }
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(soundFile));
            clip.start();
        } catch (Exception e) {
            // Silently ignore sound errors - they're not critical
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (game.isGameOver() || !canCurrentPlayerMove()) {
            return;
        }

        ArrayList<Card> hand = isPlayer1 ? game.getPlayer1Hand() : game.getPlayer2Hand();
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
                game.playCard(clicked, chosenColor, isPlayer1 ? 1 : 2);

                playSound("play");
                repaint();

                // Send move through network if applicable
                if (gameMode != GameMode.LOCAL_VS_AI && networkManager != null && networkManager.isConnected()) {
                    try {
                        networkManager.sendMove(clicked, chosenColor);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    // Start listening for opponent's move
                    startNetworkListener();
                } else if (gameMode == GameMode.LOCAL_VS_AI && game.getPlayer2Type() == PlayerType.AI) {
                    // Trigger AI turn for local game
                    Thread aiThread = new Thread(() -> {
                        try {
                            Thread.sleep(500); // Small delay for better UX
                            if (!game.isGameOver() && game.getCurrentPlayer() == 2) {
                                game.performAITurn();
                                SwingUtilities.invokeLater(() -> repaint());
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    aiThread.setDaemon(true);
                    aiThread.start();
                }
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
