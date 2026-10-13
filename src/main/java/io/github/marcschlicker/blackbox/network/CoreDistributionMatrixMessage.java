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
import io.github.marcschlicker.blackbox.world.inventory.CoreDistributionMatrixMenu;

/** All matrix changes are checked against the currently open server menu. */
@EventBusSubscriber
public record CoreDistributionMatrixMessage(int action, int x, int y, int z, int index) implements CustomPacketPayload {
	public static final Type<CoreDistributionMatrixMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "core_distribution_matrix"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CoreDistributionMatrixMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeVarInt(message.action);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeVarInt(message.index);
	}, buffer -> new CoreDistributionMatrixMessage(buffer.readVarInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readVarInt()));

	@Override
	public Type<CoreDistributionMatrixMessage> type() {
		return TYPE;
	}

	public static void handleData(CoreDistributionMatrixMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> {
			if (player.containerMenu instanceof CoreDistributionMatrixMenu matrix
					&& matrix.x == message.x && matrix.y == message.y && matrix.z == message.z) {
				matrix.handleAction(player, message.action, message.index);
			}
		}).exceptionally(error -> {
			context.connection().disconnect(Component.literal(error.getMessage()));
			return null;
		});
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, CoreDistributionMatrixMessage::handleData);
	}
}
