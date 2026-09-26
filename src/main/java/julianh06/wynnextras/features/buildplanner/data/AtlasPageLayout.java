package julianh06.wynnextras.features.buildplanner.data;

public record AtlasPageLayout(int left, int width, int columnWidth, int filtersX, int filtersY,
        int resultsY) {
    public static final int VISIBLE_ITEMS = 6;
    public static final int RESULT_GAP = 6;

    public static ResultGrid results(int width, int viewportHeight) {
        int columns = width >= 560 ? 3 : width >= 360 ? 2 : 1;
        int rows = (VISIBLE_ITEMS + columns - 1) / columns;
        int cellWidth = (width - (columns - 1) * RESULT_GAP) / columns;
        int cellHeight = Math.max(1, (viewportHeight - 24 - (rows - 1) * RESULT_GAP) / rows);
        return new ResultGrid(columns, cellWidth, cellHeight);
    }

    public record ResultGrid(int columns, int cellWidth, int cellHeight) {
        public int height(int resultCount) {
            int rows = (resultCount + columns - 1) / columns;
            return rows == 0 ? 0 : rows * cellHeight + (rows - 1) * RESULT_GAP;
        }

        public VisibleRange visibleRange(int resultCount, int top, int viewportHeight) {
            if (resultCount == 0 || viewportHeight <= 0 || top + viewportHeight <= 0) {
                return new VisibleRange(0, 0);
            }
            int stride = cellHeight + RESULT_GAP;
            int firstRow = Math.max(0, top) / stride;
            int lastRow = (top + viewportHeight - 1) / stride;
            return new VisibleRange(Math.min(resultCount, firstRow * columns),
                    Math.min(resultCount, (lastRow + 1) * columns));
        }
    }

    public record VisibleRange(int start, int end) {}

    public static AtlasPageLayout arrange(int screenWidth, int leftHeight, int filtersHeight) {
        int width = Math.min(1040, screenWidth - 40);
        boolean wide = width >= 560;
        int column = wide ? (width - 28) / 2 : width;
        int filtersY = wide ? 0 : leftHeight + 24;
        return new AtlasPageLayout((screenWidth - width) / 2, width, column,
                wide ? column + 28 : 0, filtersY,
                Math.max(leftHeight, filtersY + filtersHeight) + 28);
    }
}
