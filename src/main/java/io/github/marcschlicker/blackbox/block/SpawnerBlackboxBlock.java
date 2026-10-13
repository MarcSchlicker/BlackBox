package io.github.marcschlicker.blackbox.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import io.github.marcschlicker.blackbox.block.entity.SpawnerBlackboxBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModBlockEntities;
import io.github.marcschlicker.blackbox.util.SpawnerBlackboxRuntime;

import net.neoforged.neoforge.fluids.FluidUtil;

/** A normal BlackBox variant whose input row is configured with real Apothic Spawners. */
public class SpawnerBlackboxBlock extends Block implements EntityBlock {
	public SpawnerBlackboxBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.0F, 10.0F));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpawnerBlackboxBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || type != BlackboxModBlockEntities.SPAWNER_BLACKBOX.get()) {
			return null;
		}
		return (tickLevel, pos, tickState, blockEntity) -> {
			if (tickLevel instanceof ServerLevel serverLevel && blockEntity instanceof SpawnerBlackboxBlockEntity blackbox) {
				blackbox.processFluidContainers();
				if (tickLevel.getGameTime() % SpawnerBlackboxRuntime.TICK_INTERVAL == 0) {
					SpawnerBlackboxRuntime.tick(serverLevel, pos);
				}
				if (blackbox.consumeResourceSyncPending()) {
					blackbox.syncToClient();
				}
			}
		};
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			// The block entity already owns the BlackBox menu. Avoid an anonymous
			// MenuProvider class, which is fragile in large transformed modpacks.
			MenuProvider provider = this.getMenuProvider(state, level, pos);
			if (provider != null) {
				serverPlayer.openMenu(provider, pos);
			}
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
			if (level.getBlockEntity(pos) instanceof SpawnerBlackboxBlockEntity blackbox) {
				Containers.dropContents(level, pos, blackbox);
			}
			super.onRemove(state, level, pos, newState, moving);
		}
	}
}
