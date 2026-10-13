package io.github.marcschlicker.blackbox.block.entity;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.github.marcschlicker.blackbox.init.BlackboxModBlockEntities;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.world.inventory.CoreDistributionMatrixMenu;

/** Persistent storage for the source core and its consumed split upgrade. */
public class CoreDistributionMatrixBlockEntity extends RandomizableContainerBlockEntity {
	public static final int CORE_SLOT = 0;
	public static final int SPLITTER_SLOT = 1;
	public static final int SLOT_COUNT = 2;
	private NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

	public CoreDistributionMatrixBlockEntity(BlockPos pos, BlockState state) {
		super(BlackboxModBlockEntities.CORE_DISTRIBUTION_MATRIX.get(), pos, state);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(tag, lookupProvider);
		this.stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, this.stacks, lookupProvider);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(tag, lookupProvider);
		ContainerHelper.saveAllItems(tag, this.stacks, lookupProvider);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.blackbox.core_distribution_matrix");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public int getContainerSize() {
		return SLOT_COUNT;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new CoreDistributionMatrixMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == CORE_SLOT ? stack.is(BlackboxModItems.DIMENSION_CORE.get())
				: slot == SPLITTER_SLOT && stack.is(BlackboxModItems.CORE_SPLITTER.get());
	}
}
