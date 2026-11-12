package li.gerard.crystallomachia.golem;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class GolemFoodItem extends Item {
    private final float nourishment;
    private final float boost;
    private final int boostTicks;

    public GolemFoodItem(float nourishment, float boost, int boostTicks, Item.Properties properties) {
        super(properties);
        this.nourishment = nourishment;
        this.boost = boost;
        this.boostTicks = boostTicks;
    }

    public float nourishment() {
        return this.nourishment;
    }

    public float boost() {
        return this.boost;
    }

    public int boostTicks() {
        return this.boostTicks;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.crystallomachia.golem_food.nourishment", GolemData.formatStat(this.nourishment))
                .withStyle(ChatFormatting.GRAY));
        if (this.boostTicks > 0) {
            builder.accept(Component.translatable("tooltip.crystallomachia.golem_food.boost",
                    GolemData.formatStat((this.boost - 1f) * 100f), GolemData.formatStat(this.boostTicks / 20f)).withStyle(ChatFormatting.GOLD));
        }
        builder.accept(Component.translatable("tooltip.crystallomachia.golem_food.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
