
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import net.mcreator.blackbox.item.OpferschwertItem;
import net.mcreator.blackbox.item.DimensionCoreItem;
import net.mcreator.blackbox.BlackboxMod;

public class BlackboxModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(BlackboxMod.MODID);
	public static final DeferredItem<Item> OPFERSCHWERT = REGISTRY.register("opferschwert", OpferschwertItem::new);
	public static final DeferredItem<Item> EMERALD_BEDROCK = block(BlackboxModBlocks.EMERALD_BEDROCK);
	public static final DeferredItem<Item> BLACKBOX_BLOCK = block(BlackboxModBlocks.BLACKBOX_BLOCK);
	public static final DeferredItem<Item> DIMENSIONAL_WORKBENCH = block(BlackboxModBlocks.DIMENSIONAL_WORKBENCH);
	public static final DeferredItem<Item> DIMENSION_CORE = REGISTRY.register("dimension_core", DimensionCoreItem::new);
	public static final DeferredItem<Item> OUTPUT_BLOCK = block(BlackboxModBlocks.OUTPUT_BLOCK);

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}
