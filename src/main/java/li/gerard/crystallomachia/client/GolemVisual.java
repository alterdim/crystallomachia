package li.gerard.crystallomachia.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import li.gerard.crystallomachia.golem.GolemData;

public class GolemVisual implements GeoAnimatable {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WORK = RawAnimation.begin().thenLoop("work_heat");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);
    private GolemData data = GolemData.EMPTY;
    private boolean working;

    public void update(GolemData data, boolean working) {
        this.data = data;
        this.working = working;
    }

    public GolemData data() {
        return this.data;
    }

    public boolean isWorking() {
        return this.working;
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
