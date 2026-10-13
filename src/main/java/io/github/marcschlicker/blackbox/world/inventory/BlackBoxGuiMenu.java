package io.github.marcschlicker.blackbox.world.inventory;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import io.github.marcschlicker.blackbox.block.entity.BlackboxBlockBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.SpawnerBlackboxBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.init.BlackboxModMenus;
import io.github.marcschlicker.blackbox.util.FarmMeasurementHost;
import io.github.marcschlicker.blackbox.util.SpawnerIntegration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BlackBoxGuiMenu extends AbstractContainerMenu implements BlackboxModMenus.MenuAccessor {
	public final Level world;
	public final Player entity;
	public int x;
	public int y;
	public int z;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private final Map<String, Object> menuState = new HashMap<>();
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private BlockEntity boundBlockEntity;
	private IItemHandler internalInventory;
	private final boolean spawnerBlackbox;
	private final ContainerData calculationData;

	public BlackBoxGuiMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		super(BlackboxModMenus.BLACK_BOX_GUI.get(), id);
		this.entity = inventory.player;
		this.world = inventory.player.level();
		IItemHandler internal = new ItemStackHandler(BlackboxBlockBlockEntity.SLOT_COUNT);
		ContainerData data = new SimpleContainerData(4);
		if (extraData != null) {
			BlockPos pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			this.access = ContainerLevelAccess.create(this.world, pos);
			this.boundBlockEntity = this.world.getBlockEntity(pos);
			if (this.boundBlockEntity instanceof BaseContainerBlockEntity container) {
				internal = new InvWrapper(container);
			} else if (this.boundBlockEntity != null) {
				IItemHandler capability = this.world.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
				if (capability != null) {
					internal = capability;
				}
			}
		}
		this.internalInventory = internal;
		this.spawnerBlackbox = this.boundBlockEntity instanceof SpawnerBlackboxBlockEntity;
		if (this.boundBlockEntity instanceof FarmMeasurementHost host) {
			data = new ContainerData() {
				@Override
				public int get(int index) {
					return switch (index) {
						case 0 -> host.getCalculationPhase();
						case 1 -> host.getCalculationTicksRemaining();
						case 2 -> host.getCalculationTotalTicks();
						case 3 -> host.getCalculationItemsPerMinute();
						default -> 0;
					};
				}

				@Override
				public void set(int index, int value) {
				}

				@Override
				public int getCount() {
					return 4;
				}
			};
		}
		this.calculationData = data;
		this.addDataSlots(data);

		this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 8, 20) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return BlackBoxGuiMenu.this.spawnerBlackbox
						? SpawnerIntegration.isApothicSpawner(stack)
						: stack.is(BlackboxModItems.DIMENSION_CORE.get());
			}

			@Override
			public int getMaxStackSize() {
				return BlackBoxGuiMenu.this.spawnerBlackbox ? 1 : super.getMaxStackSize();
			}
		}));
		for (int slot = BlackboxBlockBlockEntity.INPUT_START; slot < BlackboxBlockBlockEntity.INPUT_END; slot++) {
			int inputSlot = slot;
			int slotX = this.spawnerBlackbox
					? 8 + (inputSlot < SpawnerBlackboxBlockEntity.SPAWNER_END ? inputSlot : inputSlot - SpawnerBlackboxBlockEntity.MODULE_START) * 18
					: 8 + (slot - 1) * 18;
			int slotY = this.spawnerBlackbox ? (inputSlot < SpawnerBlackboxBlockEntity.SPAWNER_END ? 20 : 49) : 49;
			this.customSlots.put(slot, this.addSlot(new SlotItemHandler(internal, slot, slotX, slotY) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					if (!BlackBoxGuiMenu.this.spawnerBlackbox) {
						return true;
					}
					return inputSlot < SpawnerBlackboxBlockEntity.SPAWNER_END
							? SpawnerIntegration.isApothicSpawner(stack)
							: canPlaceSpawnerModule(inputSlot, stack);
				}

				@Override
				public int getMaxStackSize() {
					return BlackBoxGuiMenu.this.spawnerBlackbox && inputSlot >= SpawnerBlackboxBlockEntity.MODULE_START
							? SpawnerIntegration.masherUpgradeLimit() : super.getMaxStackSize();
				}
			}));
		}
		for (int slot = BlackboxBlockBlockEntity.OUTPUT_START; slot < BlackboxBlockBlockEntity.OUTPUT_END; slot++) {
			int local = slot - BlackboxBlockBlockEntity.OUTPUT_START;
			this.customSlots.put(slot, this.addSlot(new SlotItemHandler(internal, slot, 8 + (local % 9) * 18, 78 + (local / 9) * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			}));
		}
		this.customSlots.put(BlackboxBlockBlockEntity.UPGRADE_SLOT, this.addSlot(new SlotItemHandler(internal, BlackboxBlockBlockEntity.UPGRADE_SLOT,
				this.spawnerBlackbox ? -1_000 : 152, this.spawnerBlackbox ? -1_000 : 20) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return !BlackBoxGuiMenu.this.spawnerBlackbox && stack.is(BlackboxModItems.STABILITY_UPGRADE.get());
			}
		}));
		this.customSlots.put(BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT, this.addSlot(new SlotItemHandler(internal, BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT,
				this.spawnerBlackbox ? 170 : 134, 20) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get());
			}
		}));
		this.customSlots.put(BlackboxBlockBlockEntity.FLUID_INPUT_CONTAINER_SLOT,
				this.addSlot(new SlotItemHandler(internal, BlackboxBlockBlockEntity.FLUID_INPUT_CONTAINER_SLOT, 8, 119) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return BlackboxBlockBlockEntity.isFilledFluidContainer(stack);
					}

					@Override
					public int getMaxStackSize() {
						return 1;
					}
				}));
		this.customSlots.put(BlackboxBlockBlockEntity.FLUID_OUTPUT_CONTAINER_SLOT,
				this.addSlot(new SlotItemHandler(internal, BlackboxBlockBlockEntity.FLUID_OUTPUT_CONTAINER_SLOT, 26, 119) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return stack.is(Items.BUCKET);
					}

					@Override
					public int getMaxStackSize() {
						return 1;
					}
				}));
		addPlayerInventory(inventory);
	}

	private void addPlayerInventory(Inventory inventory) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 141 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(inventory, column, 8 + column * 18, 199));
		}
	}

	private boolean canPlaceSpawnerModule(int slot, ItemStack candidate) {
		if (!SpawnerIntegration.isMobMasherUpgrade(candidate)) {
			return false;
		}
		ItemStack current = this.internalInventory.getStackInSlot(slot);
		if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, candidate)) {
			return false;
		}
		if (!current.isEmpty() && current.getCount() >= SpawnerIntegration.masherUpgradeLimit()) {
			return false;
		}
		for (int moduleSlot = SpawnerBlackboxBlockEntity.MODULE_START; moduleSlot < SpawnerBlackboxBlockEntity.MODULE_END; moduleSlot++) {
			if (moduleSlot != slot && ItemStack.isSameItemSameComponents(this.internalInventory.getStackInSlot(moduleSlot), candidate)) {
				return false;
			}
		}
		return true;
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
		if (index < BlackboxBlockBlockEntity.SLOT_COUNT) {
			if (!this.moveItemStackTo(source, BlackboxBlockBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.spawnerBlackbox && source.is(BlackboxModItems.DIMENSION_CORE.get())) {
			if (!this.moveItemStackTo(source, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (this.spawnerBlackbox && SpawnerIntegration.isApothicSpawner(source)) {
			if (!this.moveItemStackTo(source, SpawnerBlackboxBlockEntity.SPAWNER_START, SpawnerBlackboxBlockEntity.SPAWNER_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (this.spawnerBlackbox && SpawnerIntegration.isMobMasherUpgrade(source)) {
			if (!this.moveItemStackTo(source, SpawnerBlackboxBlockEntity.MODULE_START, SpawnerBlackboxBlockEntity.MODULE_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.spawnerBlackbox && source.is(BlackboxModItems.STABILITY_UPGRADE.get())) {
			if (!this.moveItemStackTo(source, BlackboxBlockBlockEntity.UPGRADE_SLOT, BlackboxBlockBlockEntity.UPGRADE_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (source.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get())) {
			if (!this.moveItemStackTo(source, BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT, BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (BlackboxBlockBlockEntity.isFilledFluidContainer(source)) {
			if (!this.moveItemStackTo(source, BlackboxBlockBlockEntity.FLUID_INPUT_CONTAINER_SLOT, BlackboxBlockBlockEntity.FLUID_INPUT_CONTAINER_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (source.is(Items.BUCKET)) {
			if (!this.moveItemStackTo(source, BlackboxBlockBlockEntity.FLUID_OUTPUT_CONTAINER_SLOT, BlackboxBlockBlockEntity.FLUID_OUTPUT_CONTAINER_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(source, BlackboxBlockBlockEntity.INPUT_START, BlackboxBlockBlockEntity.INPUT_END, false)) {
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

	public boolean isSpawnerBlackbox() {
		return this.spawnerBlackbox;
	}

	public int getCalculationPhase() {
		return this.calculationData.get(0);
	}

	public int getCalculationSecondsRemaining() {
		return (this.calculationData.get(1) + 19) / 20;
	}

	public int getCalculationTotalTicks() {
		return this.calculationData.get(2);
	}

	public int getCalculationProgressPercent() {
		int total = this.calculationData.get(2);
		if (total <= 0 || getCalculationPhase() <= 0) {
			return 0;
		}
		return Math.clamp((total - this.calculationData.get(1)) * 100 / total, 0, 100);
	}

	public int getCalculationItemsPerMinute() {
		return this.calculationData.get(3);
	}
}
