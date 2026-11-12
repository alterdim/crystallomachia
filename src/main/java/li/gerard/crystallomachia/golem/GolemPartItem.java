package li.gerard.crystallomachia.golem;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GolemPartItem extends Item {
    private final GolemPart part;

    public GolemPartItem(GolemPart part, Item.Properties properties) {
        super(properties.stacksTo(1));
        this.part = part;
    }

    public GolemPart part() {
        return this.part;
    }

    public static GolemPartItem forPart(GolemPart part) {
        return switch (part) {
            case HEAD -> crystallomachia.GOLEM_HEAD.get();
            case TORSO -> crystallomachia.GOLEM_TORSO.get();
            case HAND_LEFT -> crystallomachia.GOLEM_HAND_LEFT.get();
            case HAND_RIGHT -> crystallomachia.GOLEM_HAND_RIGHT.get();
            case LEGS -> crystallomachia.GOLEM_LEGS.get();
        };
    }

    public ItemStack create(Holder.Reference<GolemMaterial> material) {
        ItemStack stack = new ItemStack(this);
        stack.set(crystallomachia.GOLEM_MATERIAL.get(), material.key());
        stack.set(crystallomachia.GOLEM_DATA.get(), material.value().contribution(this.part));
        return stack;
    }

    public ItemStack create(Holder.Reference<GolemMaterial> material, GolemTier tier) {
        ItemStack stack = create(material);
        stack.set(crystallomachia.GOLEM_DATA.get(), material.value().contribution(this.part).capped(tier));
        stack.set(crystallomachia.GOLEM_TIER.get(), tier);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceKey<GolemMaterial> material = stack.get(crystallomachia.GOLEM_MATERIAL.get());
        if (material == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.crystallomachia.golem_part.named", GolemMaterial.displayName(material), super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        GolemItem.appendSpeed(stack, builder, flag);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return GolemItem.capabilityTooltip(stack);
    }
}
