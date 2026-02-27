
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class CardGame {

    private ArrayList<Card> deck, discardPile;
    private ArrayList<Card> playerHand, aiHand;
    private Card topCard;
    private int currentPlayer; // 1 = human, 2 = AI
    private boolean gameOver;

    public CardGame() {
        deck = new ArrayList<>();
        discardPile = new ArrayList<>();
        playerHand = new ArrayList<>();
        aiHand = new ArrayList<>();
        currentPlayer = 1;
        gameOver = false;
        initDeck();
        shuffleDeck();
        dealCards();
        initTopCard();
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
            playerHand.add(deck.remove(deck.size() - 1));
            aiHand.add(deck.remove(deck.size() - 1));
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
        return playerHand;
    }

    public ArrayList<Card> getAIHand() {
        return aiHand;
    }

    public Card getTopCard() {
        return topCard;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void playCard(Card card, String chosenColor, boolean isHuman) {
        ArrayList<Card> hand = isHuman ? playerHand : aiHand;
        if (!card.isPlayableOn(topCard)) {
            return;
        }

        hand.remove(card);
        discardPile.add(topCard);

        topCard = card;

        // Handle special cards
        if (card.getType().equals("Skip") || card.getType().equals("Reverse")) {
            // 2-player: Skip/Reverse = skip opponent turn
            if (isHuman) {
                aiTurn();
            }
        } else if (card.getType().equals("Draw Two")) {
            ArrayList<Card> target = isHuman ? aiHand : playerHand;
            target.add(drawFromDeck());
            target.add(drawFromDeck());
        } else if (card.getType().equals("Wild")) {
            topCard = new Card(chosenColor, "Wild");
        } else if (card.getType().equals("Wild Draw Four")) {
            topCard = new Card(chosenColor, "Wild Draw Four");
            ArrayList<Card> target = isHuman ? aiHand : playerHand;
            for (int i = 0; i < 4; i++) {
                target.add(drawFromDeck());
            }
        }

        checkGameOver();

        if (!gameOver) {
            if (isHuman) {
                aiTurn();
            }
        }
    }

    public void playerDrawCard() {
        playerHand.add(drawFromDeck());
        aiTurn();
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
        if (playerHand.isEmpty() || aiHand.isEmpty()) {
            gameOver = true;
        }
    }

    private void aiTurn() {
        if (gameOver) {
            return;
        }
        currentPlayer = 2;

        Card toPlay = null;
        String chosenColor = null;
        for (Card c : aiHand) {
            if (c.isPlayableOn(topCard)) {
                toPlay = c;
                break;
            }
        }

        if (toPlay == null) {
            aiHand.add(drawFromDeck());
        } else {
            if (toPlay.getColor().equals("Wild")) {
                int[] colorCount = new int[4];
                for (Card c : aiHand) {
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
            playCard(toPlay, chosenColor, false);
        }
        currentPlayer = 1;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean canPressUNO() {
        return playerHand.size() == 1;
    }

    public void pressUNO(boolean humanPressed) {
        if (humanPressed) {
            if (playerHand.size() == 1) {
                // Correct UNO
                // Could show a message in GUI
            } else {
                // Penalize human: draw 2 cards
                playerHand.add(drawFromDeck());
                playerHand.add(drawFromDeck());
            }
        }
    }
}
