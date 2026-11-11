package li.gerard.crystallomachia.golem;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public record WorkEffect(@Nullable Particles particles, @Nullable Sound sound) {
    public static final WorkEffect HEAT = new WorkEffect(
            new Particles(ParticleTypes.FLAME, Point.bone("heat_1"), Point.workstation(0, 0.5, 0), 2, 3, 12, 0.15, 0.96f),
            new Sound(SoundEvents.FIRE_AMBIENT, Point.workstation(0, 0.5, 0), 40, 0.6f, 1.0f));

    public static List<WorkEffect> forCapability(GolemCapability capability) {
        return switch (capability) {
            case HEAT -> List.of(HEAT);
            default -> List.of();
        };
    }

    public enum Origin {
        GOLEM,
        WORKSTATION,
        BONE
    }

    public record Point(Origin origin, @Nullable String bone, double right, double up, double front) {
        public static Point golem(double right, double up, double front) {
            return new Point(Origin.GOLEM, null, right, up, front);
        }

        public static Point workstation(double right, double up, double front) {
            return new Point(Origin.WORKSTATION, null, right, up, front);
        }

        public static Point bone(String bone) {
            return new Point(Origin.BONE, bone, 0, 0, 0);
        }

        public Point fallback() {
            return golem(0, 1.0, 0);
        }
    }

    public record Particles(ParticleOptions particle, Point from, Point to, int interval, int count, int lifetime, double spread, float friction) {}

    public record Sound(SoundEvent sound, Point at, int interval, float volume, float pitch) {}
}
