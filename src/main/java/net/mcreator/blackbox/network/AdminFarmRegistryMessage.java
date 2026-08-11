package net.mcreator.blackbox.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.client.AdminFarmRegistryClient;
import net.mcreator.blackbox.util.FarmDimensionRuntime;
import net.mcreator.blackbox.util.FarmWorldData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber
public record AdminFarmRegistryMessage(int action, String coreId, List<FarmEntry> entries) implements CustomPacketPayload {
	private static final int REQUEST = 0;
	private static final int OPEN = 1;
	private static final int DELETE = 2;
	private static final int MAX_ENTRIES = 4096;
	public static final Type<AdminFarmRegistryMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "admin_farm_registry"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AdminFarmRegistryMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeVarInt(message.action);
		buffer.writeUtf(message.coreId, 36);
		buffer.writeVarInt(message.entries.size());
		for (FarmEntry entry : message.entries) {
			buffer.writeUtf(entry.coreId(), 36);
			buffer.writeUtf(entry.name(), 32);
			buffer.writeUtf(entry.owner(), 32);
			buffer.writeUtf(entry.dimension(), 128);
			buffer.writeVarInt(entry.sizeChunks());
		}
	}, buffer -> {
		int action = buffer.readVarInt();
		String coreId = buffer.readUtf(36);
		int count = buffer.readVarInt();
		if (count < 0 || count > MAX_ENTRIES) {
			throw new IllegalArgumentException("Invalid farm registry size: " + count);
		}
		List<FarmEntry> entries = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			entries.add(new FarmEntry(buffer.readUtf(36), buffer.readUtf(32), buffer.readUtf(32), buffer.readUtf(128), buffer.readVarInt()));
		}
		return new AdminFarmRegistryMessage(action, coreId, List.copyOf(entries));
	});

	@Override
	public Type<AdminFarmRegistryMessage> type() {
		return TYPE;
	}

	public static AdminFarmRegistryMessage request() {
		return new AdminFarmRegistryMessage(REQUEST, "", List.of());
	}

	public static AdminFarmRegistryMessage delete(String coreId) {
		return new AdminFarmRegistryMessage(DELETE, coreId, List.of());
	}

	public static void handleData(AdminFarmRegistryMessage message, IPayloadContext context) {
		if (context.flow() == PacketFlow.CLIENTBOUND && message.action == OPEN) {
			context.enqueueWork(() -> AdminFarmRegistryClient.open(message.entries));
			return;
		}
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> {
			if (!player.hasPermissions(2)) {
				player.sendSystemMessage(Component.translatable("message.blackbox.admin.denied"));
				return;
			}
			if (message.action == DELETE) {
				try {
					FarmDimensionRuntime.deleteFarmCell(player.server, UUID.fromString(message.coreId));
				} catch (IllegalArgumentException ignored) {
				}
			}
			if (message.action == REQUEST || message.action == DELETE) {
				send(player);
			}
		});
	}

	private static void send(ServerPlayer player) {
		List<FarmEntry> entries = FarmDimensionRuntime.farmRecords(player.server).stream().limit(MAX_ENTRIES).map(FarmEntry::from).toList();
		PacketDistributor.sendToPlayer(player, new AdminFarmRegistryMessage(OPEN, "", entries));
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, AdminFarmRegistryMessage::handleData);
	}

	public record FarmEntry(String coreId, String name, String owner, String dimension, int sizeChunks) {
		private static FarmEntry from(FarmWorldData.FarmRecord record) {
			return new FarmEntry(record.coreId().toString(), record.name(), record.ownerName(), record.dimension(), record.sizeChunks());
		}
	}
}
