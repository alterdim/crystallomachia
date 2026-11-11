package li.gerard.crystallomachia;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import java.util.Map;

import li.gerard.crystallomachia.block.ForgeStationBlock;
import li.gerard.crystallomachia.block.ForgeStationBlockEntity;
import li.gerard.crystallomachia.block.GolemAssemblerBlock;
import li.gerard.crystallomachia.block.GolemAssemblerBlockEntity;
import java.util.EnumMap;
import java.util.List;
import li.gerard.crystallomachia.golem.GolemParts;
import li.gerard.crystallomachia.entity.GolemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import li.gerard.crystallomachia.recipe.ForgingRecipe;
import li.gerard.crystallomachia.recipe.TraitSmeltingRecipe;
import net.minecraft.core.Holder;
import li.gerard.crystallomachia.golem.GolemMaterial;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;
import li.gerard.crystallomachia.golem.GolemPart;
import li.gerard.crystallomachia.golem.GolemPartItem;
import li.gerard.crystallomachia.block.GolemFurnaceBlock;
import li.gerard.crystallomachia.block.GolemFurnaceBlockEntity;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(crystallomachia.MODID)
public class crystallomachia {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "crystallomachia";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "crystallomachia" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "crystallomachia" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "crystallomachia" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "crystallomachia:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "crystallomachia:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "crystallomachia:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GolemData>> GOLEM_DATA = DATA_COMPONENTS.registerComponentType("golem_data",
            builder -> builder.persistent(GolemData.CODEC).networkSynchronized(GolemData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceKey<GolemMaterial>>> GOLEM_MATERIAL = DATA_COMPONENTS.registerComponentType("golem_material",
            builder -> builder.persistent(ResourceKey.codec(GolemMaterial.REGISTRY_KEY)).networkSynchronized(ResourceKey.streamCodec(GolemMaterial.REGISTRY_KEY)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GolemParts>> GOLEM_PARTS = DATA_COMPONENTS.registerComponentType("golem_parts",
            builder -> builder.persistent(GolemParts.CODEC).networkSynchronized(GolemParts.STREAM_CODEC));

    public static final DeferredItem<GolemItem> GOLEM_ITEM =ITEMS.registerItem("golem", GolemItem::new);

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GolemEntity>> GOLEM_ENTITY = ENTITY_TYPES.registerEntityType("golem",
            GolemEntity::new, MobCategory.MONSTER, builder -> builder.sized(1.2f, 3.0f).clientTrackingRange(8));

    public static final DeferredItem<Item> BURNTMETAL_INGOT =ITEMS.registerSimpleItem("burntmetal_ingot");

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<GolemFurnaceBlock> GOLEM_FURNACE = BLOCKS.registerBlock("golem_furnace", GolemFurnaceBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(3.5f));
    public static final DeferredItem<BlockItem> GOLEM_FURNACE_ITEM = ITEMS.registerSimpleBlockItem("golem_furnace", GOLEM_FURNACE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemFurnaceBlockEntity>> GOLEM_FURNACE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("golem_furnace",
            () -> new BlockEntityType<>(GolemFurnaceBlockEntity::new, GOLEM_FURNACE.get()));

    public static final DeferredBlock<ForgeStationBlock> FORGE_STATION = BLOCKS.registerBlock("forge_station", ForgeStationBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(3.5f));
    public static final DeferredItem<BlockItem> FORGE_STATION_ITEM = ITEMS.registerSimpleBlockItem("forge_station", FORGE_STATION);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeStationBlockEntity>> FORGE_STATION_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("forge_station",
            () -> new BlockEntityType<>(ForgeStationBlockEntity::new, FORGE_STATION.get()));

    public static final DeferredBlock<GolemAssemblerBlock> GOLEM_ASSEMBLER = BLOCKS.registerBlock("golem_assembler", GolemAssemblerBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(3.5f));
    public static final DeferredItem<BlockItem> GOLEM_ASSEMBLER_ITEM = ITEMS.registerSimpleBlockItem("golem_assembler", GOLEM_ASSEMBLER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemAssemblerBlockEntity>> GOLEM_ASSEMBLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("golem_assembler",
            () -> new BlockEntityType<>(GolemAssemblerBlockEntity::new, GOLEM_ASSEMBLER.get()));

    public static final DeferredItem<GolemPartItem> GOLEM_HEAD = ITEMS.registerItem("golem_head", p -> new GolemPartItem(GolemPart.HEAD, p));
    public static final DeferredItem<GolemPartItem> GOLEM_TORSO = ITEMS.registerItem("golem_torso", p -> new GolemPartItem(GolemPart.TORSO, p));
    public static final DeferredItem<GolemPartItem> GOLEM_HAND_LEFT = ITEMS.registerItem("golem_hand_left", p -> new GolemPartItem(GolemPart.HAND_LEFT, p));
    public static final DeferredItem<GolemPartItem> GOLEM_HAND_RIGHT = ITEMS.registerItem("golem_hand_right", p -> new GolemPartItem(GolemPart.HAND_RIGHT, p));
    public static final DeferredItem<GolemPartItem> GOLEM_LEGS = ITEMS.registerItem("golem_legs", p -> new GolemPartItem(GolemPart.LEGS, p));

    // Creates a creative tab with the id "crystallomachia:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.crystallomachia")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());// Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(GOLEM_FURNACE_ITEM.get());
                output.accept(FORGE_STATION_ITEM.get());
                output.accept(GOLEM_ASSEMBLER_ITEM.get());
                output.accept(BURNTMETAL_INGOT.get());
                parameters.holders().lookup(GolemMaterial.REGISTRY_KEY).ifPresent(materials -> materials.listElements().forEach(material -> {
                    output.accept(GOLEM_HEAD.get().create(material));
                    output.accept(GOLEM_TORSO.get().create(material));
                    output.accept(GOLEM_HAND_LEFT.get().create(material));
                    output.accept(GOLEM_HAND_RIGHT.get().create(material));
                    output.accept(GOLEM_LEGS.get().create(material));
                }));
                parameters.holders().lookup(GolemMaterial.REGISTRY_KEY).ifPresent(materials -> {
                    List<Holder.Reference<GolemMaterial>> all = materials.listElements().toList();
                    if (all.isEmpty()) {
                        return;
                    }
                    Map<GolemPart, Holder.Reference<GolemMaterial>> mixed = new EnumMap<>(GolemPart.class);
                    for (GolemPart part : GolemPart.values()) {
                        mixed.put(part, all.get(part.ordinal() % all.size()));
                    }
                    output.accept(GolemItem.createFromMaterials(mixed, 5L));
                });
                output.accept(GolemItem.create(new GolemData(Map.of(GolemCapability.HEAT, 1), 1L)));
                output.accept(GolemItem.create(new GolemData(Map.of(GolemCapability.HEAT, 3), 2L)));
                output.accept(GolemItem.create(new GolemData(Map.of(GolemCapability.HANDWORK, 1), 3L)));
                output.accept(GolemItem.create(new GolemData(Map.of(GolemCapability.HEAT, 2, GolemCapability.HANDWORK, 2), 4L)));
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public crystallomachia(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener((EntityAttributeCreationEvent event) ->
                event.put(GOLEM_ENTITY.get(), GolemEntity.createAttributes().build()));
        modEventBus.addListener((RegisterSpawnPlacementsEvent event) ->
                event.register(GOLEM_ENTITY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        modEventBus.addListener((NewDatapackRegistryEvent event) -> {
            event.worldRegistry(GolemMaterial.REGISTRY_KEY, GolemMaterial.CODEC, GolemMaterial.CODEC);
            event.worldRegistry(ForgingRecipe.REGISTRY_KEY, ForgingRecipe.CODEC, ForgingRecipe.CODEC);
            event.worldRegistry(TraitSmeltingRecipe.REGISTRY_KEY, TraitSmeltingRecipe.CODEC, TraitSmeltingRecipe.CODEC);
        });
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (crystallomachia) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.LOCAL, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
