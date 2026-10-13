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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import io.github.marcschlicker.blackbox.block.entity.SpawnerBlackboxBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModMenus;
import io.github.marcschlicker.blackbox.util.SpawnerIntegration;

import java.util.HashMap;
import java.util.Map;

public class SpawnerBlackboxMenu extends AbstractContainerMenu {
	public final Level world;
	public final int x;
	public final int y;
	public final int z;
	private final ContainerLevelAccess access;
	private final BlockEntity boundBlockEntity;

	public SpawnerBlackboxMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		super(BlackboxModMenus.SPAWNER_BLACKBOX.get(), id);
		this.world = inventory.player.level();
		BlockPos pos = extraData.readBlockPos();
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.access = ContainerLevelAccess.create(this.world, pos);
		this.boundBlockEntity = this.world.getBlockEntity(pos);
		IItemHandler internal = new ItemStackHandler(SpawnerBlackboxBlockEntity.SLOT_COUNT);
		if (this.boundBlockEntity instanceof BaseContainerBlockEntity container) {
			internal = new InvWrapper(container);
		} else if (this.boundBlockEntity != null) {
			IItemHandler capability = this.world.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (capability != null) {
				internal = capability;
			}
		}

		for (int slot = SpawnerBlackboxBlockEntity.SPAWNER_START; slot < SpawnerBlackboxBlockEntity.SPAWNER_END; slot++) {
			int local = slot - SpawnerBlackboxBlockEntity.SPAWNER_START;
			addSlot(new SlotItemHandler(internal, slot, 8 + local * 18, 20) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return SpawnerIntegration.isApothicSpawner(stack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}
		for (int slot = SpawnerBlackboxBlockEntity.UPGRADE_START; slot < SpawnerBlackboxBlockEntity.UPGRADE_END; slot++) {
			int local = slot - SpawnerBlackboxBlockEntity.UPGRADE_START;
			addSlot(new SlotItemHandler(internal, slot, 8 + local * 18, 50) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return SpawnerIntegration.isMobMasherUpgrade(stack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}
		for (int slot = SpawnerBlackboxBlockEntity.OUTPUT_START; slot < SpawnerBlackboxBlockEntity.OUTPUT_END; slot++) {
			int local = slot - SpawnerBlackboxBlockEntity.OUTPUT_START;
			addSlot(new SlotItemHandler(internal, slot, 8 + local % 9 * 18, 85 + local / 9 * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		}
		addPlayerInventory(inventory);
	}

	private void addPlayerInventory(Inventory inventory) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 141 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlot(new Slot(inventory, column, 8 + column * 18, 199));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return this.boundBlockEntity != null && AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= this.slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack source = slot.getItem();
		ItemStack copy = source.copy();
		if (index < SpawnerBlackboxBlockEntity.SLOT_COUNT) {
			if (!moveItemStackTo(source, SpawnerBlackboxBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (SpawnerIntegration.isApothicSpawner(source)) {
			if (!moveItemStackTo(source, SpawnerBlackboxBlockEntity.SPAWNER_START, SpawnerBlackboxBlockEntity.SPAWNER_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (SpawnerIntegration.isMobMasherUpgrade(source)) {
			if (!moveItemStackTo(source, SpawnerBlackboxBlockEntity.UPGRADE_START, SpawnerBlackboxBlockEntity.UPGRADE_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}
		if (source.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}
}
