package io.github.marcschlicker.blackbox.block;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import io.github.marcschlicker.blackbox.world.inventory.DimensionalWorkbenchGUIMenu;
import io.github.marcschlicker.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModBlockEntities;
import io.github.marcschlicker.blackbox.util.BlackboxSimulationRuntime;
import io.github.marcschlicker.blackbox.util.WorkbenchRuntime;
import io.github.marcschlicker.blackbox.util.FarmMobTransport;
import io.github.marcschlicker.blackbox.util.LegacyWorkbenchMigration;

import io.netty.buffer.Unpooled;
import net.neoforged.neoforge.fluids.FluidUtil;

public class DimensionalWorkbenchBlock extends Block implements EntityBlock {
	public DimensionalWorkbenchBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1f, 10f));
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || type != BlackboxModBlockEntities.DIMENSIONAL_WORKBENCH.get()) {
			return null;
		}
		return (tickLevel, pos, tickState, blockEntity) -> {
			if (tickLevel instanceof ServerLevel serverLevel && LegacyWorkbenchMigration.migrateAt(serverLevel, pos)) {
				return;
			}
			if (tickLevel instanceof ServerLevel serverLevel && tickLevel.getGameTime() % BlackboxSimulationRuntime.TICK_INTERVAL == 0) {
				FarmMobTransport.checkWorkbench(serverLevel, pos);
				WorkbenchRuntime.tick(serverLevel, pos);
				if (blockEntity instanceof DimensionalWorkbenchBlockEntity workbench && workbench.consumeResourceSyncPending()) {
					workbench.syncToClient();
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
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		if (entity instanceof ServerPlayer player) {
			player.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Dimensional Workbench");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					return new DimensionalWorkbenchGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
				}
			}, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DimensionalWorkbenchBlockEntity(pos, state);
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
		super.triggerEvent(state, world, pos, eventID, eventParam);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			if (world instanceof ServerLevel serverLevel && LegacyWorkbenchMigration.isMigrating(serverLevel, pos)) {
				super.onRemove(state, world, pos, newState, isMoving);
				return;
			}
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof DimensionalWorkbenchBlockEntity be) {
				Containers.dropContents(world, pos, be);
				world.updateNeighbourForOutputSignal(pos, this);
			}
			super.onRemove(state, world, pos, newState, isMoving);
		}
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
		BlockEntity tileentity = world.getBlockEntity(pos);
		if (tileentity instanceof DimensionalWorkbenchBlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		else
			return 0;
	}
}
