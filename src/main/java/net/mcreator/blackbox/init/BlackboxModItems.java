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
import net.mcreator.blackbox.item.HandbookItem;
import net.mcreator.blackbox.item.AdminBookItem;
import net.mcreator.blackbox.item.CoreEnvironmentUpgradeItem;
import net.mcreator.blackbox.item.StabilityUpgradeItem;
import net.mcreator.blackbox.item.MobSpawnUpgradeItem;
import net.mcreator.blackbox.item.BlueprintItem;
import net.mcreator.blackbox.util.FarmEnvironment;
import net.mcreator.blackbox.BlackboxMod;

public class BlackboxModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(BlackboxMod.MODID);
	public static final DeferredItem<Item> OPFERSCHWERT;
	public static final DeferredItem<Item> EMERALD_BEDROCK;
	public static final DeferredItem<Item> BLACKBOX_BLOCK;
	public static final DeferredItem<Item> DIMENSIONAL_WORKBENCH;
	public static final DeferredItem<Item> DIMENSION_CORE;
	public static final DeferredItem<Item> OUTPUT_BLOCK;
	public static final DeferredItem<Item> INPUTBLOCK;
	public static final DeferredItem<Item> HANDBOOK;
	public static final DeferredItem<Item> ADMIN_BOOK;
	public static final DeferredItem<Item> STANDARD_ENVIRONMENT_UPGRADE;
	public static final DeferredItem<Item> OVERWORLD_ENVIRONMENT_UPGRADE;
	public static final DeferredItem<Item> NETHER_ENVIRONMENT_UPGRADE;
	public static final DeferredItem<Item> END_ENVIRONMENT_UPGRADE;
	public static final DeferredItem<Item> STABILITY_UPGRADE;
	public static final DeferredItem<Item> MOB_SPAWN_UPGRADE;
	public static final DeferredItem<Item> BLUEPRINT;
	static {
		OPFERSCHWERT = REGISTRY.register("opferschwert", OpferschwertItem::new);
		EMERALD_BEDROCK = block(BlackboxModBlocks.EMERALD_BEDROCK);
		BLACKBOX_BLOCK = block(BlackboxModBlocks.BLACKBOX_BLOCK);
		DIMENSIONAL_WORKBENCH = block(BlackboxModBlocks.DIMENSIONAL_WORKBENCH);
		DIMENSION_CORE = REGISTRY.register("dimension_core", DimensionCoreItem::new);
		OUTPUT_BLOCK = block(BlackboxModBlocks.OUTPUT_BLOCK);
		INPUTBLOCK = block(BlackboxModBlocks.INPUTBLOCK);
		HANDBOOK = REGISTRY.register("handbook", HandbookItem::new);
		ADMIN_BOOK = REGISTRY.register("admin_book", AdminBookItem::new);
		STANDARD_ENVIRONMENT_UPGRADE = REGISTRY.register("standard_environment_upgrade", () -> new CoreEnvironmentUpgradeItem(FarmEnvironment.STANDARD));
		OVERWORLD_ENVIRONMENT_UPGRADE = REGISTRY.register("overworld_environment_upgrade", () -> new CoreEnvironmentUpgradeItem(FarmEnvironment.OVERWORLD));
		NETHER_ENVIRONMENT_UPGRADE = REGISTRY.register("nether_environment_upgrade", () -> new CoreEnvironmentUpgradeItem(FarmEnvironment.NETHER));
		END_ENVIRONMENT_UPGRADE = REGISTRY.register("end_environment_upgrade", () -> new CoreEnvironmentUpgradeItem(FarmEnvironment.END));
		STABILITY_UPGRADE = REGISTRY.register("stability_upgrade", StabilityUpgradeItem::new);
		MOB_SPAWN_UPGRADE = REGISTRY.register("mob_spawn_upgrade", MobSpawnUpgradeItem::new);
		BLUEPRINT = REGISTRY.register("blueprint", BlueprintItem::new);
	}

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return block(block, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block, Item.Properties properties) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}
