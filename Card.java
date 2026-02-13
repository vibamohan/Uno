
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
}
