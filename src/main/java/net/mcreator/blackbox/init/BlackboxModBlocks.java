/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import net.mcreator.blackbox.block.OutputBlockBlock;
import net.mcreator.blackbox.block.InputblockBlock;
import net.mcreator.blackbox.block.EmeraldBedrockBlock;
import net.mcreator.blackbox.block.DimensionalWorkbenchBlock;
import net.mcreator.blackbox.block.BlackboxBlockBlock;
import net.mcreator.blackbox.BlackboxMod;

public class BlackboxModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(BlackboxMod.MODID);
	public static final DeferredBlock<Block> EMERALD_BEDROCK;
	public static final DeferredBlock<Block> BLACKBOX_BLOCK;
	public static final DeferredBlock<Block> DIMENSIONAL_WORKBENCH;
	public static final DeferredBlock<Block> OUTPUT_BLOCK;
	public static final DeferredBlock<Block> INPUTBLOCK;
	static {
		EMERALD_BEDROCK = REGISTRY.register("emerald_bedrock", EmeraldBedrockBlock::new);
		BLACKBOX_BLOCK = REGISTRY.register("blackbox_block", BlackboxBlockBlock::new);
		DIMENSIONAL_WORKBENCH = REGISTRY.register("dimensional_workbench", DimensionalWorkbenchBlock::new);
		OUTPUT_BLOCK = REGISTRY.register("output_block", OutputBlockBlock::new);
		INPUTBLOCK = REGISTRY.register("inputblock", InputblockBlock::new);
	}
	// Start of user code block custom blocks
	// End of user code block custom blocks
}