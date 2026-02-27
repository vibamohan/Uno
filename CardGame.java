
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class CardGame {

    private ArrayList<Card> deck, discardPile;
    private ArrayList<Card> player1Hand, player2Hand;
    private Card topCard;
    private int currentPlayer; // 1 = player 1, 2 = player 2
    private boolean gameOver;
    private PlayerType player1Type;
    private PlayerType player2Type;
    private String player1Name = "Player 1";
    private String player2Name = "Player 2";
    private NetworkManager networkManager;

    public CardGame(PlayerType p1Type, PlayerType p2Type) {
        deck = new ArrayList<>();
        discardPile = new ArrayList<>();
        player1Hand = new ArrayList<>();
        player2Hand = new ArrayList<>();
        currentPlayer = 1;
        gameOver = false;
        player1Type = p1Type;
        player2Type = p2Type;
        initDeck();
        shuffleDeck();
        dealCards();
        initTopCard();
    }

    public void setPlayerNames(String p1Name, String p2Name) {
        this.player1Name = p1Name;
        this.player2Name = p2Name;
    }

    public void setNetworkManager(NetworkManager nm) {
        this.networkManager = nm;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public PlayerType getPlayer1Type() {
        return player1Type;
    }

    public PlayerType getPlayer2Type() {
        return player2Type;
    }

    private void initDeck() {
        String[] colors = {"Red", "Yellow", "Green", "Blue"};
        String[] types = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip", "Reverse", "Draw Two"};

        for (String color : colors) {
            for (String type : types) {
                deck.add(new Card(color, type));
                if (!type.equals("0")) {
                    deck.add(new Card(color, type));
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            deck.add(new Card("Wild", "Wild"));
            deck.add(new Card("Wild", "Wild Draw Four"));
        }
    }

    private void shuffleDeck() {
        Collections.shuffle(deck, new Random());
    }

    private void dealCards() {
        for (int i = 0; i < 7; i++) {
            player1Hand.add(deck.remove(deck.size() - 1));
            player2Hand.add(deck.remove(deck.size() - 1));
        }
    }

    private void initTopCard() {
        topCard = deck.remove(deck.size() - 1);
        while (topCard.getColor().equals("Wild")) { // avoid starting with wild
            deck.add(topCard);
            shuffleDeck();
            topCard = deck.remove(deck.size() - 1);
        }
    }

    public ArrayList<Card> getPlayerHand() {
        return player1Hand;
    }

    public ArrayList<Card> getAIHand() {
        return player2Hand;
    }

    public ArrayList<Card> getPlayer1Hand() {
        return player1Hand;
    }

    public ArrayList<Card> getPlayer2Hand() {
        return player2Hand;
    }

    public Card getTopCard() {
        return topCard;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void playCard(Card card, String chosenColor, int playerNum) {
        ArrayList<Card> hand = (playerNum == 1) ? player1Hand : player2Hand;
        if (!card.isPlayableOn(topCard)) {
            return;
        }

        hand.remove(card);
        discardPile.add(topCard);

        topCard = card;

        // Handle special cards
        if (card.getType().equals("Skip") || card.getType().equals("Reverse")) {
            // 2-player: Skip/Reverse = skip opponent turn
            switchTurn();
        } else if (card.getType().equals("Draw Two")) {
            ArrayList<Card> target = (playerNum == 1) ? player2Hand : player1Hand;
            target.add(drawFromDeck());
            target.add(drawFromDeck());
        } else if (card.getType().equals("Wild")) {
            topCard = new Card(chosenColor, "Wild");
        } else if (card.getType().equals("Wild Draw Four")) {
            topCard = new Card(chosenColor, "Wild Draw Four");
            ArrayList<Card> target = (playerNum == 1) ? player2Hand : player1Hand;
            for (int i = 0; i < 4; i++) {
                target.add(drawFromDeck());
            }
        }

        checkGameOver();

        if (!gameOver) {
            switchTurn();
        }
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }

    public void applyRemoteMove(Card card, String chosenColor, int playerNum) {
        // Apply a move received from the network WITHOUT switching turns
        // (the turn was already switched by the sender)
        ArrayList<Card> hand = (playerNum == 1) ? player1Hand : player2Hand;
        if (!hand.contains(card)) {
            return;
        }

        hand.remove(card);
        discardPile.add(topCard);
        topCard = card;

        // Handle special cards
        if (card.getType().equals("Skip") || card.getType().equals("Reverse")) {
            // Turn already switched by sender, no additional action needed
        } else if (card.getType().equals("Draw Two")) {
            ArrayList<Card> target = (playerNum == 1) ? player2Hand : player1Hand;
            target.add(drawFromDeck());
            target.add(drawFromDeck());
        } else if (card.getType().equals("Wild")) {
            topCard = new Card(chosenColor, "Wild");
        } else if (card.getType().equals("Wild Draw Four")) {
            topCard = new Card(chosenColor, "Wild Draw Four");
            ArrayList<Card> target = (playerNum == 1) ? player2Hand : player1Hand;
            for (int i = 0; i < 4; i++) {
                target.add(drawFromDeck());
            }
        }

        checkGameOver();
        // NOTE: Do NOT switch turn here - turn was already switched by the sender
    }

    public void playerDrawCard(int playerNum) {
        ArrayList<Card> hand = (playerNum == 1) ? player1Hand : player2Hand;
        hand.add(drawFromDeck());
        switchTurn();
    }

    public void applyRemoteDraw(int playerNum) {
        // Remote player drew a card. We add a card to their hand but don't switch turns
        // (turn was already switched when they called playerDrawCard)
        ArrayList<Card> hand = (playerNum == 1) ? player1Hand : player2Hand;
        hand.add(drawFromDeck());
    }

    private Card drawFromDeck() {
        if (deck.isEmpty()) {
            reshuffleDiscard();
        }
        if (deck.isEmpty()) {
            return null;
        }
        return deck.remove(deck.size() - 1);
    }

    private void reshuffleDiscard() {
        if (discardPile.isEmpty()) {
            return;
        }
        deck.addAll(discardPile);
        discardPile.clear();
        shuffleDeck();
    }

    private void checkGameOver() {
        if (player1Hand.isEmpty() || player2Hand.isEmpty()) {
            gameOver = true;
        }
    }

    public void performAITurn() {
        if (gameOver) {
            return;
        }

        Card toPlay = null;
        String chosenColor = null;
        ArrayList<Card> hand = (currentPlayer == 1) ? player1Hand : player2Hand;

        for (Card c : hand) {
            if (c.isPlayableOn(topCard)) {
                toPlay = c;
                break;
            }
        }

        if (toPlay == null) {
            hand.add(drawFromDeck());
            switchTurn();
        } else {
            if (toPlay.getColor().equals("Wild")) {
                int[] colorCount = new int[4];
                for (Card c : hand) {
                    switch (c.getColor()) {
                        case "Red":
                            colorCount[0]++;
                            break;
                        case "Yellow":
                            colorCount[1]++;
                            break;
                        case "Green":
                            colorCount[2]++;
                            break;
                        case "Blue":
                            colorCount[3]++;
                            break;
                    }
                }
                int maxIndex = 0;
                for (int i = 1; i < 4; i++) {
                    if (colorCount[i] > colorCount[maxIndex]) {
                        maxIndex = i;
                    }
                }
                String[] colors = {"Red", "Yellow", "Green", "Blue"};
                chosenColor = colors[maxIndex];
            } else {
                chosenColor = toPlay.getColor();
            }
            playCard(toPlay, chosenColor, currentPlayer);
        }
    }

    private void aiTurn() {
        if (gameOver) {
            return;
        }

        Card toPlay = null;
        String chosenColor = null;
        for (Card c : player2Hand) {
            if (c.isPlayableOn(topCard)) {
                toPlay = c;
                break;
            }
        }

        if (toPlay == null) {
            player2Hand.add(drawFromDeck());
        } else {
            if (toPlay.getColor().equals("Wild")) {
                int[] colorCount = new int[4];
                for (Card c : player2Hand) {
                    switch (c.getColor()) {
                        case "Red":
                            colorCount[0]++;
                            break;
                        case "Yellow":
                            colorCount[1]++;
                            break;
                        case "Green":
                            colorCount[2]++;
                            break;
                        case "Blue":
                            colorCount[3]++;
                            break;
                    }
                }
                int maxIndex = 0;
                for (int i = 1; i < 4; i++) {
                    if (colorCount[i] > colorCount[maxIndex]) {
                        maxIndex = i;
                    }
                }
                String[] colors = {"Red", "Yellow", "Green", "Blue"};
                chosenColor = colors[maxIndex];
            } else {
                chosenColor = toPlay.getColor();
            }
            playCard(toPlay, chosenColor, 2);
        }
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean canPressUNO(int playerNum) {
        ArrayList<Card> hand = (playerNum == 1) ? player1Hand : player2Hand;
        return hand.size() == 1;
    }

    public void pressUNO(int playerNum) {
        ArrayList<Card> hand = (playerNum == 1) ? player1Hand : player2Hand;
        if (hand.size() == 1) {
            // Correct UNO
        } else {
            // Penalize: draw 2 cards
            hand.add(drawFromDeck());
            hand.add(drawFromDeck());
        }
    }
}
