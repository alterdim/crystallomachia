package li.gerard.crystallomachia.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import li.gerard.crystallomachia.block.AbstractWorkstationBlock;
import li.gerard.crystallomachia.block.AbstractWorkstationBlockEntity;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WorkstationRenderer implements BlockEntityRenderer<AbstractWorkstationBlockEntity, WorkstationRenderState> {
    private static final double WALK_BOB_SPEED = 0.6;
    private static final double WALK_BOB_HEIGHT = 0.08;

    private final GolemRenderer golemRenderer = new GolemRenderer();

    public WorkstationRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public WorkstationRenderState createRenderState() {
        return new WorkstationRenderState();
    }

    @Override
    public void extractRenderState(AbstractWorkstationBlockEntity workstation, WorkstationRenderState state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(workstation, state, partialTick, cameraPos, breakProgress);
        state.golems.clear();
        Level level = workstation.getLevel();
        if (level == null) {
            return;
        }
        float facingYaw = facingYaw(workstation.getBlockState().getValue(AbstractWorkstationBlock.FACING));
        double sin = Math.sin(Math.toRadians(facingYaw));
        double cos = Math.cos(Math.toRadians(facingYaw));
        BlockPos pos = workstation.getBlockPos();
        for (int slot = 0; slot < workstation.golemSlots(); slot++) {
            if (!(workstation.getStack(slot).getItem() instanceof GolemItem)) {
                continue;
            }
            GolemVisual visual = GolemVisual.get(workstation, slot);

            GolemAnchor anchor = workstation.golemAnchor(slot);
            double localX = anchor.right();
            double localZ = -anchor.front();
            double x = 0.5 + localX * cos + localZ * sin;
            double z = 0.5 - localX * sin + localZ * cos;
            double y = anchor.up();
            float yaw = facingYaw + anchor.yaw();
            GolemPose pose = workstation.golemPose(slot, partialTick);
            if (pose != null) {
                x = pose.position().x - pos.getX();
                z = pose.position().z - pos.getZ();
                y = pose.position().y - pos.getY() + Math.abs(Math.sin((level.getGameTime() + partialTick) * WALK_BOB_SPEED)) * WALK_BOB_HEIGHT;
                yaw = pose.yaw();
            }
            visual.update(workstation.golem(slot), workstation.isWorking(), new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z), yaw);

            BlockPos lightPos = BlockPos.containing(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
            int light = LightCoordsUtil.getLightCoords(level, lightPos);
            List<GeoRenderState> passes = new ArrayList<>();
            for (GolemPass pass : GolemPass.forParts(GolemItem.getParts(workstation.getStack(slot)))) {
                GeoRenderState golemState = this.golemRenderer.fillRenderState(visual, null, this.golemRenderer.createRenderState(visual, null), partialTick);
                golemState.addGeckolibData(DataTickets.PACKED_LIGHT, light);
                golemState.addGeckolibData(DataTickets.RENDER_COLOR, pass.color());
                golemState.addGeckolibData(GolemRenderer.PASS, pass);
                passes.add(golemState);
            }
            state.golems.add(new WorkstationRenderState.Golem(passes, x, y, z, yaw,
                    WorkstationRenderState.nameOf(workstation.getStack(slot)), workstation.golemSpeech(slot), light));
        }
    }

    @Override
    public void submit(WorkstationRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (WorkstationRenderState.Golem golem : state.golems) {
            golem.submitTags(poseStack, collector, camera);
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
    public AABB getRenderBoundingBox(AbstractWorkstationBlockEntity workstation) {
        return new AABB(workstation.getBlockPos()).inflate(workstation.renderReach());
    }

    @Override
    public boolean shouldRender(AbstractWorkstationBlockEntity workstation, Vec3 cameraPosition) {
        return Vec3.atCenterOf(workstation.getBlockPos()).closerThan(cameraPosition, getViewDistance() + workstation.renderReach());
    }

    private static float facingYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f;
        };
    }
}
