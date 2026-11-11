package li.gerard.crystallomachia.client;

import java.util.ArrayList;
import java.util.List;

import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class GolemEntityRenderState extends EntityRenderState {
    public final List<GeoRenderState> passes = new ArrayList<>();
    public float bodyYaw;
}
