package li.gerard.crystallomachia.block;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.world.item.ItemStack;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.CoreExtraction;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemCoreItem;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.GolemStat;
import li.gerard.crystallomachia.golem.GolemTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class CoreExtractorBlockEntity extends AbstractWorkstationBlockEntity {
    public static final int INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int WORK = 400;
    public static final List<GolemRole> GOLEM_ROLES = List.of(
            GolemRole.of(GolemCapability.FREEZING, 1),
            GolemRole.of(GolemCapability.STRENGTH, 1));
    private static final int STAT_TEXT_WIDTH = 76;

    private ItemStack confirmedGolem = ItemStack.EMPTY;

    public CoreExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.CORE_EXTRACTOR_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, 2);
    }

    @Override
    protected boolean isItemValid(int slot, ItemResource resource) {
        if (slot == OUTPUT_SLOT) {
            return resource.getItem() instanceof GolemCoreItem;
        }
        return resource.getItem() instanceof GolemItem;
    }

    @Override
    public GolemAnchor golemAnchor(int slot) {
        return slot == 0 ? GolemAnchor.onLeft() : GolemAnchor.onRight();
    }

    public @Nullable CoreExtraction extraction() {
        return this.level == null ? null : CoreExtraction.of(getStack(INPUT_SLOT), this.level.registryAccess());
    }

    private boolean isConfirmed() {
        return !this.confirmedGolem.isEmpty() && ItemStack.matches(this.confirmedGolem, getStack(INPUT_SLOT));
    }

    public void confirm() {
        if (this.level != null && !this.level.isClientSide() && extraction() != null) {
            this.confirmedGolem = getStack(INPUT_SLOT).copy();
        }
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (!isConfirmed()) {
            this.confirmedGolem = ItemStack.EMPTY;
            resetProgress();
            return;
        }
        CoreExtraction extraction = workSpeed() <= 0 ? null : extraction();
        if (extraction == null || !getStack(OUTPUT_SLOT).isEmpty()) {
            resetProgress();
            return;
        }
        if (advanceProgress(WORK)) {
            this.confirmedGolem = ItemStack.EMPTY;
            consume(INPUT_SLOT, 1);
            setStack(OUTPUT_SLOT, GolemCoreItem.create(extraction.roll(level.getRandom())));
        }
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return new AbstractWorkstationUI<>(this) {
            @Override
            protected UIElement createMachineArea() {
                return column(4).addChildren(
                        row(4).addChildren(slot(INPUT_SLOT), progressArrow(), outputSlot(OUTPUT_SLOT),
                                new Button().setText(Component.translatable("gui.crystallomachia.extract")).setOnServerClick(event -> confirm())),
                        createPreview());
            }
        }.build(holder.player);
    }

    private UIElement createPreview() {
        ScrollerView scroller = new ScrollerView();
        scroller.layout(layout -> layout.widthPercent(100).heightPercent(100));
        scroller.addScrollViewChild(new Label().setText(Component.translatable("gui.crystallomachia.preview").withStyle(ChatFormatting.GRAY)));

        List<Runnable> updates = new ArrayList<>();
        GolemStat[] stats = GolemStat.values();
        for (int first = 0; first < stats.length; first += 2) {
            UIElement statRow = row(0);
            for (int index = first; index < Math.min(first + 2, stats.length); index++) {
                GolemStat stat = stats[index];
                UIElement statIcon = new UIElement().layout(layout -> layout.width(GolemStat.ICON_SIZE).height(GolemStat.ICON_SIZE).flexShrink(0))
                        .style(style -> style.backgroundTexture(SpriteTexture.of(stat.icon().toString())).tooltips(stat.displayName(), stat.description()));
                UIElement value = new Label().bindDataSource(SupplierDataSource.of(() -> {
                    CoreExtraction extraction = extraction();
                    return extraction == null ? Component.empty()
                            : range(GolemData.formatStat(extraction.min(stat)), GolemData.formatStat(extraction.max(stat))).withStyle(ChatFormatting.WHITE);
                })).layout(layout -> layout.width(STAT_TEXT_WIDTH).flexShrink(0));
                statRow.addChild(row(2).addChildren(statIcon, value)
                        .layout(layout -> layout.width(GolemStat.ICON_SIZE + 2 + STAT_TEXT_WIDTH).flexShrink(0)));
            }
            scroller.addScrollViewChild(statRow);
            updates.add(() -> statRow.setDisplay(extraction() != null));
        }

        Component capabilityHint = Component.translatable("gui.crystallomachia.extract.capability_hint", percent(CoreExtraction.CAPABILITY_CHANCE))
                .withStyle(ChatFormatting.GRAY);
        for (GolemCapability capability : GolemCapability.values()) {
            UIElement line = row(3).addChildren(
                    icon(capability.icon().toString()),
                    new Label().bindDataSource(SupplierDataSource.of(() -> {
                        CoreExtraction extraction = extraction();
                        if (extraction == null) {
                            return Component.empty();
                        }
                        int min = extraction.minLevel(capability);
                        int max = extraction.maxLevel(capability);
                        return range(Integer.toString(min), Integer.toString(max)).withStyle(ChatFormatting.WHITE);
                    })),
                    new Label().bindDataSource(SupplierDataSource.of(() -> {
                        CoreExtraction extraction = extraction();
                        float chance = extraction == null ? 0f : extraction.capabilityChance(capability);
                        return chance <= 0f ? Component.empty() : chance(chance);
                    })));
            tooltip(line, capability.displayName(), capabilityHint);
            scroller.addScrollViewChild(line);
            updates.add(() -> {
                CoreExtraction extraction = extraction();
                line.setDisplay(extraction != null && extraction.maxLevel(capability) > 0);
            });
        }

        Component traitHint = Component.translatable("gui.crystallomachia.extract.trait_hint", percent(CoreExtraction.TRAIT_CHANCE))
                .withStyle(ChatFormatting.GRAY);
        for (GolemTrait trait : GolemTrait.values()) {
            UIElement line = row(3).addChildren(
                    icon(trait.icon().toString()),
                    new Label().setText(trait.displayName().copy().withStyle(ChatFormatting.GOLD)),
                    new Label().bindDataSource(SupplierDataSource.of(() -> {
                        CoreExtraction extraction = extraction();
                        return chance(extraction == null ? 0f : extraction.traitChance(trait));
                    })));
            tooltip(line, trait.displayName().copy().withStyle(ChatFormatting.GOLD), trait.description().copy().withStyle(ChatFormatting.GRAY), traitHint);
            scroller.addScrollViewChild(line);
            updates.add(() -> {
                CoreExtraction extraction = extraction();
                line.setDisplay(extraction != null && extraction.traitChance(trait) > 0f);
            });
        }
        updates.forEach(Runnable::run);

        UIElement panel = new UIElement().layout(layout -> layout.width(184).height(86).paddingAll(3))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF000000)));
        panel.addChild(scroller);
        panel.addEventListener(UIEvents.TICK, event -> updates.forEach(Runnable::run));
        return panel;
    }

    private static net.minecraft.network.chat.MutableComponent range(String min, String max) {
        return min.equals(max) ? Component.literal(min) : Component.translatable("gui.crystallomachia.extract.range", min, max);
    }

    private static Component chance(float chance) {
        return Component.translatable("gui.crystallomachia.extract.chance", percent(chance)).withStyle(ChatFormatting.GRAY);
    }

    private static String percent(float chance) {
        return GolemData.formatStat(Math.round(chance * 1000f) / 10f);
    }

    private static void tooltip(UIElement line, Object... lines) {
        line.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(lines));
        line.getChildren().forEach(child -> child.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(lines)));
    }

    private static UIElement icon(String texture) {
        return new UIElement().layout(layout -> layout.width(GolemCapability.ICON_SIZE).height(GolemCapability.ICON_SIZE))
                .style(style -> style.backgroundTexture(SpriteTexture.of(texture)));
    }

    private static UIElement row(int gap) {
        return new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(gap));
    }
}
