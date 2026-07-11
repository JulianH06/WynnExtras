// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — ItemTooltipRenderEvent stub. */
package julianh06.wynnextras.wtshim.mc.event;

import net.minecraft.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ItemTooltipRenderEvent extends Event {
    protected final ItemStack itemStack;
    protected java.util.List<net.minecraft.text.Text> tooltips;

    protected ItemTooltipRenderEvent(ItemStack itemStack, java.util.List<net.minecraft.text.Text> tooltips) {
        this.itemStack = itemStack;
        this.tooltips = tooltips;
    }
    public ItemStack getItemStack() { return itemStack; }
    public java.util.List<net.minecraft.text.Text> getTooltips() { return tooltips; }
    public void setTooltips(java.util.List<net.minecraft.text.Text> tooltips) { this.tooltips = tooltips; }

    public static class Pre extends ItemTooltipRenderEvent implements ICancellableEvent {
        public Pre(ItemStack stack) { super(stack, new java.util.ArrayList<>()); }
        public Pre(ItemStack stack, java.util.List<net.minecraft.text.Text> tooltips) { super(stack, tooltips); }
    }
    public static class Post extends ItemTooltipRenderEvent {
        public Post(ItemStack stack) { super(stack, new java.util.ArrayList<>()); }
        public Post(ItemStack stack, java.util.List<net.minecraft.text.Text> tooltips) { super(stack, tooltips); }
    }
}
