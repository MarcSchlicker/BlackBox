package net.mcreator.blackbox.network;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.item.BlueprintItem;
import net.mcreator.blackbox.util.BlueprintLibrary;
import net.mcreator.blackbox.util.FarmEnvironment;

@EventBusSubscriber
public record BlueprintSaveRequestMessage(InteractionHand hand, String name) implements CustomPacketPayload {
	public static final Type<BlueprintSaveRequestMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "blueprint_save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintSaveRequestMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeBoolean(message.hand == InteractionHand.OFF_HAND);
		buffer.writeUtf(message.name, 48);
	}, buffer -> new BlueprintSaveRequestMessage(buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, buffer.readUtf(48)));

	@Override
	public Type<BlueprintSaveRequestMessage> type() {
		return TYPE;
	}

	public static void handleData(BlueprintSaveRequestMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> {
			if (!(player.level() instanceof ServerLevel level) || !FarmEnvironment.isFarmDimension(level.dimension())) {
				return;
			}
			ItemStack blueprint = player.getItemInHand(message.hand);
			if (!blueprint.is(BlackboxModItems.BLUEPRINT.get())) {
				return;
			}
			BlueprintItem.clearLegacyItemData(blueprint);
			BlueprintItem.StorageScope storageScope = BlueprintItem.StorageScope.fromId(player.getData(BlackboxModVariables.PLAYER_VARIABLES).BlueprintPreferredStorage);
			BlueprintLibrary.save(player, level, blueprint, storageScope, message.name);
		}).exceptionally(error -> {
			context.connection().disconnect(Component.literal(error.getMessage()));
			return null;
		});
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, BlueprintSaveRequestMessage::handleData);
	}
}
