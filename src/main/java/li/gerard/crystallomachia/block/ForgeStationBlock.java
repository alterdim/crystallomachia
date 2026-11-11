package li.gerard.crystallomachia.block;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ForgeStationBlock extends AbstractWorkstationBlock {
    public ForgeStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<? extends AbstractWorkstationBlockEntity> blockEntityType() {
        return crystallomachia.FORGE_STATION_BLOCK_ENTITY.get();
    }
}
