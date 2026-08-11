package net.mcreator.blackbox.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.client.BlueprintLibraryClient;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.item.BlueprintItem;
import net.mcreator.blackbox.item.BlueprintItem.StorageScope;
import net.mcreator.blackbox.util.BlueprintLibrary;
import net.mcreator.blackbox.util.FarmEnvironment;

import java.util.Arrays;

@EventBusSubscriber
public record BlueprintTransferMessage(int action, InteractionHand hand, String blueprintId, String blueprintName, byte[] data) implements CustomPacketPayload {
	private static final int SAVE_LOCAL = 0;
	private static final int APPLY_LOCAL = 1;
	private static final int PUBLISH_SERVER = 2;
	public static final Type<BlueprintTransferMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "blueprint_transfer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintTransferMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeVarInt(message.action);
		buffer.writeBoolean(message.hand == InteractionHand.OFF_HAND);
		buffer.writeUtf(message.blueprintId, 36);
		buffer.writeUtf(message.blueprintName, 64);
		buffer.writeByteArray(message.data);
	}, buffer -> new BlueprintTransferMessage(buffer.readVarInt(), buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
			buffer.readUtf(36), buffer.readUtf(64), buffer.readByteArray(BlueprintLibrary.MAX_TRANSFER_BYTES)));

	public BlueprintTransferMessage {
		data = Arrays.copyOf(data, data.length);
	}

	@Override
	public Type<BlueprintTransferMessage> type() {
		return TYPE;
	}

	public static void sendLocalSave(ServerPlayer player, String id, String name, byte[] data) {
		PacketDistributor.sendToPlayer(player, new BlueprintTransferMessage(SAVE_LOCAL, InteractionHand.MAIN_HAND, id, name, data));
	}

	public static BlueprintTransferMessage applyLocal(InteractionHand hand, String id, byte[] data) {
		return new BlueprintTransferMessage(APPLY_LOCAL, hand, id, "", data);
	}

	public static BlueprintTransferMessage publishServer(InteractionHand hand, String id, byte[] data) {
		return new BlueprintTransferMessage(PUBLISH_SERVER, hand, id, "", data);
	}

	public static void handleData(BlueprintTransferMessage message, IPayloadContext context) {
		if (context.flow() == PacketFlow.CLIENTBOUND && message.action == SAVE_LOCAL) {
			context.enqueueWork(() -> BlueprintLibraryClient.saveLocal(message.blueprintName, message.data));
			return;
		}
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> handleServer(message, player));
	}

	private static void handleServer(BlueprintTransferMessage message, ServerPlayer player) {
		ItemStack held = player.getItemInHand(message.hand);
		BlackboxModVariables.PlayerVariables variables = player.getData(BlackboxModVariables.PLAYER_VARIABLES);
		if (!held.is(BlackboxModItems.BLUEPRINT.get()) || StorageScope.fromId(variables.BlueprintSelectionScope) != StorageScope.LOCAL
				|| !variables.BlueprintSelectionId.equals(message.blueprintId)) {
			return;
		}
		if (message.action == APPLY_LOCAL && player.level() instanceof ServerLevel level && FarmEnvironment.isFarmDimension(level.dimension())) {
			BlueprintLibrary.applyUploaded(player, level, message.blueprintId, message.data);
		} else if (message.action == PUBLISH_SERVER) {
			BlueprintLibrary.storeUploaded(player, message.data);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, BlueprintTransferMessage::handleData);
	}
}
