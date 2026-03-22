package julianh06.wynnextras.features.tetris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TetrominoGenerator {
    public List<Character> bag = new ArrayList<>();

    public TetrominoGenerator(int count) {
        for (int i = 0; i < count; i++) refillBag();
    }

    private void refillBag() {
        List<Character> chars = new ArrayList<>();
        for (char c : "OISZLJT".toCharArray()) chars.add(c);
        Collections.shuffle(chars);
        bag.addAll(chars);
    }

    public char nextTetromino() {
        if (bag.size() <= 7) refillBag();
        return bag.remove(0);
    }
}
