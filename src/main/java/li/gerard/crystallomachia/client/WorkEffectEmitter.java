package li.gerard.crystallomachia.client;

import li.gerard.crystallomachia.block.AbstractWorkstationBlockEntity;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.WorkEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class WorkEffectEmitter {
    private WorkEffectEmitter() {}

    public static void tick(AbstractWorkstationBlockEntity workstation) {
        Level level = workstation.getLevel();
        if (level == null || !workstation.isWorking()) {
            return;
        }
        long time = level.getGameTime();
        for (int slot = 0; slot < workstation.golemSlots(); slot++) {
            if (!(workstation.getStack(slot).getItem() instanceof GolemItem)) {
                continue;
            }
            for (WorkEffect effect : workstation.workEffects(slot)) {
                if (effect.particles() != null && time % Math.max(1, effect.particles().interval()) == 0) {
                    spawnParticles(workstation, slot, effect.particles(), level.getRandom());
                }
                if (effect.sound() != null && time % Math.max(1, effect.sound().interval()) == 0) {
                    WorkEffect.Sound sound = effect.sound();
                    Vec3 at = resolve(workstation, slot, sound.at());
                    level.playLocalSound(at.x, at.y, at.z, sound.sound(), SoundSource.BLOCKS, sound.volume(), sound.pitch(), false);
                }
            }
        }
    }

    private static Vec3 resolve(AbstractWorkstationBlockEntity workstation, int slot, WorkEffect.Point point) {
        if (point.origin() == WorkEffect.Origin.BONE && point.bone() != null) {
            Vec3 bone = GolemVisual.get(workstation, slot).boneWorldPosition(point.bone());
            return bone != null ? bone : workstation.effectPosition(slot, point.fallback());
        }
        return workstation.effectPosition(slot, point);
    }

    private static void spawnParticles(AbstractWorkstationBlockEntity workstation, int slot, WorkEffect.Particles particles, RandomSource random) {
        Vec3 from = resolve(workstation, slot, particles.from());
        Vec3 to = resolve(workstation, slot, particles.to());
        int lifetime = Math.max(1, particles.lifetime());
        float friction = particles.friction();
        double factor = friction >= 1f ? 1.0 / lifetime : (1.0 - friction) / (1.0 - Math.pow(friction, lifetime));
        Vec3 velocity = to.subtract(from).scale(factor);
        double spread = particles.spread();
        for (int i = 0; i < particles.count(); i++) {
            double x = from.x + (random.nextDouble() - 0.5) * 2 * spread;
            double y = from.y + (random.nextDouble() - 0.5) * 2 * spread;
            double z = from.z + (random.nextDouble() - 0.5) * 2 * spread;
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(particles.particle(), x, y, z, velocity.x, velocity.y, velocity.z);
            if (particle != null) {
                particle.setLifetime(lifetime);
            }
        }
    }
}
