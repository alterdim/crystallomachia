package li.gerard.crystallomachia.block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.component.DataComponents;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemCoreItem;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemMaterial;
import li.gerard.crystallomachia.golem.GolemPart;
import li.gerard.crystallomachia.golem.GolemPartItem;
import li.gerard.crystallomachia.golem.GolemParts;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.GolemStat;
import li.gerard.crystallomachia.golem.GolemTier;
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
    private static final int STAT_TEXT_WIDTH = 30;

    public static final int CORE_SLOT = GolemPart.values().length;
    private static final int SLOT_SIZE = 18;
    private static final int NAME_FIELD_WIDTH = 58;
    private static final int MAX_NAME_LENGTH = 32;

    private static final int TRAIT_TILE_HEIGHT = 14;
    private static final IGuiTexture TRAIT_TILE = new GuiTextureGroup(new ColorRectTexture(0xFF2B2B2B), new ColorBorderTexture(1, 0xFF6B5A1E));
    private static final IGuiTexture LOCKED_TRAIT_TILE = new GuiTextureGroup(new ColorRectTexture(0xFF4A1212), new ColorBorderTexture(1, 0xFFD03030));
    private static final IGuiTexture EMPTY_TRAIT_SLOT = new ColorBorderTexture(1, 0xFF555555);

    @Persisted
    private String golemName = "";

    @Persisted
    @DescSynced
    private int pickedTraits;

    private void setGolemName(String name) {
        String trimmed = name == null ? "" : name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
        if (!trimmed.equals(this.golemName)) {
            this.golemName = trimmed;
            setChanged();
        }
    }

    public GolemAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.GOLEM_ASSEMBLER_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, GolemPart.values().length + 1);
    }

    public static int partSlot(GolemPart part) {
        return part.ordinal();
    }

    @Override
    protected boolean isItemValid(int slot, ItemResource resource) {
        if (slot == CORE_SLOT) {
            return resource.getItem() instanceof GolemCoreItem;
        }
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

    private boolean hasCore() {
        return getStack(CORE_SLOT).getItem() instanceof GolemCoreItem;
    }

    private List<GolemData> contributions() {
        List<GolemData> contributions = new ArrayList<>();
        for (GolemPart part : GolemPart.values()) {
            if (hasPart(part)) {
                contributions.add(GolemItem.getData(getStack(partSlot(part))));
            }
        }
        if (hasCore()) {
            contributions.add(GolemItem.getData(getStack(CORE_SLOT)));
        }
        return contributions;
    }

    public GolemTier tier() {
        GolemTier tier = GolemTier.DEFAULT;
        for (GolemPart part : GolemPart.values()) {
            if (hasPart(part)) {
                tier = GolemTier.lowest(tier, GolemTier.of(getStack(partSlot(part))));
            }
        }
        return hasCore() ? GolemTier.lowest(tier, GolemTier.of(getStack(CORE_SLOT))) : tier;
    }

    private GolemTier previewTier() {
        ItemStack torso = getStack(partSlot(GolemPart.TORSO));
        return torso.getItem() instanceof GolemItem ? GolemTier.of(torso) : tier();
    }

    public Set<GolemTrait> availableTraits() {
        Set<GolemTrait> traits = EnumSet.noneOf(GolemTrait.class);
        for (GolemData contribution : contributions()) {
            traits.addAll(contribution.traits());
        }
        GolemTier tier = tier();
        traits.removeIf(trait -> !tier.allows(trait));
        return traits;
    }

    public Set<GolemTrait> lockedTraits() {
        Set<GolemTrait> locked = EnumSet.noneOf(GolemTrait.class);
        if (this.level == null) {
            return locked;
        }
        HolderLookup.RegistryLookup<GolemMaterial> materials = this.level.registryAccess().lookup(GolemMaterial.REGISTRY_KEY).orElse(null);
        if (materials == null) {
            return locked;
        }
        for (GolemPart part : GolemPart.values()) {
            ItemStack stack = getStack(partSlot(part));
            ResourceKey<GolemMaterial> key = stack.get(crystallomachia.GOLEM_MATERIAL.get());
            if (!hasPart(part) || key == null) {
                continue;
            }
            List<GolemTrait> partTraits = GolemItem.getData(stack).traits();
            materials.get(key).ifPresent(material -> {
                for (GolemTrait trait : material.value().lockedTraits()) {
                    if (partTraits.contains(trait)) {
                        locked.add(trait);
                    }
                }
            });
        }
        locked.retainAll(availableTraits());
        return locked;
    }

    public int traitSlots() {
        float intelligence = 0f;
        for (GolemData contribution : contributions()) {
            intelligence += contribution.intelligence();
        }
        return GolemData.traitSlots(intelligence);
    }

    private boolean isPicked(GolemTrait trait) {
        return (this.pickedTraits & 1 << trait.ordinal()) != 0;
    }

    public List<GolemTrait> chosenTraits() {
        Set<GolemTrait> available = availableTraits();
        Set<GolemTrait> locked = lockedTraits();
        int slots = traitSlots();
        List<GolemTrait> chosen = new ArrayList<>();
        for (boolean lockedPass : new boolean[] {true, false}) {
            for (GolemTrait trait : available) {
                if (isPicked(trait) && locked.contains(trait) == lockedPass && chosen.size() < slots) {
                    chosen.add(trait);
                }
            }
        }
        return chosen;
    }

    public boolean hasAllLockedTraits() {
        return chosenTraits().containsAll(lockedTraits());
    }

    private void setTraitPicked(int ordinal, boolean picked) {
        if (ordinal < 0 || ordinal >= GolemTrait.values().length || getStack(partSlot(GolemPart.TORSO)).getItem() instanceof GolemItem) {
            return;
        }
        GolemTrait trait = GolemTrait.values()[ordinal];
        if (picked == isPicked(trait)) {
            return;
        }
        if (picked && (!availableTraits().contains(trait) || chosenTraits().size() >= traitSlots())) {
            return;
        }
        this.pickedTraits ^= 1 << ordinal;
        setChanged();
    }

    public void assemble() {
        if (this.level == null || this.level.isClientSide() || !hasAllParts() || !hasCore() || !hasAllLockedTraits()) {
            return;
        }
        ItemStack golem = GolemItem.create(combineParts(this.level.getRandom().nextLong()), partMaterials(), GolemItem.getData(getStack(CORE_SLOT)));
        golem.set(crystallomachia.GOLEM_TIER.get(), tier());
        for (GolemPart part : GolemPart.values()) {
            setStack(partSlot(part), ItemStack.EMPTY);
        }
        setStack(CORE_SLOT, ItemStack.EMPTY);
        this.pickedTraits = 0;
        if (!this.golemName.isBlank()) {
            golem.set(DataComponents.CUSTOM_NAME, Component.literal(this.golemName.trim()));
            this.golemName = "";
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
        return GolemData.combine(contributions(), appearanceSeed).withTraits(chosenTraits()).capped(tier());
    }

    private TraitView traitView() {
        GolemData data = preview();
        if (data == null) {
            return TraitView.HIDDEN;
        }
        if (getStack(partSlot(GolemPart.TORSO)).getItem() instanceof GolemItem) {
            return new TraitView(true, false, data.traits(), List.of(), Set.of(), data.traitSlots());
        }
        List<GolemTrait> chosen = chosenTraits();
        List<GolemTrait> pool = new ArrayList<>(availableTraits());
        pool.removeAll(chosen);
        return new TraitView(true, true, chosen, pool, lockedTraits(), traitSlots());
    }

    private record TraitView(boolean visible, boolean editable, List<GolemTrait> picked, List<GolemTrait> pool, Set<GolemTrait> locked, int slots) {
        private static final TraitView HIDDEN = new TraitView(false, false, List.of(), List.of(), Set.of(), 0);

        private boolean isLocked(GolemTrait trait) {
            return this.locked.contains(trait);
        }

        private boolean missesLocked() {
            return this.editable && !this.picked.containsAll(this.locked);
        }
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
        return hasCore() ? combineParts(0L) : null;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement parts = column(2).addChildren(
                row(2).addChildren(spacer(), partSlotElement(GolemPart.HEAD), coreSlotElement()),
                row(2).addChildren(partSlotElement(GolemPart.HAND_LEFT), partSlotElement(GolemPart.TORSO), partSlotElement(GolemPart.HAND_RIGHT)),
                partSlotElement(GolemPart.LEGS),
                new TextField().setAnyString()
                        .bind(DataBindingBuilder.string(() -> this.golemName, this::setGolemName).build())
                        .layout(layout -> layout.width(NAME_FIELD_WIDTH))
                        .style(style -> style.tooltips(Component.translatable("gui.crystallomachia.golem_name"))),
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

    private UIElement coreSlotElement() {
        return new ItemSlot().bind(this.inventory, CORE_SLOT)
                .style(style -> style.tooltips(Component.translatable("golem_part.crystallomachia.core")));
    }

    private static UIElement spacer() {
        return new UIElement().layout(layout -> layout.width(SLOT_SIZE).height(SLOT_SIZE));
    }

    private UIElement createPreview() {
        ScrollerView scroller = new ScrollerView();
        scroller.layout(layout -> layout.widthPercent(100).heightPercent(100));
        scroller.addScrollViewChild(new Label().setText(Component.translatable("gui.crystallomachia.preview").withStyle(ChatFormatting.GRAY)));

        List<Runnable> updates = new ArrayList<>();
        UIElement tierLabel = new Label().bindDataSource(SupplierDataSource.of(
                () -> Component.translatable("tooltip.crystallomachia.tier", previewTier().displayName()).withStyle(ChatFormatting.GRAY)));
        scroller.addScrollViewChild(tierLabel);
        updates.add(() -> tierLabel.setDisplay(preview() != null));
        UIElement stats = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).widthPercent(100));
        for (GolemStat stat : GolemStat.values()) {
            UIElement statIcon = new UIElement().layout(layout -> layout.width(GolemStat.ICON_SIZE).height(GolemStat.ICON_SIZE).flexShrink(0))
                    .style(style -> style.backgroundTexture(SpriteTexture.of(stat.icon().toString())).tooltips(stat.displayName(), stat.description()));
            UIElement value = new Label().bindDataSource(SupplierDataSource.of(() -> {
                GolemData data = preview();
                return Component.literal(data == null ? "0" : stat.format(data)).withStyle(ChatFormatting.WHITE);
            })).layout(layout -> layout.width(STAT_TEXT_WIDTH).flexShrink(0));
            stats.addChild(row(2).addChildren(statIcon, value)
                    .layout(layout -> layout.width(GolemStat.ICON_SIZE + 2 + STAT_TEXT_WIDTH).flexShrink(0)));
        }
        scroller.addScrollViewChild(stats);
        updates.add(() -> stats.setDisplay(preview() != null));

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
        addTraitSection(scroller, updates);
        updates.forEach(Runnable::run);

        UIElement panel = new UIElement().layout(layout -> layout.width(184).height(86).paddingAll(3))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF000000)));
        panel.addChild(scroller);
        panel.addEventListener(UIEvents.TICK, event -> updates.forEach(Runnable::run));
        return panel;
    }

    private void addTraitSection(ScrollerView scroller, List<Runnable> updates) {
        TraitView[] view = {traitView()};
        updates.add(() -> view[0] = traitView());
        RPCEmitter pick = scroller.addRPCEvent(RPCEventBuilder.simple(Integer.class, Boolean.class, this::setTraitPicked));

        UIElement slotsLabel = new Label().bindDataSource(SupplierDataSource.of(
                () -> Component.translatable("tooltip.crystallomachia.trait_slots", view[0].picked().size(), view[0].slots()).withStyle(ChatFormatting.GRAY)));
        UIElement pickedZone = traitZone();
        UIElement poolLabel = new Label().setText(Component.translatable("gui.crystallomachia.traits.available").withStyle(ChatFormatting.GRAY));
        UIElement poolZone = traitZone();
        UIElement lockedWarning = new Label().setText(Component.translatable("gui.crystallomachia.traits.locked_missing").withStyle(ChatFormatting.RED));

        for (GolemTrait trait : GolemTrait.values()) {
            UIElement pickedTile = traitTile(trait, view, updates);
            UIElement poolTile = traitTile(trait, view, updates);
            pickedZone.addChild(pickedTile);
            poolZone.addChild(poolTile);
            updates.add(() -> {
                pickedTile.setDisplay(view[0].picked().contains(trait));
                poolTile.setDisplay(view[0].pool().contains(trait));
            });
        }
        for (int index = 0; index < GolemData.MAX_TRAIT_SLOTS; index++) {
            int slot = index;
            UIElement empty = new UIElement().layout(layout -> layout.width(TRAIT_TILE_HEIGHT).height(TRAIT_TILE_HEIGHT).flexShrink(0))
                    .style(style -> style.backgroundTexture(EMPTY_TRAIT_SLOT));
            pickedZone.addChild(empty);
            updates.add(() -> empty.setDisplay(slot < view[0].slots() - view[0].picked().size()));
        }
        pickedZone.addEventListener(UIEvents.DRAG_PERFORM, event -> {
            if (event.dragHandler.getDraggingObject() instanceof GolemTrait trait) {
                pick.send(trait.ordinal(), true);
            }
        });
        poolZone.addEventListener(UIEvents.DRAG_PERFORM, event -> {
            if (event.dragHandler.getDraggingObject() instanceof GolemTrait trait) {
                pick.send(trait.ordinal(), false);
            }
        });

        scroller.addScrollViewChild(slotsLabel);
        scroller.addScrollViewChild(pickedZone);
        scroller.addScrollViewChild(poolLabel);
        scroller.addScrollViewChild(poolZone);
        scroller.addScrollViewChild(lockedWarning);
        updates.add(() -> {
            boolean pooled = view[0].editable() && !(view[0].picked().isEmpty() && view[0].pool().isEmpty());
            slotsLabel.setDisplay(view[0].visible());
            pickedZone.setDisplay(view[0].visible());
            poolLabel.setDisplay(pooled);
            poolZone.setDisplay(pooled);
            lockedWarning.setDisplay(view[0].missesLocked());
        });
    }

    private static UIElement traitZone() {
        return new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).alignItems(AlignItems.CENTER)
                .widthPercent(100).minHeight(TRAIT_TILE_HEIGHT + 2).paddingAll(1).gapAll(2));
    }

    private UIElement traitTile(GolemTrait trait, TraitView[] view, List<Runnable> updates) {
        UIElement name = new Label().bindDataSource(SupplierDataSource.of(
                () -> trait.displayName().copy().withStyle(view[0].isLocked(trait) ? ChatFormatting.RED : ChatFormatting.GOLD)));
        UIElement tile = row(2).addChildren(icon(trait.icon().toString()), name)
                .layout(layout -> layout.height(TRAIT_TILE_HEIGHT).paddingAll(2).flexShrink(0))
                .style(style -> style.backgroundTexture(TRAIT_TILE));
        boolean[] shownLocked = {false};
        updates.add(() -> {
            boolean locked = view[0].isLocked(trait);
            if (locked != shownLocked[0]) {
                shownLocked[0] = locked;
                tile.style(style -> style.backgroundTexture(locked ? LOCKED_TRAIT_TILE : TRAIT_TILE));
            }
        });
        UIEventListener tooltip = event -> {
            List<Component> lines = new ArrayList<>();
            lines.add(trait.displayName().copy().withStyle(view[0].isLocked(trait) ? ChatFormatting.RED : ChatFormatting.GOLD));
            lines.add(trait.description().copy().withStyle(ChatFormatting.GRAY));
            if (view[0].isLocked(trait)) {
                lines.add(Component.translatable("gui.crystallomachia.traits.locked").withStyle(ChatFormatting.RED));
            } else if (view[0].editable()) {
                lines.add(Component.translatable("gui.crystallomachia.traits.drag").withStyle(ChatFormatting.DARK_GRAY));
            }
            event.hoverTooltips = HoverTooltips.create(lines.toArray());
        };
        tile.addEventListener(UIEvents.HOVER_TOOLTIPS, tooltip);
        tile.getChildren().forEach(child -> child.addEventListener(UIEvents.HOVER_TOOLTIPS, tooltip));
        tile.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0 && view[0].editable()) {
                tile.startDrag(trait, SpriteTexture.of(trait.icon().toString()))
                        .setDragTexture(-GolemCapability.ICON_SIZE / 2f, -GolemCapability.ICON_SIZE / 2f, GolemCapability.ICON_SIZE, GolemCapability.ICON_SIZE);
            }
        });
        return tile;
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
