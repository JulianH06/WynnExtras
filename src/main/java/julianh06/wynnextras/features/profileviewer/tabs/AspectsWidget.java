package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.User;
import net.minecraft.client.gui.DrawContext;

import java.util.Objects;

import static julianh06.wynnextras.features.profileviewer.PV.openedAspectPage;

public class AspectsWidget extends PVScreen.TabWidget{
    public static User currentPlayerAspectData;
    public static WynncraftApiHandler.FetchStatus fetchStatus;
    Page currentPage;

    public enum Page {Overview, Warrior, Shaman, Mage, Archer, Assassin}


    public AspectsWidget() {
        super(0, 0, 0, 0);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (PV.currentPlayerData == null) return;

        if(!openedAspectPage) {
            openedAspectPage = true;
            currentPlayerAspectData = null;
            fetchStatus = null;
            currentPage = Page.Overview;
            WynncraftApiHandler.fetchPlayerAspectData(PV.currentPlayerData.getUuid().toString())
                    .thenAccept(result -> {
                        if (result == null) return;

                        if (result.status() != null) {
                            if(result.status() == WynncraftApiHandler.FetchStatus.OK) currentPlayerAspectData = result.user();
                            fetchStatus = result.status();
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("Unexpected error: " + ex.getMessage());
                        return null;
                    });
        }

        if(fetchStatus == null) {
            ui.drawCenteredText("Loading aspects...", x + 900, y + 365, CustomColor.fromHexString("FFFF00"), 4f);
            return;
        }

        switch (fetchStatus) {
            case NOT_FOUND -> {
                ui.drawCenteredText("No data found for this player!", x + 900, y + 365, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
            case SERVER_UNREACHABLE -> {
                ui.drawCenteredText("Server unreachable. Try again later.", x + 900, y + 365, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
            case SERVER_ERROR -> {
                ui.drawCenteredText("Server error occurred!", x + 900, y + 365, CustomColor.fromHexString("FF0000"), 4f);
                return;
            }
        }

        if(currentPlayerAspectData == null) {
            System.out.println(fetchStatus);
            return;
        }

        ui.drawText(currentPlayerAspectData.getPlayerName(), x, y);
    }
}
