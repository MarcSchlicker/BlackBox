package io.github.marcschlicker.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import io.github.marcschlicker.blackbox.block.entity.OutputBlockBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.InputblockBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.BlackboxBlockBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.FarmActorBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.SpawnerBlackboxBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.CoreDistributionMatrixBlockEntity;
import io.github.marcschlicker.blackbox.BlackboxMod;

@EventBusSubscriber
public class BlackboxModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BlackboxMod.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlackboxBlockBlockEntity>> BLACKBOX_BLOCK = register("blackbox_block", BlackboxModBlocks.BLACKBOX_BLOCK, BlackboxBlockBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DimensionalWorkbenchBlockEntity>> DIMENSIONAL_WORKBENCH = register("dimensional_workbench", BlackboxModBlocks.DIMENSIONAL_WORKBENCH, DimensionalWorkbenchBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OutputBlockBlockEntity>> OUTPUT_BLOCK = register("output_block", BlackboxModBlocks.OUTPUT_BLOCK, OutputBlockBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InputblockBlockEntity>> INPUTBLOCK = register("inputblock", BlackboxModBlocks.INPUTBLOCK, InputblockBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FarmActorBlockEntity>> FARM_ACTOR = register("farm_actor", BlackboxModBlocks.FARM_ACTOR, FarmActorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpawnerBlackboxBlockEntity>> SPAWNER_BLACKBOX = register("spawner_blackbox", BlackboxModBlocks.SPAWNER_BLACKBOX, SpawnerBlackboxBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoreDistributionMatrixBlockEntity>> CORE_DISTRIBUTION_MATRIX = register("core_distribution_matrix", BlackboxModBlocks.CORE_DISTRIBUTION_MATRIX, CoreDistributionMatrixBlockEntity::new);

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<T> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLACKBOX_BLOCK.get(),
				(blackbox, side) -> blackbox.hasItemAccess(side) ? new SidedInvWrapper(blackbox, side) : null);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DIMENSIONAL_WORKBENCH.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, OUTPUT_BLOCK.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INPUTBLOCK.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SPAWNER_BLACKBOX.get(), SidedInvWrapper::new);
	}
}
