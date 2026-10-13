package io.github.marcschlicker.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import io.github.marcschlicker.blackbox.item.OpferschwertItem;
import io.github.marcschlicker.blackbox.item.DimensionCoreItem;
import io.github.marcschlicker.blackbox.item.HandbookItem;
import io.github.marcschlicker.blackbox.item.AdminBookItem;
import io.github.marcschlicker.blackbox.item.CoreEnvironmentUpgradeItem;
import io.github.marcschlicker.blackbox.item.CoreCellSizeUpgradeItem;
import io.github.marcschlicker.blackbox.item.StabilityUpgradeItem;
import io.github.marcschlicker.blackbox.item.OutputFilterUpgradeItem;
import io.github.marcschlicker.blackbox.item.CoreSplitterItem;
import io.github.marcschlicker.blackbox.item.MobSpawnUpgradeItem;
import io.github.marcschlicker.blackbox.item.BlueprintItem;
import io.github.marcschlicker.blackbox.item.VillagerHeadItem;
import io.github.marcschlicker.blackbox.item.SpawnerCoreItem;
import io.github.marcschlicker.blackbox.item.SpawnerFilterUpgradeItem;
import io.github.marcschlicker.blackbox.util.FarmEnvironment;
import io.github.marcschlicker.blackbox.BlackboxMod;

public class BlackboxModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(BlackboxMod.MODID);
	public static final DeferredItem<Item> OPFERSCHWERT;
	public static final DeferredItem<Item> EMERALD_BEDROCK;
	public static final DeferredItem<Item> BLACKBOX_BLOCK;
	public static final DeferredItem<Item> DIMENSIONAL_WORKBENCH;
	public static final DeferredItem<Item> DIMENSION_CORE;
	public static final DeferredItem<Item> OUTPUT_BLOCK;
	public static final DeferredItem<Item> INPUTBLOCK;
	public static final DeferredItem<Item> FARM_ACTOR;
	public static final DeferredItem<Item> HANDBOOK;
	public static final DeferredItem<Item> ADMIN_BOOK;
	public static final DeferredItem<Item> STANDARD_ENVIRONMENT_UPGRADE;
	public static final DeferredItem<Item> OVERWORLD_ENVIRONMENT_UPGRADE;
	public static final DeferredItem<Item> NETHER_ENVIRONMENT_UPGRADE;
	public static final DeferredItem<Item> END_ENVIRONMENT_UPGRADE;
	public static final DeferredItem<Item> STABILITY_UPGRADE;
	public static final DeferredItem<Item> OUTPUT_FILTER_UPGRADE;
	public static final DeferredItem<Item> CORE_SPLITTER;
	public static final DeferredItem<Item> MOB_SPAWN_UPGRADE;
	public static final DeferredItem<Item> MEDIUM_CELL_UPGRADE;
	public static final DeferredItem<Item> LARGE_CELL_UPGRADE;
	public static final DeferredItem<Item> BLUEPRINT;
	public static final DeferredItem<Item> VILLAGER_HEAD;
	public static final DeferredItem<Item> SPAWNER_CORE;
	public static final DeferredItem<Item> SPAWNER_ARMOR_FILTER_UPGRADE;
	public static final DeferredItem<Item> SPAWNER_GEM_FILTER_UPGRADE;
	public static final DeferredItem<Item> SPAWNER_BLACKBOX;
	public static final DeferredItem<Item> CORE_DISTRIBUTION_MATRIX;
	static {
		OPFERSCHWERT = REGISTRY.register("opferschwert", OpferschwertItem::new);
		EMERALD_BEDROCK = block(BlackboxModBlocks.EMERALD_BEDROCK);
		BLACKBOX_BLOCK = block(BlackboxModBlocks.BLACKBOX_BLOCK);
		DIMENSIONAL_WORKBENCH = block(BlackboxModBlocks.DIMENSIONAL_WORKBENCH);
		DIMENSION_CORE = REGISTRY.register("dimension_core", DimensionCoreItem::new);
		OUTPUT_BLOCK = block(BlackboxModBlocks.OUTPUT_BLOCK);
		INPUTBLOCK = block(BlackboxModBlocks.INPUTBLOCK);
		FARM_ACTOR = block(BlackboxModBlocks.FARM_ACTOR);
		HANDBOOK = REGISTRY.register("handbook", HandbookItem::new);
		ADMIN_BOOK = REGISTRY.register("admin_book", AdminBookItem::new);
		STANDARD_ENVIRONMENT_UPGRADE = REGISTRY.register("standard_environment_upgrade", () -> new CoreEnvironmentUpgradeItem(FarmEnvironment.STANDARD));
		OVERWORLD_ENVIRONMENT_UPGRADE = REGISTRY.register("overworld_environment_upgrade", () -> new CoreEnvironmentUpgradeItem(FarmEnvironment.OVERWORLD));
		NETHER_ENVIRONMENT_UPGRADE = REGISTRY.register("nether_environment_upgrade", () -> new CoreEnvironmentUpgradeItem(FarmEnvironment.NETHER));
		END_ENVIRONMENT_UPGRADE = REGISTRY.register("end_environment_upgrade", () -> new CoreEnvironmentUpgradeItem(FarmEnvironment.END));
		STABILITY_UPGRADE = REGISTRY.register("stability_upgrade", StabilityUpgradeItem::new);
		OUTPUT_FILTER_UPGRADE = REGISTRY.register("output_filter_upgrade", OutputFilterUpgradeItem::new);
		CORE_SPLITTER = REGISTRY.register("core_splitter", CoreSplitterItem::new);
		MOB_SPAWN_UPGRADE = REGISTRY.register("mob_spawn_upgrade", MobSpawnUpgradeItem::new);
		MEDIUM_CELL_UPGRADE = REGISTRY.register("medium_cell_upgrade", () -> new CoreCellSizeUpgradeItem(2));
		LARGE_CELL_UPGRADE = REGISTRY.register("large_cell_upgrade", () -> new CoreCellSizeUpgradeItem(3));
		BLUEPRINT = REGISTRY.register("blueprint", BlueprintItem::new);
		VILLAGER_HEAD = REGISTRY.register("villager_head", VillagerHeadItem::new);
		SPAWNER_CORE = REGISTRY.register("spawner_core", SpawnerCoreItem::new);
		SPAWNER_ARMOR_FILTER_UPGRADE = REGISTRY.register("spawner_armor_filter_upgrade", () -> new SpawnerFilterUpgradeItem(SpawnerFilterUpgradeItem.Kind.ARMOR));
		SPAWNER_GEM_FILTER_UPGRADE = REGISTRY.register("spawner_gem_filter_upgrade", () -> new SpawnerFilterUpgradeItem(SpawnerFilterUpgradeItem.Kind.GEM));
		SPAWNER_BLACKBOX = block(BlackboxModBlocks.SPAWNER_BLACKBOX);
		CORE_DISTRIBUTION_MATRIX = block(BlackboxModBlocks.CORE_DISTRIBUTION_MATRIX);
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
