
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.blackbox.block.entity.OutputBlockBlockEntity;
import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.block.entity.BlackboxBlockBlockEntity;
import net.mcreator.blackbox.BlackboxMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class BlackboxModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BlackboxMod.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> BLACKBOX_BLOCK = register("blackbox_block", BlackboxModBlocks.BLACKBOX_BLOCK, BlackboxBlockBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> DIMENSIONAL_WORKBENCH = register("dimensional_workbench", BlackboxModBlocks.DIMENSIONAL_WORKBENCH, DimensionalWorkbenchBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> OUTPUT_BLOCK = register("output_block", BlackboxModBlocks.OUTPUT_BLOCK, OutputBlockBlockEntity::new);

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLACKBOX_BLOCK.get(), (blockEntity, side) -> ((BlackboxBlockBlockEntity) blockEntity).getItemHandler());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DIMENSIONAL_WORKBENCH.get(), (blockEntity, side) -> ((DimensionalWorkbenchBlockEntity) blockEntity).getItemHandler());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, OUTPUT_BLOCK.get(), (blockEntity, side) -> ((OutputBlockBlockEntity) blockEntity).getItemHandler());
	}
}
