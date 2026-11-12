package li.gerard.crystallomachia.golem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public record GolemHunger(float food, float boost, int boostTicks) {
    public static final float FOOD_PER_WORK_TICK = 0.05f;
    private static final float MIN_EFFICIENCY = 0.01f;

    public static final Codec<GolemHunger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("food").forGetter(GolemHunger::food),
            Codec.FLOAT.optionalFieldOf("boost", 1f).forGetter(GolemHunger::boost),
            Codec.INT.optionalFieldOf("boost_ticks", 0).forGetter(GolemHunger::boostTicks)
    ).apply(instance, GolemHunger::new));

    public static final StreamCodec<ByteBuf, GolemHunger> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, GolemHunger::food,
            ByteBufCodecs.FLOAT, GolemHunger::boost,
            ByteBufCodecs.VAR_INT, GolemHunger::boostTicks,
            GolemHunger::new);

    public static GolemHunger of(ItemStack golem) {
        GolemHunger hunger = golem.get(crystallomachia.GOLEM_HUNGER.get());
        return hunger != null ? hunger : new GolemHunger(stomach(golem), 1f, 0);
    }

    public static float stomach(ItemStack golem) {
        return GolemItem.getData(golem).appetite().stomach();
    }

    public static float foodPerWorkTick(GolemData data) {
        return FOOD_PER_WORK_TICK / Math.max(MIN_EFFICIENCY, data.appetite().efficiency());
    }

    public static float workSpeed(ItemStack golem) {
        GolemHunger hunger = of(golem);
        return hunger.isFed() ? GolemItem.getData(golem).speed() * hunger.speedMultiplier() : 0f;
    }

    public static void work(ItemStack golem, int ticks) {
        GolemHunger hunger = of(golem);
        float food = Math.max(0f, hunger.food - ticks * foodPerWorkTick(GolemItem.getData(golem)));
        golem.set(crystallomachia.GOLEM_HUNGER.get(), new GolemHunger(food, hunger.boost, Math.max(0, hunger.boostTicks - ticks)));
    }

    public static boolean wantsMeal(ItemStack golem, GolemFoodItem meal) {
        if (!(golem.getItem() instanceof GolemItem) || !golem.has(crystallomachia.GOLEM_DATA.get())) {
            return false;
        }
        float stomach = stomach(golem);
        float missing = stomach - of(golem).food;
        return missing > 0f && missing >= Math.min(meal.nourishment(), stomach);
    }

    public static boolean feed(ItemStack golem, GolemFoodItem meal) {
        GolemHunger hunger = of(golem);
        float stomach = stomach(golem);
        if (hunger.food >= stomach) {
            return false;
        }
        boolean boosts = meal.boostTicks() > 0;
        golem.set(crystallomachia.GOLEM_HUNGER.get(), new GolemHunger(Math.min(stomach, hunger.food + meal.nourishment()),
                boosts ? meal.boost() : hunger.boost, boosts ? meal.boostTicks() : hunger.boostTicks));
        return true;
    }

    public static void playEatEffects(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, position.x, position.y, position.z, 12, 0.4, 0.6, 0.4, 0.0);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1f, 0.8f);
    }

    public boolean isFed() {
        return this.food > 0f;
    }

    public float speedMultiplier() {
        return this.boostTicks > 0 ? this.boost : 1f;
    }
}
