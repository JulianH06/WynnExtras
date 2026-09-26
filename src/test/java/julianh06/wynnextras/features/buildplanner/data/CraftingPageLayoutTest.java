package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CraftingPageLayoutTest {
    @Test
    void matchesTheWideThreeSectionLayoutAndStacksWithoutOverlapOnNarrowScreens() {
        for (int width : List.of(280, 320, 426, 519, 520, 640, 699, 700, 800, 1000)) {
            var columns = CraftingPageLayout.columns(width);
            var page = CraftingPageLayout.arrange(columns, 251, 234, 371, List.of(140, 256, 201, 188, 188, 188));
            List<CraftingPageLayout.Rect> bounds = new ArrayList<>(List.of(page.controls(), page.summary(), page.result()));
            bounds.addAll(page.ingredients());
            for (var rect : bounds) {
                assertTrue(rect.x() >= 0 && rect.y() >= 0, "negative coordinate at " + width);
                assertTrue(rect.right() <= width, "horizontal overflow at " + width);
                assertTrue(rect.bottom() <= page.height(), "unreachable card at " + width);
                assertTrue(rect.width() >= 100);
            }
            for (int first = 0; first < bounds.size(); first++) {
                for (int second = first + 1; second < bounds.size(); second++) {
                    var a = bounds.get(first);
                    var b = bounds.get(second);
                    assertTrue(a.right() <= b.x() || b.right() <= a.x() || a.bottom() <= b.y() || b.bottom() <= a.y(),
                            "overlap at width " + width + ": " + first + ", " + second);
                }
            }
            if (width >= 700) {
                assertEquals(0, page.controls().y());
                assertEquals(0, page.summary().y());
                assertEquals(0, page.result().y());
                assertTrue(page.result().x() > page.summary().x());
            }
            assertEquals(6, page.ingredients().size());
            assertEquals(page.ingredients().get(0).y(), page.ingredients().get(1).y());
            assertTrue(page.ingredients().get(2).y() > page.ingredients().get(1).bottom());
        }
    }

    @Test
    void rejectsUnsupportedDimensionsInsteadOfProducingBrokenBounds() {
        assertThrows(IllegalArgumentException.class, () -> CraftingPageLayout.columns(100));
        assertThrows(IllegalArgumentException.class,
                () -> CraftingPageLayout.arrange(CraftingPageLayout.columns(800), 251, 234, 300, List.of(100)));
    }
}
