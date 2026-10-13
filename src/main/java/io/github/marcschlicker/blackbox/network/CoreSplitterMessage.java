package io.github.marcschlicker.blackbox.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import io.github.marcschlicker.blackbox.BlackboxMod;
import io.github.marcschlicker.blackbox.world.inventory.CoreSplitterMenu;

@EventBusSubscriber
public record CoreSplitterMessage(int action) implements CustomPacketPayload {
	public static final int ARM_SPLIT = 0;
	public static final int CONFIRM_SPLIT = 1;
	public static final Type<CoreSplitterMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "core_splitter"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CoreSplitterMessage> STREAM_CODEC = StreamCodec.of(
				(buffer, message) -> buffer.writeVarInt(message.action), buffer -> new CoreSplitterMessage(buffer.readVarInt()));

	@Override
	public Type<CoreSplitterMessage> type() {
		return TYPE;
	}

	public static void handleData(CoreSplitterMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> {
			if (player.containerMenu instanceof CoreSplitterMenu splitter) {
				splitter.handleAction(player, message.action);
			}
		}).exceptionally(error -> {
			context.connection().disconnect(Component.literal(error.getMessage()));
			return null;
		});
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, CoreSplitterMessage::handleData);
	}
}
