// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — PoiLocation.
 * DEVIATION: the Location-backed helpers (asLocation()/fromLocation(Location)) are omitted
 * here — utils/mc/type/Location pulls in MathUtils/PosUtils/BlockPos/Vec3 and is only needed
 * by the map RENDER layer (phase 8b). Add them back alongside Location when the screen ports.
 */
package julianh06.wynnextras.wtshim.utils.mc.type;

import java.util.Objects;
import java.util.Optional;

public class PoiLocation {
    private final int x;
    private final Integer y;
    private final int z;

    public PoiLocation(int x, Integer y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public Optional<Integer> getY() {
        return Optional.ofNullable(y);
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        // Use short form if we're missing y coordinate
        if (y == null) return "[" + x + ", " + z + "]";

        return "[" + x + ", " + y + ", " + z + "]";
    }

    public String asChatCoordinates() {
        return x + " " + y + " " + z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        PoiLocation that = (PoiLocation) other;
        return x == that.x && Objects.equals(y, that.y) && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
