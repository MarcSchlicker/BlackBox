package net.mcreator.blackbox.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.mcreator.blackbox.init.BlackboxModBlockEntities;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.world.inventory.BlackBoxGuiMenu;
import net.mcreator.blackbox.util.FarmSimulationMachine;

import io.netty.buffer.Unpooled;

import javax.annotation.Nullable;

public class BlackboxBlockBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, FarmSimulationMachine {
	public static final int CORE_SLOT = FarmSimulationMachine.CORE_SLOT;
	public static final int INPUT_START = FarmSimulationMachine.INPUT_START;
	public static final int INPUT_END = FarmSimulationMachine.INPUT_END;
	public static final int OUTPUT_START = FarmSimulationMachine.OUTPUT_START;
	public static final int OUTPUT_END = FarmSimulationMachine.OUTPUT_END;
	public static final int UPGRADE_SLOT = FarmSimulationMachine.UPGRADE_SLOT;
	public static final int SLOT_COUNT = FarmSimulationMachine.SLOT_COUNT;
	private static final int[] AUTOMATION_SLOTS = java.util.stream.IntStream.range(INPUT_START, OUTPUT_END).toArray();
	private NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int simulationTicks;
	private String activeCoreId = "";
	private boolean stableCycleFunded;

	public BlackboxBlockBlockEntity(BlockPos position, BlockState state) {
		super(BlackboxModBlockEntities.BLACKBOX_BLOCK.get(), position, state);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(tag, lookupProvider);
		this.stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, this.stacks, lookupProvider);
		this.simulationTicks = tag.getInt("SimulationTicks");
		this.activeCoreId = tag.getString("ActiveCoreId");
		this.stableCycleFunded = tag.getBoolean("StableCycleFunded");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(tag, lookupProvider);
		ContainerHelper.saveAllItems(tag, this.stacks, lookupProvider);
		tag.putInt("SimulationTicks", this.simulationTicks);
		tag.putString("ActiveCoreId", this.activeCoreId);
		tag.putBoolean("StableCycleFunded", this.stableCycleFunded);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.blackbox.blackbox_block");
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
		return new BlackBoxGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == CORE_SLOT) {
			return stack.is(BlackboxModItems.DIMENSION_CORE.get());
		}
		if (slot == UPGRADE_SLOT) {
			return stack.is(BlackboxModItems.STABILITY_UPGRADE.get());
		}
		return slot >= INPUT_START && slot < INPUT_END;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return AUTOMATION_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= OUTPUT_START && slot < OUTPUT_END;
	}

	public int getSimulationTicks() {
		return this.simulationTicks;
	}

	public void setSimulationTicks(int simulationTicks) {
		if (this.simulationTicks != simulationTicks) {
			this.simulationTicks = simulationTicks;
			this.setChanged();
		}
	}

	public String getActiveCoreId() {
		return this.activeCoreId;
	}

	public void setActiveCoreId(String activeCoreId) {
		if (!this.activeCoreId.equals(activeCoreId)) {
			this.activeCoreId = activeCoreId;
			this.setChanged();
		}
	}

	@Override
	public boolean isStableCycleFunded() {
		return this.stableCycleFunded;
	}

	@Override
	public void setStableCycleFunded(boolean funded) {
		if (this.stableCycleFunded != funded) {
			this.stableCycleFunded = funded;
			this.setChanged();
		}
	}
}
