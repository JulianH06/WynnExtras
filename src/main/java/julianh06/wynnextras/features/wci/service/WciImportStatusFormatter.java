package julianh06.wynnextras.features.wci.service;

public final class WciImportStatusFormatter {
    private WciImportStatusFormatter() {}
    public static String format(ShoppingCartService.ImportResult result) {
        if (result.success()) return "Imported " + result.importedEntries() + " WCI entries (" + result.totalItems() + " total items).";
        return "WCI import failed: " + result.error();
    }
}
