package li.gerard.crystallomachia;

import li.gerard.crystallomachia.client.ClientCapabilityTooltip;
import li.gerard.crystallomachia.block.AbstractWorkstationBlockEntity;
import li.gerard.crystallomachia.client.GolemMaterialTint;
import li.gerard.crystallomachia.client.WorkEffectEmitter;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import li.gerard.crystallomachia.client.WorkstationRenderer;
import li.gerard.crystallomachia.golem.CapabilityTooltip;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = crystallomachia.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = crystallomachia.MODID, value = Dist.CLIENT)
public class crystallomachiaClient {
    public crystallomachiaClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        AbstractWorkstationBlockEntity.CLIENT_TICK = WorkEffectEmitter::tick;
    }

    @SubscribeEvent
    static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CapabilityTooltip.class, ClientCapabilityTooltip::new);
    }

    @SubscribeEvent
    static void registerTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.fromNamespaceAndPath(crystallomachia.MODID, "golem_material"), GolemMaterialTint.MAP_CODEC);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(crystallomachia.GOLEM_FURNACE_BLOCK_ENTITY.get(), WorkstationRenderer::new);
        event.registerBlockEntityRenderer(crystallomachia.FORGE_STATION_BLOCK_ENTITY.get(), WorkstationRenderer::new);
        event.registerBlockEntityRenderer(crystallomachia.GOLEM_ASSEMBLER_BLOCK_ENTITY.get(), WorkstationRenderer::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        crystallomachia.LOGGER.info("HELLO FROM CLIENT SETUP");
        crystallomachia.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
