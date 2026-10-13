package io.github.marcschlicker.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import io.github.marcschlicker.blackbox.block.OutputBlockBlock;
import io.github.marcschlicker.blackbox.block.InputblockBlock;
import io.github.marcschlicker.blackbox.block.EmeraldBedrockBlock;
import io.github.marcschlicker.blackbox.block.DimensionalWorkbenchBlock;
import io.github.marcschlicker.blackbox.block.BlackboxBlockBlock;
import io.github.marcschlicker.blackbox.block.FarmActorBlock;
import io.github.marcschlicker.blackbox.block.SpawnerBlackboxBlock;
import io.github.marcschlicker.blackbox.block.CoreDistributionMatrixBlock;
import io.github.marcschlicker.blackbox.BlackboxMod;

public class BlackboxModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(BlackboxMod.MODID);
	public static final DeferredBlock<Block> EMERALD_BEDROCK;
	public static final DeferredBlock<Block> BLACKBOX_BLOCK;
	public static final DeferredBlock<Block> DIMENSIONAL_WORKBENCH;
	public static final DeferredBlock<Block> OUTPUT_BLOCK;
	public static final DeferredBlock<Block> INPUTBLOCK;
	public static final DeferredBlock<Block> FARM_ACTOR;
	public static final DeferredBlock<Block> SPAWNER_BLACKBOX;
	public static final DeferredBlock<Block> CORE_DISTRIBUTION_MATRIX;
	static {
		EMERALD_BEDROCK = REGISTRY.register("emerald_bedrock", EmeraldBedrockBlock::new);
		BLACKBOX_BLOCK = REGISTRY.register("blackbox_block", BlackboxBlockBlock::new);
		DIMENSIONAL_WORKBENCH = REGISTRY.register("dimensional_workbench", DimensionalWorkbenchBlock::new);
		OUTPUT_BLOCK = REGISTRY.register("output_block", OutputBlockBlock::new);
		INPUTBLOCK = REGISTRY.register("inputblock", InputblockBlock::new);
		FARM_ACTOR = REGISTRY.register("farm_actor", FarmActorBlock::new);
		SPAWNER_BLACKBOX = REGISTRY.register("spawner_blackbox", SpawnerBlackboxBlock::new);
		CORE_DISTRIBUTION_MATRIX = REGISTRY.register("core_distribution_matrix", CoreDistributionMatrixBlock::new);
	}
	// Start of user code block custom blocks
	// End of user code block custom blocks
}
