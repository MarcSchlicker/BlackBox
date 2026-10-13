package io.github.marcschlicker.blackbox.block;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import io.github.marcschlicker.blackbox.block.entity.CoreDistributionMatrixBlockEntity;
import io.github.marcschlicker.blackbox.world.inventory.CoreDistributionMatrixMenu;

/**
 * Lets players inspect and confirm an irreversible core split before the farm
 * parcel is retired. The block keeps its two input slots when its GUI closes.
 */
public class CoreDistributionMatrixBlock extends Block implements EntityBlock {
	public CoreDistributionMatrixBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.0F, 10.0F));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CoreDistributionMatrixBlockEntity(pos, state);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.translatable("block.blackbox.core_distribution_matrix");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
					return new CoreDistributionMatrixMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
				}
			}, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof MenuProvider provider ? provider : null;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (state.getBlock() != newState.getBlock()) {
			if (level.getBlockEntity(pos) instanceof CoreDistributionMatrixBlockEntity matrix) {
				Containers.dropContents(level, pos, matrix);
			}
			super.onRemove(state, level, pos, newState, moving);
		}
	}
}
