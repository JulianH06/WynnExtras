package julianh06.wynnextras.features.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IngredientPositionModifiers {
    private enum Position { LEFT, RIGHT, ABOVE, UNDER, TOUCHING, NOT_TOUCHING }

    public int left;
    public int right;
    public int above;
    public int under;
    public int touching;
    public int not_touching;

    public IngredientPositionModifiers(Map<?, Integer> modifierMap) {
        for (Map.Entry<?, Integer> entry : modifierMap.entrySet()) {
            if (!(entry.getKey() instanceof Enum<?> key) || entry.getValue() == null) continue;
            switch (key.name()) {
                case "LEFT" -> left = entry.getValue();
                case "RIGHT" -> right = entry.getValue();
                case "ABOVE" -> above = entry.getValue();
                case "UNDER" -> under = entry.getValue();
                case "TOUCHING" -> touching = entry.getValue();
                case "NOT_TOUCHING" -> not_touching = entry.getValue();
            }
        }
    }

    public boolean hasAny() {
        return left != 0 || right != 0 || above != 0 || under != 0 || touching != 0 || not_touching != 0;
    }

    public Double[] getMultipliers(int index) {
        return getMultipliers(index, 2, 6);
    }

    private Double[] getMultipliers(int index, int width, int length) {
        Double[] multipliers = new Double[length];
        Arrays.fill(multipliers, 0.0);

        if (!this.hasAny()) return multipliers;

        // Apply each modifier to the relevant indexes
        if (this.left != 0) {
            for (int i : getAdjacentIndexes(index, width, length, Position.LEFT)) {
                multipliers[i] += this.left / 100.0;
            }
        }

        if (this.right != 0) {
            for (int i : getAdjacentIndexes(index, width, length, Position.RIGHT)) {
                multipliers[i] += this.right / 100.0;
            }
        }

        if (this.above != 0) {
            for (int i : getAdjacentIndexes(index, width, length, Position.ABOVE)) {
                multipliers[i] += this.above / 100.0;
            }
        }

        if (this.under != 0) {
            for (int i : getAdjacentIndexes(index, width, length, Position.UNDER)) {
                multipliers[i] += this.under / 100.0;
            }
        }

        if (this.touching != 0) {
            for (int i : getAdjacentIndexes(index, width, length, Position.TOUCHING)) {
                multipliers[i] += this.touching / 100.0;
            }
        }

        if (this.not_touching != 0) {
            for (int i : getAdjacentIndexes(index, width, length, Position.NOT_TOUCHING)) {
                multipliers[i] += this.not_touching / 100.0;
            }
        }

        return multipliers;
    }

    private static List<Integer> getAdjacentIndexes(int index, Position type) {
        return getAdjacentIndexes(index, 2, 6, type);
    }

    private static List<Integer> getAdjacentIndexes(int index, int width, int length, Position type) {
        List<Integer> result = new ArrayList<>();

        int height = (int) Math.ceil((double) length / width);
        int row = index / width;
        int col = index % width;

        switch (type) {
            case LEFT:
                for (int c = col - 1; c >= 0; c--) {
                    result.add(row * width + c);
                }
                break;

            case RIGHT:
                for (int c = col + 1; c < width; c++) {
                    int idx = row * width + c;
                    if (idx < length) result.add(idx);
                }
                break;

            case ABOVE:
                for (int r = row - 1; r >= 0; r--) {
                    int idx = r * width + col;
                    result.add(idx);
                }
                break;

            case UNDER:
                for (int r = row + 1; r < height; r++) {
                    int idx = r * width + col;
                    if (idx < length) result.add(idx);
                }
                break;
            case TOUCHING:
                // Left
                if (col > 0) result.add(index - 1);
                // Right
                if (col < width - 1 && index + 1 < length) result.add(index + 1);
                // Above
                if (row > 0) result.add(index - width);
                // Below
                if (row < height - 1 && index + width < length) result.add(index + width);
                break;
            case NOT_TOUCHING:
                for (int i = 0; i < length; i++) {
                    if (i == index) continue;
                    if (!getAdjacentIndexes(index, width, length, Position.TOUCHING).contains(i)) {
                        result.add(i);
                    }
                }
                break;
        }

        return result;
    }
}
