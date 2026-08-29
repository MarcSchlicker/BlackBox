package net.mcreator.blackbox.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.util.FarmCoreData;
import net.mcreator.blackbox.util.FarmDimensionRuntime;

@EventBusSubscriber
public record FarmNameUpdateMessage(int x, int y, int z, String name) implements CustomPacketPayload {
	public static final Type<FarmNameUpdateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "farm_name_update"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FarmNameUpdateMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeUtf(message.name, 32);
	}, buffer -> new FarmNameUpdateMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readUtf(32)));

	@Override
	public Type<FarmNameUpdateMessage> type() {
		return TYPE;
	}

	public static void handleData(FarmNameUpdateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> {
			BlockPos pos = new BlockPos(message.x, message.y, message.z);
			if (!pos.closerToCenterThan(player.position(), 8.0D) || !player.level().getBlockState(pos).is(BlackboxModBlocks.DIMENSIONAL_WORKBENCH.get())) {
				return;
			}
			if (player.level().getBlockEntity(pos) instanceof DimensionalWorkbenchBlockEntity workbench) {
				ItemStack core = workbench.getItem(0);
				if (core.is(BlackboxModItems.DIMENSION_CORE.get()) && !FarmCoreData.isVillageArchiveCore(core) && FarmCoreData.canManage(core, player)) {
					FarmCoreData.ensureOwner(core, player);
					FarmCoreData.setFarmName(core, message.name);
					workbench.setChanged();
					FarmDimensionRuntime.refreshFarmRecord(player.server, core);
				}
			}
		}).exceptionally(error -> {
			context.connection().disconnect(Component.literal(error.getMessage()));
			return null;
		});
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, FarmNameUpdateMessage::handleData);
	}
}
