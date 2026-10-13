package io.github.marcschlicker.blackbox.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.github.marcschlicker.blackbox.BlackboxMod;
import io.github.marcschlicker.blackbox.client.gui.BlackBoxGuiScreen;

/** Server-confirmed filter data for the currently open BlackBox interface. */
@EventBusSubscriber
public record BlackboxOutputFilterStateMessage(int x, int y, int z, boolean accepted, CompoundTag filterData) implements CustomPacketPayload {
	public static final Type<BlackboxOutputFilterStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "blackbox_output_filter_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlackboxOutputFilterStateMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeBoolean(message.accepted);
		buffer.writeNbt(message.filterData);
	}, buffer -> {
		int x = buffer.readInt();
		int y = buffer.readInt();
		int z = buffer.readInt();
		boolean accepted = buffer.readBoolean();
		CompoundTag filterData = buffer.readNbt();
		return new BlackboxOutputFilterStateMessage(x, y, z, accepted, filterData == null ? new CompoundTag() : filterData);
	});

	@Override
	public Type<BlackboxOutputFilterStateMessage> type() {
		return TYPE;
	}

	public static void handleData(BlackboxOutputFilterStateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND) {
			return;
		}
		context.enqueueWork(() -> {
			if (Minecraft.getInstance().screen instanceof BlackBoxGuiScreen screen) {
				screen.applyConfirmedFilterState(message.x, message.y, message.z, message.accepted, message.filterData);
			}
		}).exceptionally(error -> {
			context.connection().disconnect(Component.literal(error.getMessage()));
			return null;
		});
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(TYPE, STREAM_CODEC, BlackboxOutputFilterStateMessage::handleData);
	}
}
