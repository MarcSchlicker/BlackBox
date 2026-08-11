package net.mcreator.blackbox.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.procedures.EmeraldBedrockTeleportProcedure;
import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.util.FarmEnvironment;
import net.mcreator.blackbox.BlackboxMod;

@EventBusSubscriber
public record EmeraldBedrockGUIButtonMessage(int buttonID, int x, int y, int z) implements CustomPacketPayload {
	public static final Type<EmeraldBedrockGUIButtonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "emerald_bedrock_gui_buttons"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EmeraldBedrockGUIButtonMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, EmeraldBedrockGUIButtonMessage message) -> {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}, (RegistryFriendlyByteBuf buffer) -> new EmeraldBedrockGUIButtonMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));

	@Override
	public Type<EmeraldBedrockGUIButtonMessage> type() {
		return TYPE;
	}

	public static void handleData(final EmeraldBedrockGUIButtonMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> handleButtonAction(context.player(), message.buttonID, message.x, message.y, message.z)).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		if (buttonID != 0 || !(entity instanceof ServerPlayer player) || !FarmEnvironment.isFarmDimension(player.level().dimension())) {
			return;
		}
		BlockPos floorPos = new BlockPos(x, y, z);
		if (!floorPos.closerToCenterThan(player.position(), 8.0D) || !player.level().getBlockState(floorPos).is(BlackboxModBlocks.EMERALD_BEDROCK.get())) {
			return;
		}
		player.closeContainer();
		EmeraldBedrockTeleportProcedure.execute(player);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(EmeraldBedrockGUIButtonMessage.TYPE, EmeraldBedrockGUIButtonMessage.STREAM_CODEC, EmeraldBedrockGUIButtonMessage::handleData);
	}
}
