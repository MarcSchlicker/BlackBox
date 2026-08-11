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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.mcreator.blackbox.block.entity.BlackboxBlockBlockEntity;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.init.BlackboxModMenus;

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

	public BlackBoxGuiMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		super(BlackboxModMenus.BLACK_BOX_GUI.get(), id);
		this.entity = inventory.player;
		this.world = inventory.player.level();
		IItemHandler internal = new ItemStackHandler(BlackboxBlockBlockEntity.SLOT_COUNT);
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

		this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 8, 20) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(BlackboxModItems.DIMENSION_CORE.get());
			}
		}));
		for (int slot = BlackboxBlockBlockEntity.INPUT_START; slot < BlackboxBlockBlockEntity.INPUT_END; slot++) {
			this.customSlots.put(slot, this.addSlot(new SlotItemHandler(internal, slot, 8 + (slot - 1) * 18, 49)));
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
		this.customSlots.put(BlackboxBlockBlockEntity.UPGRADE_SLOT, this.addSlot(new SlotItemHandler(internal, BlackboxBlockBlockEntity.UPGRADE_SLOT, 152, 20) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(BlackboxModItems.STABILITY_UPGRADE.get());
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
		} else if (source.is(BlackboxModItems.DIMENSION_CORE.get())) {
			if (!this.moveItemStackTo(source, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (source.is(BlackboxModItems.STABILITY_UPGRADE.get())) {
			if (!this.moveItemStackTo(source, BlackboxBlockBlockEntity.UPGRADE_SLOT, BlackboxBlockBlockEntity.UPGRADE_SLOT + 1, false)) {
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
}
