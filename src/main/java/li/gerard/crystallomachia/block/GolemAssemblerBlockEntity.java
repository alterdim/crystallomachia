package li.gerard.crystallomachia.block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemMaterial;
import li.gerard.crystallomachia.golem.GolemPart;
import li.gerard.crystallomachia.golem.GolemPartItem;
import li.gerard.crystallomachia.golem.GolemParts;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.GolemTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class GolemAssemblerBlockEntity extends AbstractWorkstationBlockEntity {
    public static final List<GolemRole> GOLEM_ROLES = List.of();

    public GolemAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.GOLEM_ASSEMBLER_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, GolemPart.values().length);
    }

    public static int partSlot(GolemPart part) {
        return part.ordinal();
    }

    @Override
    protected boolean isItemValid(int slot, ItemResource resource) {
        GolemPart part = GolemPart.values()[slot];
        if (part == GolemPart.TORSO && resource.getItem() instanceof GolemItem) {
            return true;
        }
        return resource.getItem() instanceof GolemPartItem item && item.part() == part;
    }

    @Override
    public GolemAnchor golemAnchor(int slot) {
        return GolemAnchor.inFront();
    }

    @Override
    protected void serverTick(ServerLevel level) {
    }

    private boolean hasPart(GolemPart part) {
        return getStack(partSlot(part)).getItem() instanceof GolemPartItem;
    }

    private boolean hasAllParts() {
        for (GolemPart part : GolemPart.values()) {
            if (!hasPart(part)) {
                return false;
            }
        }
        return true;
    }

    public void assemble() {
        if (this.level == null || this.level.isClientSide() || !hasAllParts()) {
            return;
        }
        ItemStack golem = GolemItem.create(combineParts(this.level.getRandom().nextLong()), partMaterials());
        for (GolemPart part : GolemPart.values()) {
            setStack(partSlot(part), ItemStack.EMPTY);
        }
        setStack(partSlot(GolemPart.TORSO), golem);
    }

    private GolemParts partMaterials() {
        Map<GolemPart, ResourceKey<GolemMaterial>> materials = new EnumMap<>(GolemPart.class);
        for (GolemPart part : GolemPart.values()) {
            ResourceKey<GolemMaterial> material = getStack(partSlot(part)).get(crystallomachia.GOLEM_MATERIAL.get());
            if (material != null) {
                materials.put(part, material);
            }
        }
        return new GolemParts(materials);
    }

    protected GolemData combineParts(long appearanceSeed) {
        List<GolemData> contributions = new ArrayList<>();
        for (GolemPart part : GolemPart.values()) {
            if (hasPart(part)) {
                contributions.add(GolemItem.getData(getStack(partSlot(part))));
            }
        }
        return GolemData.combine(contributions, appearanceSeed);
    }

    public @Nullable GolemData preview() {
        ItemStack torso = getStack(partSlot(GolemPart.TORSO));
        if (torso.getItem() instanceof GolemItem) {
            return GolemItem.getData(torso);
        }
        for (GolemPart part : GolemPart.values()) {
            if (hasPart(part)) {
                return combineParts(0L);
            }
        }
        return null;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement parts = column(2).addChildren(
                partSlotElement(GolemPart.HEAD),
                row(2).addChildren(partSlotElement(GolemPart.HAND_LEFT), partSlotElement(GolemPart.TORSO), partSlotElement(GolemPart.HAND_RIGHT)),
                partSlotElement(GolemPart.LEGS),
                new Button().setText(Component.translatable("gui.crystallomachia.assemble")).setOnServerClick(event -> assemble()));

        UIElement root = new UIElement().layout(layout -> layout.paddingAll(6).gapAll(4).alignItems(AlignItems.CENTER));
        root.addChildren(
                new Label().setText(getBlockState().getBlock().getName()),
                row(8).addChildren(parts, createPreview()),
                new InventorySlots());
        root.addClass("panel_bg");
        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC)), holder.player);
    }

    private UIElement partSlotElement(GolemPart part) {
        return new ItemSlot().bind(this.inventory, partSlot(part)).style(style -> style.tooltips(part.displayName()));
    }

    private UIElement createPreview() {
        ScrollerView scroller = new ScrollerView();
        scroller.layout(layout -> layout.widthPercent(100).heightPercent(100));
        scroller.addScrollViewChild(new Label().setText(Component.translatable("gui.crystallomachia.preview").withStyle(ChatFormatting.GRAY)));

        List<Runnable> updates = new ArrayList<>();
        addStatLine(scroller, updates, "tooltip.crystallomachia.speed", data -> data.speed());
        addStatLine(scroller, updates, "tooltip.crystallomachia.health", data -> data.combat().health());
        addStatLine(scroller, updates, "tooltip.crystallomachia.attack", data -> data.combat().attack());
        addStatLine(scroller, updates, "tooltip.crystallomachia.armor", data -> data.combat().armor());

        for (GolemCapability capability : GolemCapability.values()) {
            UIElement line = row(3).addChildren(
                    icon(capability.icon().toString()),
                    new Label().bindDataSource(SupplierDataSource.of(() -> {
                        GolemData data = preview();
                        return Component.literal(Integer.toString(data == null ? 0 : data.level(capability))).withStyle(ChatFormatting.WHITE);
                    })));
            scroller.addScrollViewChild(line);
            updates.add(() -> {
                GolemData data = preview();
                line.setDisplay(data != null && data.level(capability) > 0);
            });
        }
        for (GolemTrait trait : GolemTrait.values()) {
            UIElement line = row(3).addChildren(
                    icon(trait.icon().toString()),
                    new Label().setText(trait.displayName().copy().withStyle(ChatFormatting.GOLD)));
            scroller.addScrollViewChild(line);
            updates.add(() -> {
                GolemData data = preview();
                line.setDisplay(data != null && data.hasTrait(trait));
            });
        }
        updates.forEach(Runnable::run);

        UIElement panel = new UIElement().layout(layout -> layout.width(96).height(86).paddingAll(3))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF000000)));
        panel.addChild(scroller);
        panel.addEventListener(UIEvents.TICK, event -> updates.forEach(Runnable::run));
        return panel;
    }

    private void addStatLine(ScrollerView scroller, List<Runnable> updates, String key, Function<GolemData, Float> stat) {
        UIElement line = new Label().bindDataSource(SupplierDataSource.of(() -> {
            GolemData data = preview();
            return Component.translatable(key, GolemData.formatStat(data == null ? 0f : stat.apply(data))).withStyle(ChatFormatting.WHITE);
        }));
        scroller.addScrollViewChild(line);
        updates.add(() -> line.setDisplay(preview() != null));
    }
    private static UIElement icon(String texture) {
        return new UIElement().layout(layout -> layout.width(GolemCapability.ICON_SIZE).height(GolemCapability.ICON_SIZE))
                .style(style -> style.backgroundTexture(SpriteTexture.of(texture)));
    }

    private static UIElement row(int gap) {
        return new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(gap));
    }

    private static UIElement column(int gap) {
        return new UIElement().layout(layout -> layout.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER).gapAll(gap));
    }
}
