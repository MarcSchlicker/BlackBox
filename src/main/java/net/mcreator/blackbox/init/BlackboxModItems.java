
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.item.Item;

import net.mcreator.blackbox.item.TestItem;
import net.mcreator.blackbox.item.OpferschwertItem;
import net.mcreator.blackbox.BlackboxMod;

public class BlackboxModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(BlackboxMod.MODID);
	public static final DeferredItem<Item> OPFERSCHWERT = REGISTRY.register("opferschwert", OpferschwertItem::new);
	public static final DeferredItem<Item> TEST = REGISTRY.register("test", TestItem::new);
	// Start of user code block custom items
	// End of user code block custom items
}
