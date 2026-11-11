package li.gerard.crystallomachia.client;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import li.gerard.crystallomachia.block.AbstractWorkstationBlockEntity;
import li.gerard.crystallomachia.golem.GolemData;
import net.minecraft.world.phys.Vec3;

public class GolemVisual implements GeoAnimatable {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WORK = RawAnimation.begin().thenLoop("work_heat");
    private static final Map<AbstractWorkstationBlockEntity, GolemVisual[]> VISUALS = new WeakHashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);
    private final Set<String> trackedBones = ConcurrentHashMap.newKeySet();
    private final Map<String, Vec3> boneLocalPositions = new ConcurrentHashMap<>();
    private GolemData data = GolemData.EMPTY;
    private boolean working;
    private Vec3 worldPosition = Vec3.ZERO;
    private float yaw;

    public static GolemVisual get(AbstractWorkstationBlockEntity workstation, int slot) {
        GolemVisual[] slots = VISUALS.computeIfAbsent(workstation, key -> new GolemVisual[key.golemSlots()]);
        if (slots[slot] == null) {
            slots[slot] = new GolemVisual();
        }
        return slots[slot];
    }

    public void update(GolemData data, boolean working, Vec3 worldPosition, float yaw) {
        this.data = data;
        this.working = working;
        this.worldPosition = worldPosition;
        this.yaw = yaw;
    }

    public GolemData data() {
        return this.data;
    }

    public boolean isWorking() {
        return this.working;
    }

    public Set<String> trackedBones() {
        return this.trackedBones;
    }

    public void setBoneLocalPosition(String bone, @Nullable Vec3 localPosition) {
        if (localPosition != null) {
            this.boneLocalPositions.put(bone, localPosition);
        }
    }

    public @Nullable Vec3 boneWorldPosition(String bone) {
        this.trackedBones.add(bone);
        Vec3 local = this.boneLocalPositions.get(bone);
        if (local == null) {
            return null;
        }
        return this.worldPosition.add(local.yRot((float) Math.toRadians(this.yaw)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GolemVisual>("main", 5,
                test -> test.setAndContinue(test.animatable().isWorking() ? WORK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
