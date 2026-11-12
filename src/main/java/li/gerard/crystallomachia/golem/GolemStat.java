package li.gerard.crystallomachia.golem;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public enum GolemStat {
    SPEED("textures/mob_effect/speed.png", GolemData::speed),
    INTELLIGENCE("textures/item/book.png", GolemData::intelligence),
    HEALTH("textures/gui/sprites/hud/heart/full.png", data -> data.combat().health()),
    ATTACK("textures/mob_effect/strength.png", data -> data.combat().attack()),
    ARMOR("textures/gui/sprites/hud/armor_full.png", data -> data.combat().armor()),
    STOMACH("textures/gui/sprites/hud/food_full.png", data -> data.appetite().stomach()),
    EFFICIENCY("textures/mob_effect/haste.png", data -> data.appetite().efficiency());

    public static final int ICON_SIZE = 9;

    private final Identifier icon;
    private final Function<GolemData, Float> getter;

    GolemStat(String icon, Function<GolemData, Float> getter) {
        this.icon = Identifier.withDefaultNamespace(icon);
        this.getter = getter;
    }

    public static GolemStat[] coreStats() {
        return Arrays.stream(values()).filter(stat -> stat != INTELLIGENCE).toArray(GolemStat[]::new);
    }

    public Identifier icon() {
        return this.icon;
    }

    public float value(GolemData data) {
        return this.getter.apply(data);
    }

    public String format(GolemData data) {
        return GolemData.formatStat(value(data));
    }

    public Component displayName() {
        return Component.translatable("stat.crystallomachia." + name().toLowerCase(Locale.ROOT));
    }

    public Component description() {
        return Component.translatable("stat.crystallomachia." + name().toLowerCase(Locale.ROOT) + ".description").withStyle(ChatFormatting.GRAY);
    }
}
