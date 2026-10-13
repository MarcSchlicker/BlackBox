package io.github.marcschlicker.blackbox.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import io.github.marcschlicker.blackbox.BlackboxMod;
import io.github.marcschlicker.blackbox.block.entity.BlackboxBlockBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModBlocks;
import io.github.marcschlicker.blackbox.world.inventory.BlackBoxGuiMenu;

@EventBusSubscriber
public record BlackboxSideConfigMessage(int x, int y, int z, int directionId) implements CustomPacketPayload {
	public static final Type<BlackboxSideConfigMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "blackbox_side_config"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlackboxSideConfigMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeVarInt(message.directionId);
	}, buffer -> new BlackboxSideConfigMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readVarInt()));

	@Override
	public Type<BlackboxSideConfigMessage> type() {
		return TYPE;
	}

	public static void handleData(BlackboxSideConfigMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> handleServer(player, message)).exceptionally(error -> {
			context.connection().disconnect(Component.literal(error.getMessage()));
			return null;
		});
	}

	private static void handleServer(ServerPlayer player, BlackboxSideConfigMessage message) {
		if (message.directionId < 0 || message.directionId >= Direction.values().length) {
			return;
		}
		BlockPos pos = new BlockPos(message.x, message.y, message.z);
		if (!pos.closerToCenterThan(player.position(), 8.0D)
				|| !player.level().getBlockState(pos).is(BlackboxModBlocks.BLACKBOX_BLOCK.get())
				|| !(player.containerMenu instanceof BlackBoxGuiMenu menu)
				|| menu.x != message.x || menu.y != message.y || menu.z != message.z
				|| !(player.level().getBlockEntity(pos) instanceof BlackboxBlockBlockEntity blackbox)) {
			return;
		}
		blackbox.cycleSideMode(Direction.from3DDataValue(message.directionId));
		blackbox.syncToClient();
		menu.broadcastChanges();
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, BlackboxSideConfigMessage::handleData);
	}
}
