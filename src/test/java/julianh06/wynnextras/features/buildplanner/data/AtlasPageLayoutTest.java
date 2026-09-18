package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AtlasPageLayoutTest {
    @Test
    void sixFullWidthCardViewportsFitWithSmallGaps() {
        assertEquals(6, AtlasPageLayout.VISIBLE_ITEMS);
        assertEquals(6, AtlasPageLayout.RESULT_GAP);
        for (int width : new int[]{280, 386, 560, 914, 1040}) {
            for (int viewport : new int[]{240, 369, 600}) {
                var grid = AtlasPageLayout.results(width, viewport);
                assertTrue(grid.height(6) + 24 <= viewport);
                int occupiedWidth = grid.columns() * grid.cellWidth()
                        + (grid.columns() - 1) * AtlasPageLayout.RESULT_GAP;
                assertTrue(occupiedWidth <= width);
                assertTrue(width - occupiedWidth < grid.columns());
                if (width >= 560) assertEquals(3, grid.columns());
                assertTrue(grid.cellWidth() >= 177);
                assertTrue(grid.cellHeight() > 0);
            }
        }
    }

    @Test
    void desktopResultsUseThreeColumnsAndTwoLargeRows() {
        var grid = AtlasPageLayout.results(914, 369);
        assertEquals(3, grid.columns());
        assertEquals(300, grid.cellWidth());
        assertEquals(169, grid.cellHeight());
        assertEquals(344, grid.height(6));
    }

    @Test
    void scrollingReachesEveryResultWithoutLayingOutTheEntireCatalog() {
        for (int width : new int[]{280, 386, 914}) {
            var grid = AtlasPageLayout.results(width, 369);
            for (int count : new int[]{0, 1, 6, 7, 5409}) {
                boolean[] reached = new boolean[count];
                for (int top = -369; top <= grid.height(count); top += 30) {
                    var range = grid.visibleRange(count, top, 369);
                    assertTrue(range.start() >= 0 && range.end() <= count);
                    assertTrue(range.start() <= range.end());
                    assertTrue(range.end() - range.start()
                            <= grid.columns() * (369 / (grid.cellHeight() + AtlasPageLayout.RESULT_GAP) + 2));
                    for (int index = range.start(); index < range.end(); index++) reached[index] = true;
                }
                for (boolean visible : reached) assertTrue(visible);
                assertEquals(count, grid.visibleRange(count, Math.max(0, grid.height(count) - 369), 369).end());
            }
        }
    }

    @Test
    void offscreenResultsProduceNoCardsAndKeepCurrentSizing() {
        var grid = AtlasPageLayout.results(914, 369);
        assertEquals(new AtlasPageLayout.VisibleRange(0, 0), grid.visibleRange(5409, -400, 369));
        assertEquals(new AtlasPageLayout.VisibleRange(0, 9), grid.visibleRange(5409, 0, 369));
        assertEquals(new AtlasPageLayout.VisibleRange(5409, 5409),
                grid.visibleRange(5409, grid.height(5409) + 10, 369));
        assertEquals(300, grid.cellWidth());
        assertEquals(169, grid.cellHeight());
    }

    @Test
    void stacksSmallScreensAndKeepsAllFilterRowsAboveResults() {
        for (int width : new int[]{320, 426, 599, 600, 854, 1280, 1920}) {
            for (int rows : new int[]{0, 1, 10, 40}) {
                var layout = AtlasPageLayout.arrange(width, 278, 140 + rows * 46);
                assertTrue(layout.left() >= 12);
                assertTrue(layout.left() + layout.width() <= width - 12);
                assertTrue(layout.filtersX() + layout.columnWidth() <= layout.width());
                assertTrue(layout.resultsY() > 278);
                assertTrue(layout.resultsY() > layout.filtersY() + 140 + rows * 46);
                if (width < 600) {
                    assertEquals(0, layout.filtersX());
                    assertTrue(layout.filtersY() > 278);
                } else {
                    assertEquals(0, layout.filtersY());
                    assertTrue(layout.filtersX() > layout.columnWidth());
                }
            }
        }
    }
}
