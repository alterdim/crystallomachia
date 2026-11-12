package li.gerard.crystallomachia.golem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class GolemItem extends Item {
    public GolemItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack create(GolemData data) {
        return create(data, GolemParts.EMPTY);
    }

    public static ItemStack create(GolemData data, GolemParts parts) {
        return create(data, parts, null);
    }

    public static ItemStack create(GolemData data, GolemParts parts, @Nullable GolemData core) {
        ItemStack stack = new ItemStack(crystallomachia.GOLEM_ITEM.get());
        stack.set(crystallomachia.GOLEM_DATA.get(), data);
        if (!parts.isEmpty()) {
            stack.set(crystallomachia.GOLEM_PARTS.get(), parts);
        }
        if (core != null) {
            stack.set(crystallomachia.GOLEM_CORE_DATA.get(), core);
        }
        return stack;
    }

    public static ItemStack createFromMaterials(Map<GolemPart, Holder.Reference<GolemMaterial>> materials, long appearanceSeed) {
        List<GolemData> contributions = new ArrayList<>();
        Map<GolemPart, ResourceKey<GolemMaterial>> parts = new EnumMap<>(GolemPart.class);
        materials.forEach((part, material) -> {
            parts.put(part, material.key());
            contributions.add(material.value().contribution(part));
        });
        return create(GolemData.combine(contributions, appearanceSeed), new GolemParts(parts));
    }

    public static GolemData getData(ItemStack stack) {
        return stack.getOrDefault(crystallomachia.GOLEM_DATA.get(), GolemData.EMPTY);
    }

    public static GolemParts getParts(ItemStack stack) {
        return stack.getOrDefault(crystallomachia.GOLEM_PARTS.get(), GolemParts.EMPTY);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        if (!appendSpeed(stack, builder, flag)) {
            return;
        }
        GolemData data = getData(stack);
        builder.accept(Component.translatable("tooltip.crystallomachia.trait_slots", data.traits().size(), data.traitSlots()).withStyle(ChatFormatting.GRAY));
        GolemHunger hunger = GolemHunger.of(stack);
        builder.accept(Component.translatable("tooltip.crystallomachia.food", GolemData.formatStat(hunger.food()), GolemData.formatStat(GolemHunger.stomach(stack)))
                .withStyle(hunger.isFed() ? ChatFormatting.GRAY : ChatFormatting.RED));
        if (hunger.boostTicks() > 0) {
            builder.accept(Component.translatable("tooltip.crystallomachia.golem_food.boost",
                    GolemData.formatStat((hunger.boost() - 1f) * 100f), GolemData.formatStat(hunger.boostTicks() / 20f)).withStyle(ChatFormatting.GOLD));
        }
        if (!flag.hasControlDown()) {
            return;
        }
        GolemParts parts = getParts(stack);
        for (GolemPart part : GolemPart.values()) {
            ResourceKey<GolemMaterial> material = parts.material(part);
            if (material != null) {
                builder.accept(Component.translatable("tooltip.crystallomachia.golem_part", part.displayName(), GolemMaterial.displayName(material))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack golem, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !(other.getItem() instanceof GolemFoodItem meal) || !golem.has(crystallomachia.GOLEM_DATA.get())) {
            return false;
        }
        if (GolemHunger.feed(golem, meal)) {
            other.shrink(1);
            if (player.level() instanceof ServerLevel level) {
                GolemHunger.playEatEffects(level, player.position().add(0, 1, 0));
            }
        }
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(crystallomachia.GOLEM_DATA.get()) && GolemHunger.of(stack).food() < GolemHunger.stomach(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float stomach = GolemHunger.stomach(stack);
        return stomach <= 0f ? 0 : Math.round(13f * Math.min(1f, GolemHunger.of(stack).food() / stomach));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFC8782D;
    }

    public static boolean appendSpeed(ItemStack stack, Consumer<Component> builder, TooltipFlag flag) {
        if (!stack.has(crystallomachia.GOLEM_DATA.get())) {
            return false;
        }
        if (!flag.hasShiftDown()) {
            builder.accept(Component.translatable("tooltip.crystallomachia.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            return false;
        }
        GolemTier tier = stack.get(crystallomachia.GOLEM_TIER.get());
        if (tier != null) {
            builder.accept(Component.translatable("tooltip.crystallomachia.tier", tier.displayName()).withStyle(ChatFormatting.GRAY));
        }
        return true;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return capabilityTooltip(stack);
    }

    private static boolean showsIntelligence(ItemStack stack) {
        return stack.getItem() instanceof GolemItem || stack.getItem() instanceof GolemPartItem partItem && partItem.part() == GolemPart.HEAD;
    }

    public static Optional<TooltipComponent> capabilityTooltip(ItemStack stack) {
        if (!stack.has(crystallomachia.GOLEM_DATA.get())) {
            return Optional.empty();
        }
        GolemData data = getData(stack);
        List<CapabilityTooltip.Line> stats = new ArrayList<>();
        for (GolemStat stat : GolemStat.values()) {
            if (stat == GolemStat.INTELLIGENCE && !showsIntelligence(stack)) {
                continue;
            }
            stats.add(new CapabilityTooltip.Line(stat.icon(), Component.literal(stat.format(data)).withStyle(ChatFormatting.GRAY)));
        }
        List<CapabilityTooltip.Line> capabilities = new ArrayList<>();
        List<CapabilityTooltip.Line> lines = new ArrayList<>();
        for (GolemCapability capability : GolemCapability.values()) {
            int level = data.level(capability);
            if (level > 0) {
                capabilities.add(new CapabilityTooltip.Line(capability, Component.literal(Integer.toString(level)).withStyle(ChatFormatting.GRAY)));
            }
        }
        for (GolemTrait trait : data.traits()) {
            lines.add(new CapabilityTooltip.Line(trait, trait.displayName().copy().withStyle(ChatFormatting.GOLD)));
        }
        return Optional.of(new CapabilityTooltip(stats, capabilities, lines));
    }
}
