package li.gerard.crystallomachia.block;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class DiscoBallBlock extends BaseEntityBlock implements BlockUIMenuType.BlockUI {
    public DiscoBallBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DiscoBallBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return createTickerHelper(type, crystallomachia.DISCO_BALL_BLOCK_ENTITY.get(), (tickLevel, pos, tickState, blockEntity) -> blockEntity.clientTick());
        }
        return createTickerHelper(type, crystallomachia.DISCO_BALL_BLOCK_ENTITY.get(), (tickLevel, pos, tickState, blockEntity) -> blockEntity.serverTick(serverLevel));
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof DiscoBallBlockEntity discoBall) {
            return discoBall.createUI(holder);
        }
        return new ModularUI(UI.of(new UIElement()), holder.player);
    }

    @Override
    public boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        return BlockUIMenuType.BlockUI.super.stillValid(holder) && DancefloorBlock.hasFloorBelow(holder.player.level(), holder.pos);
    }
}
