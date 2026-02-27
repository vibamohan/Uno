
import javax.swing.*;

public class Runner {

    public static void main(String[] args) {
        GameSetup setup = new GameSetup();
        setup.showGameModeSelection();

        if (!setup.isReady()) {
            System.exit(0);
        }

        GameMode mode = setup.getSelectedMode();
        CardGame game;
        final NetworkManager[] networkManager = new NetworkManager[1];
        networkManager[0] = null;
        boolean isPlayer1 = true;

        if (mode == GameMode.LOCAL_VS_AI) {
            // Local game vs AI
            game = new CardGame(PlayerType.HUMAN_LOCAL, PlayerType.AI);
            game.setPlayerNames("You", "AI");
        } else if (mode == GameMode.NETWORK_SERVER) {
            // Host a network game
            game = new CardGame(PlayerType.HUMAN_LOCAL, PlayerType.HUMAN_NETWORK);
            game.setPlayerNames(setup.getPlayerName(), "Opponent");

            networkManager[0] = new NetworkManager();
            try {
                networkManager[0].startServer(setup.getPort());
                game.setNetworkManager(networkManager[0]);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Failed to start server: " + e.getMessage());
                System.exit(1);
            }
            isPlayer1 = true;
        } else {
            // Join a network game
            game = new CardGame(PlayerType.HUMAN_NETWORK, PlayerType.HUMAN_LOCAL);
            game.setPlayerNames("Opponent", setup.getPlayerName());

            networkManager[0] = new NetworkManager();
            try {
                networkManager[0].connectToServer(setup.getHostAddress(), setup.getPort());
                game.setNetworkManager(networkManager[0]);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Failed to connect to server: " + e.getMessage());
                System.exit(1);
            }
            isPlayer1 = false;
        }

        // Create and display GUI
        JFrame frame = new JFrame("Uno Card Game - " + setup.getPlayerName());
        Table table = new Table(game, mode, networkManager[0], isPlayer1);
        frame.add(table);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (networkManager[0] != null) {
                    networkManager[0].close();
                }
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }
}

