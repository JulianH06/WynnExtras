package julianh06.wynnextras.features.buildplanner.data;

import java.util.ArrayList;
import java.util.List;

public final class CraftingPageLayout {
    private static final int GAP = 12;

    private CraftingPageLayout() {}

    public static Columns columns(int width) {
        if (width < 280) throw new IllegalArgumentException("Crafting page is too narrow");
        int mode = width >= 700 ? 3 : width >= 520 ? 2 : 1;
        int controls = mode == 3 ? (width - 2 * GAP) * 43 / 100 : mode == 2 ? (width - GAP) * 55 / 100 : width;
        int summary = mode == 3 ? (width - 2 * GAP) * 30 / 100 : mode == 2 ? width - GAP - controls : width;
        int result = mode == 3 ? width - controls - summary - 2 * GAP : Math.min(270, width);
        int ingredients = Math.min(440, mode == 3 ? controls + GAP + summary : width);
        return new Columns(width, mode, controls, summary, result, ingredients, (ingredients - GAP) / 2);
    }

    public static Page arrange(Columns columns, int controlsHeight, int summaryHeight, int resultHeight,
            List<Integer> ingredientHeights) {
        if (ingredientHeights.size() != 6 || ingredientHeights.stream().anyMatch(height -> height <= 0)
                || controlsHeight <= 0 || summaryHeight <= 0 || resultHeight <= 0) {
            throw new IllegalArgumentException("Invalid crafting card dimensions");
        }
        Rect controls = new Rect(0, 0, columns.controls(), controlsHeight);
        Rect summary = new Rect(columns.mode() == 1 ? 0 : columns.controls() + GAP,
                columns.mode() == 1 ? controls.bottom() + GAP : 0, columns.summary(), summaryHeight);
        Rect result = new Rect(columns.mode() == 3 ? summary.right() + GAP : (columns.width() - columns.result()) / 2,
                columns.mode() == 3 ? 0 : Math.max(controls.bottom(), summary.bottom()) + GAP,
                columns.result(), resultHeight);
        int headingY = Math.max(Math.max(controls.bottom(), summary.bottom()), result.bottom()) + 24;
        int areaWidth = columns.mode() == 3 ? columns.controls() + GAP + columns.summary() : columns.width();
        int ingredientLeft = (areaWidth - columns.ingredients()) / 2;
        int y = headingY + 20;
        List<Rect> ingredients = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                int index = row * 2 + col;
                ingredients.add(new Rect(ingredientLeft + col * (columns.ingredientCard() + GAP), y,
                        columns.ingredientCard(), ingredientHeights.get(index)));
            }
            y += Math.max(ingredientHeights.get(row * 2), ingredientHeights.get(row * 2 + 1)) + GAP;
        }
        return new Page(controls, summary, result, List.copyOf(ingredients), headingY, ingredientLeft,
                columns.ingredients(), y);
    }

    public record Columns(int width, int mode, int controls, int summary, int result, int ingredients, int ingredientCard) {}
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
    }
    public record Page(Rect controls, Rect summary, Rect result, List<Rect> ingredients, int headingY,
            int ingredientLeft, int ingredientWidth, int height) {}
}
