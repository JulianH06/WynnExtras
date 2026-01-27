package julianh06.wynnextras.features.inventory;

import com.wynntils.models.containers.containers.personal.*;

import java.awt.*;

public enum BankOverlayType {
    ACCOUNT, CHARACTER, BOOKSHELF, MISC, NONE;

    public boolean matches(PersonalStorageContainer container) {
        return switch (this) {
            case ACCOUNT -> container instanceof AccountBankContainer;
            case CHARACTER -> container instanceof CharacterBankContainer;
            case BOOKSHELF -> container instanceof BookshelfContainer;
            case MISC -> container instanceof MiscBucketContainer;
            default -> false;
        };
    }
}


