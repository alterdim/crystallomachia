package li.gerard.crystallomachia.client;

import org.jspecify.annotations.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;

public class GolemRenderer extends GeoObjectRenderer<GolemVisual, Void, GeoRenderState> {
    public static final DataTicket<GolemData> GOLEM_DATA = DataTicket.create("crystallomachia_golem_data", GolemData.class);
    private static final int MAX_MODULE_LEVEL = 5;
    private static final float SCALE = 1.2f;

    public GolemRenderer() {
        super(new GolemModel());
        withScale(SCALE);
    }

    @Override
    public void addRenderData(GolemVisual animatable, @Nullable Void relatedObject, GeoRenderState renderState, float partialTick) {
        renderState.addGeckolibData(GOLEM_DATA, animatable.data());
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
        GolemData data = renderPassInfo.getOrDefaultGeckolibData(GOLEM_DATA, GolemData.EMPTY);
        for (GolemCapability capability : GolemCapability.values()) {
            int level = data.level(capability);
            for (int module = 1; module <= MAX_MODULE_LEVEL; module++) {
                boolean hidden = module > level;
                snapshots.ifPresent(capability.getSerializedName() + "_" + module,
                        bone -> bone.skipRender(hidden).skipChildrenRender(hidden));
            }
        }
    }
}
