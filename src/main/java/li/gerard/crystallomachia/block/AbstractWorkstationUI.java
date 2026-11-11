package li.gerard.crystallomachia.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.lowdragmc.lowdraglib2.gui.slot.ItemResourceHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import li.gerard.crystallomachia.golem.GolemCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public abstract class AbstractWorkstationUI<T extends AbstractWorkstationBlockEntity> {
    protected final T workstation;

    protected AbstractWorkstationUI(T workstation) {
        this.workstation = workstation;
    }

    protected abstract UIElement createMachineArea();

    public ModularUI build(Player player) {
        UIElement body = row(8).addChildren(createGolemSlots(), createMachineArea());
        UIElement root = new UIElement().layout(layout -> layout.paddingAll(6).gapAll(4).alignItems(AlignItems.CENTER));
        root.addChildren(
                new Label().setText(this.workstation.getBlockState().getBlock().getName()),
                body,
                new Label().bindDataSource(SupplierDataSource.of(this::requirementsText)),
                createSpeedIndicator(),
                new InventorySlots());
        root.addClass("panel_bg");
        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC)), player);
    }

    protected UIElement row(int gap) {
        return new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(gap));
    }

    protected UIElement column(int gap) {
        return new UIElement().layout(layout -> layout.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER).gapAll(gap));
    }

    protected UIElement slot(int index) {
        return new ItemSlot().bind(this.workstation.inventory, index);
    }

    protected UIElement outputSlot(int index) {
        return new ItemSlot().bind(new ItemResourceHandlerSlot(this.workstation.inventory, index).setCanPlace(stack -> false));
    }

    protected UIElement progressArrow() {
        return new ProgressBar().bindDataSource(SupplierDataSource.of(this.workstation::progressFraction))
                .layout(layout -> layout.width(40).height(10));
    }

    protected UIElement createGolemSlots() {
        UIElement golems = column(2);
        for (int slot = 0; slot < this.workstation.golemSlots(); slot++) {
            golems.addChild(slot(slot));
        }
        return golems;
    }

    protected UIElement createSpeedIndicator() {
        UIElement icon = new UIElement().layout(layout -> layout.width(16).height(16))
                .style(style -> style.backgroundTexture(SpriteTexture.of("crystallomachia:sprites/speed.png")));
        UIElement label = new Label().bindDataSource(SupplierDataSource.of(this::speedBonusText));
        UIElement indicator = row(2).addChildren(icon, label);
        icon.addEventListener(UIEvents.HOVER_TOOLTIPS, this::onSpeedTooltip);
        label.addEventListener(UIEvents.HOVER_TOOLTIPS, this::onSpeedTooltip);
        indicator.addEventListener(UIEvents.HOVER_TOOLTIPS, this::onSpeedTooltip);
        return indicator;
    }

    private Component speedBonusText() {
        return Component.translatable("gui.crystallomachia.speed_bonus", this.workstation.speedBonusPercent());
    }

    private void onSpeedTooltip(UIEvent event) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.crystallomachia.speed_bonus.title", this.workstation.speedBonusPercent()));
        if (!this.workstation.meetsRequirements()) {
            lines.add(Component.translatable("gui.crystallomachia.speed_bonus.unmet").withStyle(ChatFormatting.RED));
        }
        for (Map.Entry<GolemCapability, Integer> requirement : this.workstation.requirements().entrySet()) {
            int level = this.workstation.capabilityLevel(requirement.getKey());
            int bonus = Math.max(0, level - requirement.getValue()) * 100;
            lines.add(Component.translatable("gui.crystallomachia.speed_bonus.detail",
                    requirement.getKey().displayName(), level, requirement.getValue(), bonus).withStyle(ChatFormatting.GRAY));
        }
        event.hoverTooltips = HoverTooltips.create(lines.toArray());
    }

    private Component requirementsText() {
        MutableComponent text = Component.empty();
        boolean first = true;
        for (Map.Entry<GolemCapability, Integer> requirement : this.workstation.requirements().entrySet()) {
            int level = this.workstation.capabilityLevel(requirement.getKey());
            if (!first) {
                text.append(", ");
            }
            text.append(Component.translatable("gui.crystallomachia.requirement", requirement.getKey().displayName(), level, requirement.getValue())
                    .withStyle(level >= requirement.getValue() ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
            first = false;
        }
        return text;
    }
}
