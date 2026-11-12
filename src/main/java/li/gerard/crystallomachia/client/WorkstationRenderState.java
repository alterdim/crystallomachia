package li.gerard.crystallomachia.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class WorkstationRenderState extends BlockEntityRenderState {
    private static final Vec3 TAG_ATTACHMENT = new Vec3(0, 3.2, 0);
    private static final float TAG_LINE_HEIGHT = 9.0f * 1.15f * 0.025f;

    public final List<Golem> golems = new ArrayList<>();

    public record Golem(List<GeoRenderState> passes, double x, double y, double z, float yaw, Component name, @Nullable Component speech, int light) {
        public void submitTags(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            poseStack.pushPose();
            poseStack.translate(this.x, this.y, this.z);
            if (this.speech != null) {
                collector.submitNameTag(poseStack, TAG_ATTACHMENT, 0, this.speech, false, this.light, camera);
                poseStack.translate(0f, TAG_LINE_HEIGHT, 0f);
            }
            collector.submitNameTag(poseStack, TAG_ATTACHMENT, 0, this.name, false, this.light, camera);
            poseStack.popPose();
        }
    }

    public static Component nameOf(ItemStack golem) {
        return golem.getHoverName().copy().withStyle(ChatFormatting.BOLD);
    }
}
