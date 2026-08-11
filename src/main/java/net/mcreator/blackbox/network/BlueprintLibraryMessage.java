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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.client.BlueprintLibraryClient;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.item.BlueprintItem;
import net.mcreator.blackbox.item.BlueprintItem.StorageScope;
import net.mcreator.blackbox.util.BlueprintLibrary;
import net.mcreator.blackbox.util.BlueprintLibrary.BlueprintSummary;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public record BlueprintLibraryMessage(int action, InteractionHand hand, String blueprintId, String blueprintName, StorageScope scope, StorageScope preferredScope,
		List<BlueprintSummary> entries) implements CustomPacketPayload {
	private static final int REQUEST = 0;
	private static final int OPEN = 1;
	private static final int SELECT = 2;
	private static final int SET_SCOPE = 3;
	private static final int DOWNLOAD = 4;
	private static final int MAX_ENTRIES = 256;
	public static final Type<BlueprintLibraryMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "blueprint_library"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintLibraryMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeVarInt(message.action);
		buffer.writeBoolean(message.hand == InteractionHand.OFF_HAND);
		buffer.writeUtf(message.blueprintId, 36);
		buffer.writeUtf(message.blueprintName, 64);
		buffer.writeBoolean(message.scope == StorageScope.SERVER);
		buffer.writeBoolean(message.preferredScope == StorageScope.SERVER);
		buffer.writeVarInt(message.entries.size());
		for (BlueprintSummary entry : message.entries) {
			buffer.writeUtf(entry.id(), 36);
			buffer.writeUtf(entry.name(), 64);
			buffer.writeUtf(entry.author(), 32);
			buffer.writeVarInt(entry.blockCount());
		}
	}, buffer -> {
		int action = buffer.readVarInt();
		InteractionHand hand = buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		String blueprintId = buffer.readUtf(36);
		String blueprintName = buffer.readUtf(64);
		StorageScope scope = buffer.readBoolean() ? StorageScope.SERVER : StorageScope.LOCAL;
		StorageScope preferredScope = buffer.readBoolean() ? StorageScope.SERVER : StorageScope.LOCAL;
		int count = buffer.readVarInt();
		if (count < 0 || count > MAX_ENTRIES) {
			throw new IllegalArgumentException("Invalid blueprint library size: " + count);
		}
		List<BlueprintSummary> entries = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			entries.add(new BlueprintSummary(buffer.readUtf(36), buffer.readUtf(64), buffer.readUtf(32), buffer.readVarInt()));
		}
		return new BlueprintLibraryMessage(action, hand, blueprintId, blueprintName, scope, preferredScope, List.copyOf(entries));
	});

	@Override
	public Type<BlueprintLibraryMessage> type() {
		return TYPE;
	}

	public static void sendLibrary(ServerPlayer player, InteractionHand hand) {
		BlackboxModVariables.PlayerVariables variables = player.getData(BlackboxModVariables.PLAYER_VARIABLES);
		StorageScope selectedScope = StorageScope.fromId(variables.BlueprintSelectionScope);
		StorageScope preferredStorage = StorageScope.fromId(variables.BlueprintPreferredStorage);
		PacketDistributor.sendToPlayer(player, new BlueprintLibraryMessage(OPEN, hand, variables.BlueprintSelectionId, variables.BlueprintSelectionName,
				selectedScope, preferredStorage, BlueprintLibrary.list(player.server)));
	}

	public static BlueprintLibraryMessage select(InteractionHand hand, String blueprintId, String blueprintName, StorageScope scope) {
		return new BlueprintLibraryMessage(SELECT, hand, blueprintId, blueprintName, scope, StorageScope.LOCAL, List.of());
	}

	public static BlueprintLibraryMessage setScope(InteractionHand hand, StorageScope scope) {
		return new BlueprintLibraryMessage(SET_SCOPE, hand, "", "", StorageScope.LOCAL, scope, List.of());
	}

	public static BlueprintLibraryMessage download(InteractionHand hand, String blueprintId) {
		return new BlueprintLibraryMessage(DOWNLOAD, hand, blueprintId, "", StorageScope.SERVER, StorageScope.LOCAL, List.of());
	}

	public static void handleData(BlueprintLibraryMessage message, IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND && context.player() instanceof ServerPlayer player) {
			context.enqueueWork(() -> handleServer(message, player)).exceptionally(error -> {
				context.connection().disconnect(Component.literal(error.getMessage()));
				return null;
			});
		} else if (context.flow() == PacketFlow.CLIENTBOUND && message.action == OPEN) {
			context.enqueueWork(() -> BlueprintLibraryClient.open(message.entries, message.blueprintId, message.blueprintName, message.scope, message.preferredScope, message.hand));
		}
	}

	private static void handleServer(BlueprintLibraryMessage message, ServerPlayer player) {
		if (message.action == REQUEST) {
			sendLibrary(player, message.hand);
			return;
		}
		ItemStack held = player.getItemInHand(message.hand);
		if (!held.is(BlackboxModItems.BLUEPRINT.get())) {
			return;
		}
		BlackboxModVariables.PlayerVariables variables = player.getData(BlackboxModVariables.PLAYER_VARIABLES);
		if (message.action == SET_SCOPE) {
			variables.BlueprintPreferredStorage = message.preferredScope.id();
			variables.markSyncDirty();
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.storage_selected." + message.preferredScope.id()), true);
			return;
		}
		if (message.action == DOWNLOAD) {
			BlueprintSummary summary = BlueprintLibrary.find(player.server, message.blueprintId);
			byte[] data = BlueprintLibrary.loadCompressed(player.server, message.blueprintId);
			if (summary != null && data.length > 0) {
				BlueprintTransferMessage.sendLocalSave(player, summary.id(), summary.name(), data);
			}
			return;
		}
		if (message.action != SELECT) {
			return;
		}
		if (message.scope == StorageScope.LOCAL) {
			String name = message.blueprintName.trim();
			if (!message.blueprintId.matches("[0-9a-fA-F-]{36}") || name.isEmpty() || name.length() > 64) {
				return;
			}
			setSelection(variables, message.blueprintId, name, StorageScope.LOCAL);
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.selected", name), true);
			return;
		}
		BlueprintSummary summary = BlueprintLibrary.find(player.server, message.blueprintId);
		if (summary == null) {
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.not_on_server"), true);
			return;
		}
		setSelection(variables, summary.id(), summary.name(), StorageScope.SERVER);
		player.displayClientMessage(Component.translatable("message.blackbox.blueprint.selected", summary.name()), true);
	}

	private static void setSelection(BlackboxModVariables.PlayerVariables variables, String id, String name, StorageScope scope) {
		variables.BlueprintSelectionId = id;
		variables.BlueprintSelectionName = name;
		variables.BlueprintSelectionScope = scope.id();
		variables.markSyncDirty();
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, BlueprintLibraryMessage::handleData);
	}
}
