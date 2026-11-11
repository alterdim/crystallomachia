package li.gerard.crystallomachia.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import li.gerard.crystallomachia.entity.GolemEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class GolemEntityRenderer extends EntityRenderer<GolemEntity, GolemEntityRenderState> {
    private final GolemRenderer golemRenderer = new GolemRenderer();
    private final Map<GolemEntity, GolemVisual> visuals = new WeakHashMap<>();

    public GolemEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.9f;
    }

    @Override
    public GolemEntityRenderState createRenderState() {
        return new GolemEntityRenderState();
    }

    @Override
    public void extractRenderState(GolemEntity entity, GolemEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.bodyYaw = entity.getPreciseBodyRotation(partialTick);
        state.passes.clear();
        GolemVisual visual = this.visuals.computeIfAbsent(entity, key -> new GolemVisual());
        visual.update(entity.golemData(), entity.isAggressive(), entity.position(), 180f - state.bodyYaw);
        for (GolemPass pass : GolemPass.forParts(entity.parts())) {
            GeoRenderState golemState = this.golemRenderer.fillRenderState(visual, null, this.golemRenderer.createRenderState(visual, null), partialTick);
            golemState.addGeckolibData(DataTickets.PACKED_LIGHT, state.lightCoords);
            golemState.addGeckolibData(DataTickets.RENDER_COLOR, pass.color());
            golemState.addGeckolibData(GolemRenderer.PASS, pass);
            state.passes.add(golemState);
        }
    }

    @Override
    public void submit(GolemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180f - state.bodyYaw);
        for (GeoRenderState pass : state.passes) {
            this.golemRenderer.performRenderPass(pass, poseStack, collector, camera);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
