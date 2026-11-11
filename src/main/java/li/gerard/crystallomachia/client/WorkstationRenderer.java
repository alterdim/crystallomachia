package li.gerard.crystallomachia.client;

import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import li.gerard.crystallomachia.block.AbstractWorkstationBlock;
import li.gerard.crystallomachia.block.AbstractWorkstationBlockEntity;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemItem;
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
    private final GolemRenderer golemRenderer = new GolemRenderer();
    private final Map<AbstractWorkstationBlockEntity, GolemVisual[]> visuals = new WeakHashMap<>();

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
        GolemVisual[] slots = this.visuals.computeIfAbsent(workstation, key -> new GolemVisual[key.golemSlots()]);
        float facingYaw = facingYaw(workstation.getBlockState().getValue(AbstractWorkstationBlock.FACING));
        double sin = Math.sin(Math.toRadians(facingYaw));
        double cos = Math.cos(Math.toRadians(facingYaw));
        BlockPos pos = workstation.getBlockPos();
        for (int slot = 0; slot < slots.length; slot++) {
            if (!(workstation.getStack(slot).getItem() instanceof GolemItem)) {
                continue;
            }
            if (slots[slot] == null) {
                slots[slot] = new GolemVisual();
            }
            GolemVisual visual = slots[slot];
            visual.update(workstation.golem(slot), workstation.isWorking());

            GolemAnchor anchor = workstation.golemAnchor(slot);
            double localX = anchor.right();
            double localZ = -anchor.front();
            double x = 0.5 + localX * cos + localZ * sin;
            double z = 0.5 - localX * sin + localZ * cos;
            double y = anchor.up();

            GeoRenderState golemState = this.golemRenderer.fillRenderState(visual, null, this.golemRenderer.createRenderState(visual, null), partialTick);
            BlockPos lightPos = BlockPos.containing(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
            golemState.addGeckolibData(DataTickets.PACKED_LIGHT, LightCoordsUtil.getLightCoords(level, lightPos));
            state.golems.add(new WorkstationRenderState.Golem(golemState, x, y, z, facingYaw + anchor.yaw()));
        }
    }

    @Override
    public void submit(WorkstationRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (WorkstationRenderState.Golem golem : state.golems) {
            poseStack.pushPose();
            poseStack.translate(golem.x(), golem.y(), golem.z());
            poseStack.rotateDegrees(Axis.YP, golem.yaw());
            this.golemRenderer.performRenderPass(golem.state(), poseStack, collector, camera);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(AbstractWorkstationBlockEntity workstation) {
        return new AABB(workstation.getBlockPos()).inflate(2);
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
