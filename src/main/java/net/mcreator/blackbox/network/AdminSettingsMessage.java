package net.mcreator.blackbox.network;

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

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.config.BlackboxConfig;
import net.mcreator.blackbox.world.inventory.AdminBookMenu;

@EventBusSubscriber
public record AdminSettingsMessage(int measurementSeconds) implements CustomPacketPayload {
	public static final Type<AdminSettingsMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "admin_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AdminSettingsMessage> STREAM_CODEC = StreamCodec.of(
			(buffer, message) -> buffer.writeVarInt(message.measurementSeconds),
			buffer -> new AdminSettingsMessage(buffer.readVarInt()));

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
			menu.setMeasurementSeconds(seconds);
		});
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, AdminSettingsMessage::handleData);
	}
}
