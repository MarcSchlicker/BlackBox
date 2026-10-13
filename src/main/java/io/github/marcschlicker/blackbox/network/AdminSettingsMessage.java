package io.github.marcschlicker.blackbox.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import io.github.marcschlicker.blackbox.BlackboxMod;
import io.github.marcschlicker.blackbox.config.BlackboxConfig;
import io.github.marcschlicker.blackbox.util.VillageArchiveRuntime;
import io.github.marcschlicker.blackbox.world.inventory.AdminBookMenu;

@EventBusSubscriber
public record AdminSettingsMessage(int measurementSeconds, boolean villageArchiveEnabled) implements CustomPacketPayload {
	public static final Type<AdminSettingsMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "admin_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AdminSettingsMessage> STREAM_CODEC = StreamCodec.of(
			(buffer, message) -> {
				buffer.writeVarInt(message.measurementSeconds);
				buffer.writeBoolean(message.villageArchiveEnabled);
			},
			buffer -> new AdminSettingsMessage(buffer.readVarInt(), buffer.readBoolean()));

	@Override
	public Type<AdminSettingsMessage> type() {
		return TYPE;
	}

	public static void handleData(AdminSettingsMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> {
			if (!player.hasPermissions(2) || !(player.containerMenu instanceof AdminBookMenu menu)) {
				return;
			}
			int seconds = Math.max(10, Math.min(3600, message.measurementSeconds));
			BlackboxConfig.setMeasurementSeconds(seconds);
			boolean changedVillageArchive = BlackboxConfig.isVillageArchiveEnabled() != message.villageArchiveEnabled;
			BlackboxConfig.setVillageArchiveEnabled(message.villageArchiveEnabled);
			menu.setMeasurementSeconds(seconds);
			menu.setVillageArchiveEnabled(message.villageArchiveEnabled);
			if (changedVillageArchive) {
				VillageArchiveRuntime.onModuleStateChanged(player.server, message.villageArchiveEnabled);
			}
		});
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, AdminSettingsMessage::handleData);
	}
}
