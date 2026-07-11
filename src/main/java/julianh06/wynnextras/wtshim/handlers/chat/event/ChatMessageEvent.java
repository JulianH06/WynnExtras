// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * package relocation only, logic unchanged. Replaces the earlier reduced stub.
 */
package julianh06.wynnextras.wtshim.handlers.chat.event;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.handlers.chat.type.RecipientType;
import net.neoforged.bus.api.Event;

public abstract class ChatMessageEvent extends Event {
    protected final StyledText message;
    private final RecipientType recipientType;

    protected ChatMessageEvent(StyledText message, RecipientType recipientType) {
        this.message = message;
        this.recipientType = recipientType;
    }

    public StyledText getMessage() {
        return message;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }

    /**
     * This event is what models and features should use to listen to chat messages.
     * Any listener can request to cancel the chat, but it will still be sent to all
     * other listeners for matching, even if it is going to be canceled.
     */
    public static class Match extends ChatMessageEvent {
        private boolean chatCanceled = false;

        public Match(StyledText message, RecipientType recipientType) {
            super(message, recipientType);
        }

        public boolean isChatCanceled() {
            return chatCanceled;
        }

        public void cancelChat() {
            this.chatCanceled = true;
        }
    }

    /**
     * This event is what features should use if they want to rewrite chat messages.
     */
    public static class Edit extends ChatMessageEvent {
        private StyledText editedMessage = null;

        public Edit(StyledText message, RecipientType recipientType) {
            super(message, recipientType);
        }

        @Override
        public StyledText getMessage() {
            return (editedMessage != null) ? editedMessage : message;
        }

        public void setMessage(StyledText message) {
            this.editedMessage = message;
        }
    }
}
