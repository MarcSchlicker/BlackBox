package net.mcreator.blackbox.world.inventory;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.init.BlackboxModMenus;
import net.mcreator.blackbox.item.CoreEnvironmentUpgradeItem;
import net.mcreator.blackbox.util.FarmSimulationMachine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DimensionalWorkbenchGUIMenu extends AbstractContainerMenu implements BlackboxModMenus.MenuAccessor {
	public final Level world;
	public final Player entity;
	public int x;
	public int y;
	public int z;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private final Map<String, Object> menuState = new HashMap<>();
	private final ContainerData calculationData;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private BlockEntity boundBlockEntity;

	public DimensionalWorkbenchGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		super(BlackboxModMenus.DIMENSIONAL_WORKBENCH_GUI.get(), id);
		this.entity = inventory.player;
		this.world = inventory.player.level();
		IItemHandler internal = new ItemStackHandler(FarmSimulationMachine.SLOT_COUNT);
		ContainerData data = new SimpleContainerData(2);
		if (extraData != null) {
			BlockPos pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			this.access = ContainerLevelAccess.create(this.world, pos);
			this.boundBlockEntity = this.world.getBlockEntity(pos);
			if (this.boundBlockEntity instanceof BaseContainerBlockEntity container) {
				internal = new InvWrapper(container);
			}
			if (this.boundBlockEntity instanceof DimensionalWorkbenchBlockEntity workbench) {
				data = new ContainerData() {
					@Override
					public int get(int index) {
						return index == 0 ? workbench.getCalculationPhase() : workbench.getCalculationTicksRemaining();
					}

					@Override
					public void set(int index, int value) {
					}

					@Override
					public int getCount() {
						return 2;
					}
				};
			} else if (this.boundBlockEntity != null) {
				IItemHandler capability = this.world.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
				if (capability != null) {
					internal = capability;
				}
			}
		}
		this.calculationData = data;
		this.addDataSlots(data);

		this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 8, 24) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(BlackboxModItems.DIMENSION_CORE.get());
			}
		}));
		for (int slot = FarmSimulationMachine.INPUT_START; slot < FarmSimulationMachine.INPUT_END; slot++) {
			this.customSlots.put(slot, this.addSlot(new SlotItemHandler(internal, slot, 8 + (slot - 1) * 18, 75)));
		}
		for (int slot = FarmSimulationMachine.OUTPUT_START; slot < FarmSimulationMachine.OUTPUT_END; slot++) {
			int local = slot - FarmSimulationMachine.OUTPUT_START;
			this.customSlots.put(slot, this.addSlot(new SlotItemHandler(internal, slot, 8 + (local % 9) * 18, 103 + (local / 9) * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			}));
		}
		this.customSlots.put(FarmSimulationMachine.UPGRADE_SLOT, this.addSlot(new SlotItemHandler(internal, FarmSimulationMachine.UPGRADE_SLOT, 170, 51) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(BlackboxModItems.STABILITY_UPGRADE.get()) || stack.is(BlackboxModItems.MOB_SPAWN_UPGRADE.get()) || stack.getItem() instanceof CoreEnvironmentUpgradeItem;
			}
		}));
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 161 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(inventory, column, 8 + column * 18, 219));
		}
	}

	public int getCalculationPhase() {
		return this.calculationData.get(0);
	}

	public int getCalculationSecondsRemaining() {
		return (this.calculationData.get(1) + 19) / 20;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.boundBlockEntity == null || AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack source = slot.getItem();
		ItemStack copy = source.copy();
		if (index < FarmSimulationMachine.SLOT_COUNT) {
			if (!this.moveItemStackTo(source, FarmSimulationMachine.SLOT_COUNT, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (source.is(BlackboxModItems.DIMENSION_CORE.get())) {
			if (!this.moveItemStackTo(source, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (source.is(BlackboxModItems.STABILITY_UPGRADE.get()) || source.is(BlackboxModItems.MOB_SPAWN_UPGRADE.get()) || source.getItem() instanceof CoreEnvironmentUpgradeItem) {
			if (!this.moveItemStackTo(source, FarmSimulationMachine.UPGRADE_SLOT, FarmSimulationMachine.UPGRADE_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(source, FarmSimulationMachine.INPUT_START, FarmSimulationMachine.INPUT_END, false)) {
			return ItemStack.EMPTY;
		}
		if (source.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public Map<Integer, Slot> getSlots() {
		return Collections.unmodifiableMap(this.customSlots);
	}

	@Override
	public Map<String, Object> getMenuState() {
		return this.menuState;
	}
}
