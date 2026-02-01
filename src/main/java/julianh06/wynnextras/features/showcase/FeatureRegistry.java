package julianh06.wynnextras.features.showcase;

import julianh06.wynnextras.config.WynnExtrasConfig;

import java.util.ArrayList;
import java.util.List;

public class FeatureRegistry {
    public static final List<FeatureInfo> FEATURES = new ArrayList<>();

    static {
        // Profile Viewer
        FEATURES.add(new FeatureInfo(
                "Profile Viewer",
                "View detailed player profiles including class stats, gear, and more",
                "Social",
                "/pv <player>"
        ));

        // Guild Viewer
        FEATURES.add(new FeatureInfo(
                "Guild Viewer",
                "View guild information, members, and statistics",
                "Social",
                "/gv <tag>"
        ));

        // Bank Overlay
        FEATURES.add(new FeatureInfo(
                "Bank Overlay",
                "Enhanced bank interface with multi-page view, search, and organization",
                "Inventory",
                "Auto in bank",
                () -> WynnExtrasConfig.INSTANCE.toggleBankOverlay
        ));

        // Raid Tracker
        FEATURES.add(new FeatureInfo(
                "Raid Tracker",
                "Track raid completions, times, and loot history",
                "Raids",
                "/we raids",
                () -> WynnExtrasConfig.INSTANCE.toggleRaidTimestamps
        ));

        // Raid Loot Tracker
        FEATURES.add(new FeatureInfo(
                "Raid Loot Tracker",
                "HUD overlay showing current raid loot pool and expected drops",
                "Raids",
                "Auto in raids",
                () -> WynnExtrasConfig.INSTANCE.toggleRaidLootTracker
        ));

        // Aspect Tracker
        FEATURES.add(new FeatureInfo(
                "Aspect Tracker",
                "Track aspect progress and share with the community",
                "Aspects",
                "Auto"
        ));

        // Achievements
        FEATURES.add(new FeatureInfo(
                "Achievements",
                "Track your progress with custom WynnExtras achievements",
                "Misc",
                "/we achievements",
                () -> WynnExtrasConfig.INSTANCE.achievementsEnabled
        ));

        // Ability Tree
        FEATURES.add(new FeatureInfo(
                "Ability Tree Viewer",
                "View and import/export ability tree builds",
                "Builds",
                "/we tree"
        ));

        // Player Hider
        FEATURES.add(new FeatureInfo(
                "Player Hider",
                "Hide nearby players to reduce visual clutter",
                "Rendering",
                "Config toggle",
                () -> WynnExtrasConfig.INSTANCE.playerHiderToggle
        ));

        // Chat Notifier
        FEATURES.add(new FeatureInfo(
                "Chat Notifier",
                "Get notified when specific words appear in chat",
                "Chat",
                "/we notifier"
        ));

        // Fast Requeue
        FEATURES.add(new FeatureInfo(
                "Fast Requeue",
                "Quickly requeue for raids after completion",
                "Raids",
                "Shift+Click",
                () -> WynnExtrasConfig.INSTANCE.toggleFastRequeue
        ));

        // Waypoints
        FEATURES.add(new FeatureInfo(
                "Waypoints",
                "Create and manage custom waypoints on the map",
                "Navigation",
                "/we waypoints"
        ));

        // Weight Display
        FEATURES.add(new FeatureInfo(
                "Weight Display",
                "Show item weight/scales on tooltips",
                "Inventory",
                "Auto on hover",
                () -> WynnExtrasConfig.INSTANCE.showWeight
        ));

        // Totem Range
        FEATURES.add(new FeatureInfo(
                "Totem Range Visualizer",
                "Show shaman totem range indicator",
                "Combat",
                "Auto for Shaman",
                () -> WynnExtrasConfig.INSTANCE.totemRangeVisualizerToggle
        ));

        // Provoke Timer
        FEATURES.add(new FeatureInfo(
                "Provoke Timer",
                "Track warrior provoke cooldown",
                "Combat",
                "Auto for Warrior",
                () -> WynnExtrasConfig.INSTANCE.provokeTimerToggle
        ));

        // Trade Market Overlay
        FEATURES.add(new FeatureInfo(
                "Trade Market Overlay",
                "Enhanced trade market interface with value calculations",
                "Economy",
                "Auto in TM"
        ));

        // Trade Market Item Comparison
        FEATURES.add(new FeatureInfo(
                "Item Comparison",
                "Ctrl+Click on items in Trade Market to save for comparison",
                "Economy",
                "Ctrl+Click"
        ));

        // Crafting Helper
        FEATURES.add(new FeatureInfo(
                "Crafting Helper",
                "Assist with crafting recipes and materials",
                "Crafting",
                "Auto in stations",
                () -> WynnExtrasConfig.INSTANCE.craftingHelperOverlay
        ));

        // Lootrun Tracker
        FEATURES.add(new FeatureInfo(
                "Lootrun Loot Pools",
                "View and track lootrun loot pools across camps",
                "Lootruns",
                "/we lootrun"
        ));

        // Lootrun Statistics HUD
        FEATURES.add(new FeatureInfo(
                "Lootrun Statistics",
                "Track pulls, mythics, and more during lootruns with a HUD overlay",
                "Lootruns",
                "Auto during lootruns",
                () -> WynnExtrasConfig.INSTANCE.lootrunHudEnabled
        ));

        // User Badges
        FEATURES.add(new FeatureInfo(
                "User Badges",
                "Show a star badge above WynnExtras users and in chat",
                "Social",
                "Auto",
                () -> WynnExtrasConfig.INSTANCE.badgesEnabled
        ));
    }

    public static List<FeatureInfo> getFeaturesByCategory(String category) {
        List<FeatureInfo> result = new ArrayList<>();
        for (FeatureInfo feature : FEATURES) {
            if (feature.getCategory().equals(category)) {
                result.add(feature);
            }
        }
        return result;
    }

    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        for (FeatureInfo feature : FEATURES) {
            if (!categories.contains(feature.getCategory())) {
                categories.add(feature.getCategory());
            }
        }
        return categories;
    }
}
