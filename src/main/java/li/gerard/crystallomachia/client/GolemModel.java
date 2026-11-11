package li.gerard.crystallomachia.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.resources.Identifier;

public class GolemModel extends GeoModel<GolemVisual> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(crystallomachia.MODID, "golem");
    private static final Identifier ANIMATIONS = Identifier.fromNamespaceAndPath(crystallomachia.MODID, "golem");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(crystallomachia.MODID, "textures/golem/stone.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(GolemVisual animatable) {
        return ANIMATIONS;
    }
}
