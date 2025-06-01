
package net.mcreator.blackbox.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.world.inventory.DimensionalWorkbenchGUIMenu;
import net.mcreator.blackbox.procedures.TeleporttoDimensionProcedure;
import net.mcreator.blackbox.procedures.DimWBNamingProcedure;
import net.mcreator.blackbox.procedures.DimWBEndProcedure;
import net.mcreator.blackbox.BlackboxMod;

import java.util.HashMap;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record DimensionalWorkbenchGUIButtonMessage(int buttonID, int x, int y, int z) implements CustomPacketPayload {

	public static final Type<DimensionalWorkbenchGUIButtonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "dimensional_workbench_gui_buttons"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionalWorkbenchGUIButtonMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, DimensionalWorkbenchGUIButtonMessage message) -> {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}, (RegistryFriendlyByteBuf buffer) -> new DimensionalWorkbenchGUIButtonMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));
	@Override
	public Type<DimensionalWorkbenchGUIButtonMessage> type() {
		return TYPE;
	}

	public static void handleData(final DimensionalWorkbenchGUIButtonMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				Player entity = context.player();
				int buttonID = message.buttonID;
				int x = message.x;
				int y = message.y;
				int z = message.z;
				handleButtonAction(entity, buttonID, x, y, z);
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = entity.level();
		HashMap guistate = DimensionalWorkbenchGUIMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			DimWBNamingProcedure.execute(world, x, y, z, entity, guistate);
		}
		if (buttonID == 1) {

			TeleporttoDimensionProcedure.execute(x, y, z, entity);
		}
		if (buttonID == 2) {

			DimWBEndProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(DimensionalWorkbenchGUIButtonMessage.TYPE, DimensionalWorkbenchGUIButtonMessage.STREAM_CODEC, DimensionalWorkbenchGUIButtonMessage::handleData);
	}
}
