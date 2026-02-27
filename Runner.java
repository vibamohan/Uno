
import javax.swing.*;

public class Runner {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Uno Card Game");
        Table table = new Table();
        frame.add(table);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
