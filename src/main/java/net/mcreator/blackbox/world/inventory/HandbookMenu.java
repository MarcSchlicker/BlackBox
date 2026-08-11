package net.mcreator.blackbox.world.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.init.BlackboxModMenus;

public class HandbookMenu extends AbstractContainerMenu {
	public HandbookMenu(int id) {
		super(BlackboxModMenus.HANDBOOK.get(), id);
	}

	public HandbookMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
		this(id);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}
