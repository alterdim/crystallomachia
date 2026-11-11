package li.gerard.crystallomachia.client;

import java.util.ArrayList;
import java.util.List;

import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class WorkstationRenderState extends BlockEntityRenderState {
    public final List<Golem> golems = new ArrayList<>();

    public record Golem(GeoRenderState state, double x, double y, double z, float yaw) {}
}
