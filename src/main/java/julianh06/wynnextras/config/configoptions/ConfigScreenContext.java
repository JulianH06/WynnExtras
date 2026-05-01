package julianh06.wynnextras.config.configoptions;

public interface ConfigScreenContext {
    int getContentWidth();
    boolean matchesSearch(ConfigOption opt);
    boolean subHasMatches(SubCategory sub);
    void openDropdown(DropdownOption<?> opt, int x, int y, int w);
}
