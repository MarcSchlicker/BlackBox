
package net.mcreator.blackbox.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class DimensionCoreItem extends Item {
	public DimensionCoreItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
	}
}
