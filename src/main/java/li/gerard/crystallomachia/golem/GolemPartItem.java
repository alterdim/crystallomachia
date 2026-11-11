package li.gerard.crystallomachia.golem;

import java.util.Optional;

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
        super(properties);
        this.part = part;
    }

    public GolemPart part() {
        return this.part;
    }

    public ItemStack create(Holder.Reference<GolemMaterial> material) {
        ItemStack stack = new ItemStack(this);
        stack.set(crystallomachia.GOLEM_MATERIAL.get(), material.key());
        stack.set(crystallomachia.GOLEM_DATA.get(), material.value().contribution(this.part));
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
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return GolemItem.capabilityTooltip(GolemItem.getData(stack));
    }
}
