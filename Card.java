
import java.awt.Color;

public class Card {

    private String color;
    private String type;

    public Card(String color, String type) {
        this.color = color;
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public String getType() {
        return type;
    }

    public boolean isPlayableOn(Card topCard) {
        return this.color.equals(topCard.getColor()) || this.type.equals(topCard.getType()) || this.color.equals("Wild");
    }

    public Color getColorForGraphics() {
        switch (color) {
            case "Red":
                return Color.RED;
            case "Yellow":
                return Color.YELLOW;
            case "Green":
                return Color.GREEN;
            case "Blue":
                return Color.BLUE;
            default:
                return Color.BLACK;
        }
    }

    @Override
    public String toString() {
        return color.equals("Wild") ? type : color + " " + type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Card card = (Card) obj;
        return color.equals(card.color) && type.equals(card.type);
    }

    @Override
    public int hashCode() {
        return color.hashCode() * 31 + type.hashCode();
    }
}
