package li.gerard.crystallomachia.client;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import li.gerard.crystallomachia.golem.GolemPart;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;
import net.minecraft.client.renderer.SubmitNodeCollector;

public class GolemRenderer extends GeoObjectRenderer<GolemVisual, Void, GeoRenderState> {
    public static final DataTicket<GolemData> GOLEM_DATA = DataTicket.create("crystallomachia_golem_data", GolemData.class);
    public static final DataTicket<GolemVisual> VISUAL = DataTicket.create("crystallomachia_golem_visual", GolemVisual.class);
    public static final DataTicket<GolemPass> PASS = DataTicket.create("crystallomachia_golem_pass", GolemPass.class);
    private static final Map<String, GolemPart> BONE_PARTS = Map.of(
            "head", GolemPart.HEAD,
            "body", GolemPart.TORSO,
            "arm_l", GolemPart.HAND_LEFT,
            "arm_r", GolemPart.HAND_RIGHT,
            "legs", GolemPart.LEGS);
    private static final int MAX_MODULE_LEVEL = 5;
    private static final float SCALE = 1.2f;

    public GolemRenderer() {
        super(new GolemModel());
        withScale(SCALE);
    }

    @Override
    public void addRenderData(GolemVisual animatable, @Nullable Void relatedObject, GeoRenderState renderState, float partialTick) {
        renderState.addGeckolibData(GOLEM_DATA, animatable.data());
        renderState.addGeckolibData(VISUAL, animatable);
    }

    @Override
    public void preRenderPass(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
        GolemVisual visual = renderPassInfo.getGeckolibData(VISUAL);
        if (visual == null) {
            return;
        }
        for (String bone : visual.trackedBones()) {
            renderPassInfo.addBonePositionListener(bone, (worldPos, modelPos, localPos) -> visual.setBoneLocalPosition(bone, localPos));
        }
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
        GolemPass pass = renderPassInfo.getGeckolibData(PASS);
        if (pass == null) {
            return;
        }
        for (GeoBone bone : renderPassInfo.model().boneLookup().get().values()) {
            GolemPart part = partOf(bone);
            if (part != null && !pass.parts().contains(part)) {
                snapshots.get(bone).skipRender(true);
            }
        }
    }

    @Override
    public Identifier getTextureLocation(GeoRenderState renderState) {
        GolemPass pass = renderState.getGeckolibData(PASS);
        return pass != null ? pass.texture() : GolemPass.DEFAULT_TEXTURE;
    }

    private static @Nullable GolemPart partOf(@Nullable GeoBone bone) {
        while (bone != null) {
            GolemPart part = BONE_PARTS.get(bone.name());
            if (part != null) {
                return part;
            }
            bone = bone.parent();
        }
        return null;
    }
}
