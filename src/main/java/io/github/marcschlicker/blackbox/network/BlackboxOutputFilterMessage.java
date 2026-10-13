package io.github.marcschlicker.blackbox.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.network.PacketDistributor;

import io.github.marcschlicker.blackbox.BlackboxMod;
import io.github.marcschlicker.blackbox.block.entity.BlackboxBlockBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.SpawnerBlackboxBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModBlocks;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.util.BlackboxSimulationRuntime;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.util.LiveMeasurementOutputs;
import io.github.marcschlicker.blackbox.util.OutputFilterData;
import io.github.marcschlicker.blackbox.world.inventory.BlackBoxGuiMenu;

@EventBusSubscriber
public record BlackboxOutputFilterMessage(int action, int x, int y, int z, int outputIndex, String spawnerOutputGroup) implements CustomPacketPayload {
	public static final int TOGGLE_OUTPUT = 0;
	public static final int ENABLE_ALL = 1;
	public static final int TOGGLE_FLUID = 2;
	public static final int TOGGLE_ENERGY = 3;
	public static final int TOGGLE_SPAWNER_ARMOR = 4;
	public static final int TOGGLE_SPAWNER_GEMS = 5;
	public static final Type<BlackboxOutputFilterMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "blackbox_output_filter"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlackboxOutputFilterMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeVarInt(message.action);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeVarInt(message.outputIndex);
		buffer.writeUtf(message.spawnerOutputGroup, 512);
	}, buffer -> new BlackboxOutputFilterMessage(buffer.readVarInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readVarInt(), buffer.readUtf(512)));

	@Override
	public Type<BlackboxOutputFilterMessage> type() {
		return TYPE;
	}

	public static void handleData(BlackboxOutputFilterMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> handleServer(player, message)).exceptionally(error -> {
			context.connection().disconnect(Component.literal(error.getMessage()));
			return null;
		});
	}

	private static void handleServer(ServerPlayer player, BlackboxOutputFilterMessage message) {
		BlockPos pos = new BlockPos(message.x, message.y, message.z);
		if (!pos.closerToCenterThan(player.position(), 8.0D)
				|| !(player.containerMenu instanceof BlackBoxGuiMenu menu)
				|| menu.x != message.x || menu.y != message.y || menu.z != message.z
				|| !(player.level().getBlockEntity(pos) instanceof BlackboxBlockBlockEntity blackbox)) {
			reject(player, message);
			return;
		}
		boolean spawnerMode = player.level().getBlockState(pos).is(BlackboxModBlocks.SPAWNER_BLACKBOX.get()) && blackbox instanceof SpawnerBlackboxBlockEntity;
		if (!spawnerMode && !player.level().getBlockState(pos).is(BlackboxModBlocks.BLACKBOX_BLOCK.get())) {
			reject(player, message);
			return;
		}
		ItemStack core = blackbox.getItem(BlackboxBlockBlockEntity.CORE_SLOT);
		ItemStack filter = blackbox.getItem(BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT);
		if (!filter.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get())) {
			reject(player, message);
			return;
		}
		if (spawnerMode) {
			SpawnerBlackboxBlockEntity spawner = (SpawnerBlackboxBlockEntity) blackbox;
			if (!spawner.isReady()) {
				reject(player, message);
				return;
			}
			if (message.action == TOGGLE_OUTPUT) {
				java.util.List<SpawnerBlackboxBlockEntity.DropStatistic> drops = spawner.observedDrops();
				SpawnerBlackboxBlockEntity.DropStatistic drop = null;
				if (!message.spawnerOutputGroup.isBlank()) {
					for (SpawnerBlackboxBlockEntity.DropStatistic observed : drops) {
						if (message.spawnerOutputGroup.equals(observed.filterGroup())) {
							drop = observed;
							break;
						}
					}
				} else if (message.outputIndex >= 0 && message.outputIndex < drops.size()) {
					drop = drops.get(message.outputIndex);
				}
				if (drop == null) {
					reject(player, message);
					return;
				}
				OutputFilterData.toggleSpawnerDrop(filter, drop.stack(), drop.filterGroup(), player.level().registryAccess());
			} else if (message.action == TOGGLE_SPAWNER_ARMOR) {
				OutputFilterData.toggleSpawnerArmor(filter);
			} else if (message.action == TOGGLE_SPAWNER_GEMS) {
				OutputFilterData.toggleSpawnerGems(filter);
			} else if (message.action == ENABLE_ALL) {
				OutputFilterData.enableAll(filter);
			} else {
				reject(player, message);
				return;
			}
		} else {
			boolean measuring = blackbox.getCalculationPhase() > 0;
			if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || (!FarmCoreData.isProgrammed(core) && !measuring)) {
				reject(player, message);
				return;
			}
			if (measuring) {
				if (message.action == TOGGLE_OUTPUT) {
					java.util.List<FarmCoreData.StackAmount> outputs = LiveMeasurementOutputs.items(blackbox);
					if (message.outputIndex < 0 || message.outputIndex >= outputs.size()) {
						reject(player, message);
						return;
					}
					OutputFilterData.toggle(filter, outputs.get(message.outputIndex).stack(), player.level().registryAccess());
				} else if (message.action == TOGGLE_FLUID) {
					java.util.List<FarmCoreData.FluidAmount> outputs = LiveMeasurementOutputs.fluids(blackbox);
					if (message.outputIndex < 0 || message.outputIndex >= outputs.size()) {
						reject(player, message);
						return;
					}
					OutputFilterData.toggleFluid(filter, outputs.get(message.outputIndex).stack(), player.level().registryAccess());
				} else if (message.action == TOGGLE_ENERGY) {
					if (LiveMeasurementOutputs.energy(blackbox) <= 0) {
						reject(player, message);
						return;
					}
					OutputFilterData.toggleEnergy(filter);
				} else if (message.action == ENABLE_ALL) {
					OutputFilterData.enableAll(filter);
				} else {
					reject(player, message);
					return;
				}
			} else {
				FarmCoreData.Recipe recipe = FarmCoreData.read(core, player.level().registryAccess());
				if (!recipe.isValid()) {
					reject(player, message);
					return;
				}
				if (message.action == TOGGLE_OUTPUT) {
					if (message.outputIndex < 0 || message.outputIndex >= recipe.outputTemplates().size()) {
						reject(player, message);
						return;
					}
					OutputFilterData.toggle(filter, recipe.outputTemplates().get(message.outputIndex).stack(), player.level().registryAccess());
				} else if (message.action == TOGGLE_FLUID) {
					if (message.outputIndex < 0 || message.outputIndex >= recipe.fluidOutputs().size()) {
						reject(player, message);
						return;
					}
					OutputFilterData.toggleFluid(filter, recipe.fluidOutputs().get(message.outputIndex).stack(), player.level().registryAccess());
				} else if (message.action == TOGGLE_ENERGY) {
					if (recipe.energyOutput() <= 0) {
						reject(player, message);
						return;
					}
					OutputFilterData.toggleEnergy(filter);
				} else if (message.action == ENABLE_ALL) {
					OutputFilterData.enableAll(filter);
				} else {
					reject(player, message);
					return;
				}
			}
		}
		BlackboxSimulationRuntime.discardFilteredPendingOutputs(blackbox, player.level().registryAccess());
		// The filter stores its state inside the upgrade ItemStack.  Explicitly
		// dirty its menu slot so an already-open client receives the new item data.
		menu.getSlot(BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT).setChanged();
		blackbox.setChanged();
		blackbox.syncToClient();
		menu.broadcastChanges();
		PacketDistributor.sendToPlayer(player, new BlackboxOutputFilterStateMessage(message.x, message.y, message.z, true,
				filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()));
	}

	private static void reject(ServerPlayer player, BlackboxOutputFilterMessage message) {
		PacketDistributor.sendToPlayer(player, new BlackboxOutputFilterStateMessage(message.x, message.y, message.z, false, new net.minecraft.nbt.CompoundTag()));
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, BlackboxOutputFilterMessage::handleData);
	}
}
