package li.gerard.crystallomachia.golem;

import org.jspecify.annotations.Nullable;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public enum CrudeProduct {
    CORE(null, 9),
    TORSO(GolemPart.TORSO, 8),
    LEGS(GolemPart.LEGS, 7),
    HAND_LEFT(GolemPart.HAND_LEFT, 4),
    HAND_RIGHT(GolemPart.HAND_RIGHT, 4),
    HEAD(GolemPart.HEAD, 5);

    private static final int WORK_PER_MATERIAL = 40;

    private final @Nullable GolemPart part;
    private final int cost;

    CrudeProduct(@Nullable GolemPart part, int cost) {
        this.part = part;
        this.cost = cost;
    }

    public int cost() {
        return this.cost;
    }

    public int work() {
        return this.cost * WORK_PER_MATERIAL;
    }

    public Component displayName() {
        return this.part == null ? Component.translatable("golem_part.crystallomachia.core") : this.part.displayName();
    }

    public ItemStack create(Holder.Reference<GolemMaterial> material) {
        if (this.part != null) {
            return GolemPartItem.forPart(this.part).create(material, GolemTier.CRUDE);
        }
        GolemMaterial value = material.value();
        ItemStack core = GolemCoreItem.create(GolemCoreItem.stats(value.speed(), value.health(), value.attack(), value.armor(), value.stomach(), value.efficiency()));
        core.set(crystallomachia.GOLEM_TIER.get(), GolemTier.CRUDE);
        return core;
    }
}
