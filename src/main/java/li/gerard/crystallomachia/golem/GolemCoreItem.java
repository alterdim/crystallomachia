package li.gerard.crystallomachia.golem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class GolemCoreItem extends Item {
    public static final float WILD_QUALITY = 0.3f;

    private static final float MIN_SPEED = 0.5f;
    private static final float MAX_SPEED = 3f;
    private static final float MIN_HEALTH = 2f;
    private static final float MAX_HEALTH = 12f;
    private static final float MIN_ATTACK = 0.5f;
    private static final float MAX_ATTACK = 4f;
    private static final float MIN_ARMOR = 0f;
    private static final float MAX_ARMOR = 4f;
    private static final float MIN_STOMACH = 10f;
    private static final float MAX_STOMACH = 60f;
    private static final float MIN_EFFICIENCY = 0.5f;
    private static final float MAX_EFFICIENCY = 2f;

    public GolemCoreItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack create(GolemData data) {
        ItemStack stack = new ItemStack(crystallomachia.GOLEM_CORE.get());
        stack.set(crystallomachia.GOLEM_DATA.get(), data);
        return stack;
    }

    public static GolemData random(RandomSource random, float maxQuality) {
        return stats(
                roll(random, MIN_SPEED, MAX_SPEED, maxQuality),
                roll(random, MIN_HEALTH, MAX_HEALTH, maxQuality),
                roll(random, MIN_ATTACK, MAX_ATTACK, maxQuality),
                roll(random, MIN_ARMOR, MAX_ARMOR, maxQuality),
                roll(random, MIN_STOMACH, MAX_STOMACH, maxQuality),
                roll(random, MIN_EFFICIENCY, MAX_EFFICIENCY, maxQuality));
    }

    public static GolemData wild(RandomSource random) {
        return random(random, WILD_QUALITY);
    }

    public static GolemData stats(float speed, float health, float attack, float armor, float stomach, float efficiency) {
        return new GolemData(Map.of(), List.of(), speed, 0f, new GolemCombat(health, attack, armor), new GolemAppetite(stomach, efficiency), 0L);
    }

    private static float roll(RandomSource random, float min, float max, float maxQuality) {
        return round(min + (max - min) * maxQuality * random.nextFloat());
    }

    public static float round(float value) {
        return Math.round(value * 100f) / 100f;
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
