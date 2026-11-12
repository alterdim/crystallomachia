package li.gerard.crystallomachia.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.slot.ItemResourceHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
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
import li.gerard.crystallomachia.golem.CapabilityTooltip;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.GolemTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractWorkstationUI<T extends AbstractWorkstationBlockEntity> {
    private static final int GHOST_ICON_COLOR = 0x50FFFFFF;
    public static @Nullable Consumer<ItemStack> SHOW_RECIPES;

    private static final int PROGRESS_WIDTH = 56;
    private static final int PROGRESS_HEIGHT = 16;
    private static final int PANEL_GAP = 2;
    private static final int REQUIREMENT_WIDTH = 26;

    protected final T workstation;

    protected AbstractWorkstationUI(T workstation) {
        this.workstation = workstation;
    }

    protected abstract UIElement createMachineArea();

    public ModularUI build(Player player) {
        UIElement main = new UIElement().layout(layout -> layout.paddingAll(6).gapAll(4).alignItems(AlignItems.CENTER));
        main.addChildren(
                new Label().setText(this.workstation.getBlockState().getBlock().getName()),
                createMachineArea(),
                createSpeedIndicator(),
                new InventorySlots());
        main.addClass("panel_bg");
        UIElement root = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.FLEX_START).gapAll(PANEL_GAP));
        if (this.workstation.golemSlots() > 0) {
            root.addChild(createGolemPanel());
        }
        root.addChild(main);
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
        ProgressBar bar = new ProgressBar().label(label -> label.setDisplay(false))
                .bindDataSource(SupplierDataSource.of(this.workstation::progressFraction));
        bar.layout(layout -> layout.width(PROGRESS_WIDTH).height(PROGRESS_HEIGHT));
        bar.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0 && SHOW_RECIPES != null) {
                SHOW_RECIPES.accept(new ItemStack(this.workstation.getBlockState().getBlock()));
            }
        });
        for (UIElement element : List.of(bar, bar.barContainer, bar.barBackground, bar.bar)) {
            element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                if (SHOW_RECIPES != null) {
                    event.hoverTooltips = HoverTooltips.create(Component.translatable("gui.crystallomachia.show_recipes"));
                }
            });
        }
        return bar;
    }

    protected UIElement createGolemPanel() {
        UIElement panel = new UIElement().layout(layout -> layout.paddingAll(6).gapAll(4).alignItems(AlignItems.FLEX_START));
        panel.addChild(new Label().setText(Component.translatable("gui.crystallomachia.golems")));
        for (int slot = 0; slot < this.workstation.golemSlots(); slot++) {
            panel.addChild(row(4).addChildren(createGolemSlot(slot), createRequirements(slot)));
        }
        panel.addClass("panel_bg");
        return panel;
    }

    protected UIElement createGolemSlot(int slot) {
        ItemSlot element = new ItemSlot().bind(this.workstation.inventory, slot);
        List<GolemCapability> capabilities = this.workstation.golemRole(slot).capabilities();
        if (!capabilities.isEmpty()) {
            IGuiTexture ghost = SpriteTexture.of(capabilities.getFirst().icon().toString()).setColor(GHOST_ICON_COLOR);
            element.slotStyle(style -> style.slotOverlay(ghost).showSlotOverlayOnlyEmpty(true));
        }
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> onGolemSlotTooltip(event, slot));
        return element;
    }

    private void onGolemSlotTooltip(UIEvent event, int slot) {
        GolemRole role = this.workstation.golemRole(slot);
        List<CapabilityTooltip.Line> lines = new ArrayList<>();
        for (GolemCapability capability : role.capabilities()) {
            lines.add(new CapabilityTooltip.Line(capability, Component.literal(Integer.toString(role.required(capability)))));
        }
        Component title = Component.translatable("gui.crystallomachia.golem_slot.requires").withStyle(ChatFormatting.GRAY);
        HoverTooltips existing = event.hoverTooltips == null ? HoverTooltips.empty() : event.hoverTooltips;
        event.hoverTooltips = existing.append(title, new CapabilityTooltip(lines));
    }

    protected UIElement createSpeedIndicator() {
        UIElement icon = new UIElement().layout(layout -> layout.width(16).height(16))
                .style(style -> style.backgroundTexture(SpriteTexture.of("crystallomachia:sprites/speed.png")));
        UIElement label = new Label().bindDataSource(SupplierDataSource.of(this::speedText));
        UIElement indicator = row(2).addChildren(icon, label);
        icon.addEventListener(UIEvents.HOVER_TOOLTIPS, this::onSpeedTooltip);
        label.addEventListener(UIEvents.HOVER_TOOLTIPS, this::onSpeedTooltip);
        indicator.addEventListener(UIEvents.HOVER_TOOLTIPS, this::onSpeedTooltip);
        for (GolemTrait trait : this.workstation.supportedTraits()) {
            UIElement traitIcon = new UIElement().layout(layout -> layout.width(GolemCapability.ICON_SIZE).height(GolemCapability.ICON_SIZE))
                    .style(style -> style.backgroundTexture(SpriteTexture.of(trait.icon().toString())));
            traitIcon.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(
                    trait.displayName().copy().withStyle(ChatFormatting.GOLD),
                    trait.description().copy().withStyle(ChatFormatting.GRAY)));
            traitIcon.setDisplay(this.workstation.hasActiveTrait(trait));
            indicator.addChild(traitIcon);
            indicator.addEventListener(UIEvents.TICK, event -> traitIcon.setDisplay(this.workstation.hasActiveTrait(trait)));
        }
        return indicator;
    }

    private Component speedText() {
        return Component.translatable("gui.crystallomachia.speed", GolemData.formatSpeed(this.workstation.workSpeed()));
    }

    private void onSpeedTooltip(UIEvent event) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.crystallomachia.speed.title", GolemData.formatSpeed(this.workstation.workSpeed())));
        if (this.workstation.hasStarvingGolem()) {
            lines.add(Component.translatable("gui.crystallomachia.speed.hungry").withStyle(ChatFormatting.RED));
        } else if (!this.workstation.meetsRequirements()) {
            lines.add(Component.translatable("gui.crystallomachia.speed.unmet").withStyle(ChatFormatting.RED));
        }
        for (int slot = 0; slot < this.workstation.golemSlots(); slot++) {
            if (!this.workstation.getStack(slot).isEmpty()) {
                lines.add(Component.translatable("gui.crystallomachia.speed.detail", slot + 1,
                        GolemData.formatSpeed(this.workstation.golemSpeed(slot))).withStyle(ChatFormatting.GRAY));
            }
        }
        event.hoverTooltips = HoverTooltips.create(lines.toArray());
    }

    protected UIElement createRequirements(int slot) {
        UIElement requirements = new UIElement().layout(layout -> layout.gapAll(1).alignItems(AlignItems.FLEX_START));
        List<GolemCapability> capabilities = this.workstation.golemRole(slot).capabilities();
        for (GolemCapability capability : capabilities) {
            UIElement label = new Label().bindDataSource(SupplierDataSource.of(() -> requirementText(slot, capability)))
                    .layout(layout -> layout.minWidth(REQUIREMENT_WIDTH).flexShrink(0));
            if (capabilities.size() == 1) {
                requirements.addChild(label);
                continue;
            }
            UIElement icon = new UIElement().layout(layout -> layout.width(GolemCapability.ICON_SIZE).height(GolemCapability.ICON_SIZE).flexShrink(0))
                    .style(style -> style.backgroundTexture(SpriteTexture.of(capability.icon().toString())));
            requirements.addChild(row(3).addChildren(icon, label));
        }
        return requirements;
    }

    private Component requirementText(int slot, GolemCapability capability) {
        int required = this.workstation.golemRole(slot).required(capability);
        int level = this.workstation.golem(slot).level(capability);
        return Component.translatable("gui.crystallomachia.requirement", level, required)
                .withStyle(level >= required ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED);
    }
}
