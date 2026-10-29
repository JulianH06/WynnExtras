package julianh06.wynnextras.features.buildplanner.data;

import java.util.ArrayList;
import java.util.List;

public final class CraftingPageLayout {
    private static final int GAP = 12;
    private static final int ROW_GAP = 6;

    private CraftingPageLayout() {}

    public static Columns columns(int width) {
        if (width < 280) throw new IllegalArgumentException("Crafting page is too narrow");
        int mode = width >= 700 ? 3 : width >= 520 ? 2 : 1;
        int result = mode == 3 ? (width - GAP) * 25 / 100 : Math.min(340, width);
        int left = mode == 3 ? width - result - GAP : width;
        int form = mode == 3 ? (width - GAP) * 36 / 100 : left;
        return new Columns(width, mode, left, form, result);
    }

    public static Page arrange(Columns columns, int controlsHeight, int resultHeight,
            List<Integer> ingredientHeights) {
        if (ingredientHeights.size() != 6 || ingredientHeights.stream().anyMatch(height -> height <= 0)
                || controlsHeight <= 0 || resultHeight <= 0) {
            throw new IllegalArgumentException("Invalid crafting card dimensions");
        }
        Rect controls = new Rect(0, 0, columns.form(), controlsHeight);
        Rect result = new Rect(columns.mode() == 3 ? columns.left() + GAP : (columns.width() - columns.result()) / 2,
                columns.mode() == 3 ? 0 : controls.bottom() + GAP,
                columns.result(), resultHeight);
        List<Rect> ingredients = new ArrayList<>();
        int height;
        if (columns.mode() == 3) {
            int ingredientX = controls.right() + GAP;
            int availableWidth = columns.left() - ingredientX;
            int cardWidth = (availableWidth - GAP) / 2;
            int y = 0;
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 2; column++) {
                    int index = row * 2 + column;
                    ingredients.add(new Rect(ingredientX + column * (cardWidth + GAP), y,
                            cardWidth, ingredientHeights.get(index)));
                }
                y += Math.max(ingredientHeights.get(row * 2), ingredientHeights.get(row * 2 + 1)) + ROW_GAP;
            }
            height = Math.max(Math.max(y - ROW_GAP, controls.bottom()), result.bottom());
        } else {
            int y = result.bottom() + 12;
            int cardWidth = (columns.width() - GAP) / 2;
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 2; column++) {
                    int index = row * 2 + column;
                    ingredients.add(new Rect(column * (cardWidth + GAP), y,
                            cardWidth, ingredientHeights.get(index)));
                }
                y += Math.max(ingredientHeights.get(row * 2), ingredientHeights.get(row * 2 + 1)) + ROW_GAP;
            }
            height = y - ROW_GAP;
        }
        return new Page(controls, result, List.copyOf(ingredients), height);
    }

    public record Columns(int width, int mode, int left, int form, int result) {}
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
    }
    public record Page(Rect controls, Rect result, List<Rect> ingredients, int height) {}
}
