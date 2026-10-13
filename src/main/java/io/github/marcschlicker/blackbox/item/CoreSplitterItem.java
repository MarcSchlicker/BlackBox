package io.github.marcschlicker.blackbox.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CoreSplitterItem extends Item {
	public CoreSplitterItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
