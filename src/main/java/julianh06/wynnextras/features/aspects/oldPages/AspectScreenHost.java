package julianh06.wynnextras.features.aspects.oldPages;

import julianh06.wynnextras.features.aspects.LootPoolData;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import net.minecraft.client.MinecraftClient;

/**
 * Interface for the host screen that pages can access
 */
public interface AspectScreenHost {
    /**
     * Get logical width (screen width in logical units)
     */
    int getHostLogicalWidth();

    /**
     * Get logical height (screen height in logical units)
     */
    int getHostLogicalHeight();

    double getScaleFactor();
    MinecraftClient getClient();

    /**
     * Navigate to a specific page
     */
    void setCurrentPage(int page);

    /**
     * Get current page index
     */
    int getCurrentPage();

    /**
     * Set hovered aspect for tooltip display
     */
    void setHoveredAspect(LootPoolData.AspectEntry aspect, int x, int y, int columnX);

    /**
     * Clear hovered aspect
     */
    void clearHoveredAspect();

    /**
     * Search for a player's aspects and switch to My Aspects page
     */
    void searchPlayer(String playerName);

    /**
     * Join raid party finder
     */
    void joinRaidPartyFinder(String raidCode);

    /**
     * Get the calculated column width for raid columns
     */
    int getHostSafeColumnWidth(int numColumns, int spacing);

    /**
     * Get API aspects list
     */
    java.util.List<ApiAspect> getAllApiAspects();
}
