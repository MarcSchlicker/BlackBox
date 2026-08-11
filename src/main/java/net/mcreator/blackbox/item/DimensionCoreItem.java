package net.mcreator.blackbox.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.util.FarmCoreData;

public class DimensionCoreItem extends Item {
	public DimensionCoreItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return FarmCoreData.isProgrammed(stack);
	}
}
