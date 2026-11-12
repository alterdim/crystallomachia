package li.gerard.crystallomachia.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import li.gerard.crystallomachia.block.DancefloorBlock;
import li.gerard.crystallomachia.block.DiscoBallBlockEntity;
import li.gerard.crystallomachia.golem.GolemItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DiscoBallRenderer implements BlockEntityRenderer<DiscoBallBlockEntity, WorkstationRenderState> {
    private static final double RING_RADIUS = 2.5;
    private static final float GOLEM_HEIGHT = 4f;

    private final GolemRenderer golemRenderer = new GolemRenderer();

    public DiscoBallRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public WorkstationRenderState createRenderState() {
        return new WorkstationRenderState();
    }

    @Override
    public void extractRenderState(DiscoBallBlockEntity discoBall, WorkstationRenderState state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(discoBall, state, partialTick, cameraPos, breakProgress);
        state.golems.clear();
        Level level = discoBall.getLevel();
        int floorDistance = discoBall.floorDistance();
        if (level == null || floorDistance == 0) {
            return;
        }
        BlockPos pos = discoBall.getBlockPos();
        double y = 1 - floorDistance;
        for (int slot = 0; slot < DiscoBallBlockEntity.GOLEM_SLOTS; slot++) {
            ItemStack stack = discoBall.getStack(slot);
            if (!(stack.getItem() instanceof GolemItem)) {
                continue;
            }
            double angle = 2 * Math.PI * slot / DiscoBallBlockEntity.GOLEM_SLOTS;
            double offsetX = RING_RADIUS * Math.sin(angle);
            double offsetZ = RING_RADIUS * Math.cos(angle);
            double x = 0.5 + offsetX;
            double z = 0.5 + offsetZ;
            float yaw = (float) Math.toDegrees(Math.atan2(offsetX, offsetZ));
            Vec3 worldPos = new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z);

            GolemVisual visual = GolemVisual.get(discoBall, DiscoBallBlockEntity.GOLEM_SLOTS, slot);
            visual.update(discoBall.golem(slot), false, worldPos, yaw);

            int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(worldPos));
            List<GeoRenderState> passes = new ArrayList<>();
            for (GolemPass pass : GolemPass.forParts(GolemItem.getParts(stack))) {
                GeoRenderState golemState = this.golemRenderer.fillRenderState(visual, null, this.golemRenderer.createRenderState(visual, null), partialTick);
                golemState.addGeckolibData(DataTickets.PACKED_LIGHT, light);
                golemState.addGeckolibData(DataTickets.RENDER_COLOR, pass.color());
                golemState.addGeckolibData(GolemRenderer.PASS, pass);
                passes.add(golemState);
            }
            state.golems.add(new WorkstationRenderState.Golem(passes, x, y, z, yaw));
        }
    }

    @Override
    public void submit(WorkstationRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (WorkstationRenderState.Golem golem : state.golems) {
            poseStack.pushPose();
            poseStack.translate(golem.x(), golem.y(), golem.z());
            poseStack.rotateDegrees(Axis.YP, golem.yaw());
            for (GeoRenderState pass : golem.passes()) {
                this.golemRenderer.performRenderPass(pass, poseStack, collector, camera);
            }
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(DiscoBallBlockEntity discoBall) {
        return new AABB(discoBall.getBlockPos())
                .inflate(DancefloorBlock.RADIUS + 1, GOLEM_HEIGHT, DancefloorBlock.RADIUS + 1)
                .expandTowards(0, -DancefloorBlock.MAX_BALL_HEIGHT, 0);
    }
}
