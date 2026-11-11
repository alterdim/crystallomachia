package li.gerard.crystallomachia.golem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
        ItemStack stack = new ItemStack(crystallomachia.GOLEM_ITEM.get());
        stack.set(crystallomachia.GOLEM_DATA.get(), data);
        if (!parts.isEmpty()) {
            stack.set(crystallomachia.GOLEM_PARTS.get(), parts);
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
        GolemParts parts = getParts(stack);
        for (GolemPart part : GolemPart.values()) {
            ResourceKey<GolemMaterial> material = parts.material(part);
            if (material != null) {
                builder.accept(Component.translatable("tooltip.crystallomachia.golem_part", part.displayName(), GolemMaterial.displayName(material))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static boolean appendSpeed(ItemStack stack, Consumer<Component> builder, TooltipFlag flag) {
        if (!stack.has(crystallomachia.GOLEM_DATA.get())) {
            return false;
        }
        if (!flag.hasShiftDown()) {
            builder.accept(Component.translatable("tooltip.crystallomachia.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            return false;
        }
        builder.accept(Component.translatable("tooltip.crystallomachia.speed", GolemData.formatSpeed(getData(stack).speed()))
                .withStyle(ChatFormatting.GRAY));
        return true;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return capabilityTooltip(getData(stack));
    }

    public static Optional<TooltipComponent> capabilityTooltip(GolemData data) {
        List<CapabilityTooltip.Line> lines = new ArrayList<>();
        for (GolemCapability capability : GolemCapability.values()) {
            int level = data.level(capability);
            if (level > 0) {
                lines.add(new CapabilityTooltip.Line(capability, Component.literal(Integer.toString(level)).withStyle(ChatFormatting.GRAY)));
            }
        }
        for (GolemTrait trait : data.traits()) {
            lines.add(new CapabilityTooltip.Line(trait, trait.displayName().copy().withStyle(ChatFormatting.GOLD)));
        }
        return lines.isEmpty() ? Optional.empty() : Optional.of(new CapabilityTooltip(lines));
    }
}
