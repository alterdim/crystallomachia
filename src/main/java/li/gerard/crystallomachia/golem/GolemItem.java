package li.gerard.crystallomachia.golem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GolemItem extends Item {
    public GolemItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack create(GolemData data) {
        ItemStack stack = new ItemStack(crystallomachia.GOLEM_ITEM.get());
        stack.set(crystallomachia.GOLEM_DATA.get(), data);
        return stack;
    }

    public static GolemData getData(ItemStack stack) {
        return stack.getOrDefault(crystallomachia.GOLEM_DATA.get(), GolemData.EMPTY);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        GolemData data = getData(stack);
        List<CapabilityTooltip.Line> lines = new ArrayList<>();
        for (GolemCapability capability : GolemCapability.values()) {
            int level = data.level(capability);
            if (level > 0) {
                lines.add(new CapabilityTooltip.Line(capability, Component.literal(Integer.toString(level)).withStyle(ChatFormatting.GRAY)));
            }
        }
        return lines.isEmpty() ? Optional.empty() : Optional.of(new CapabilityTooltip(lines));
    }
}
