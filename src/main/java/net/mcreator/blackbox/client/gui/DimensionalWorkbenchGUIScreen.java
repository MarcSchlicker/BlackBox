package net.mcreator.blackbox.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.blackbox.world.inventory.DimensionalWorkbenchGUIMenu;
import net.mcreator.blackbox.network.DimensionalWorkbenchGUIButtonMessage;
import net.mcreator.blackbox.init.BlackboxModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class DimensionalWorkbenchGUIScreen extends AbstractContainerScreen<DimensionalWorkbenchGUIMenu> implements BlackboxModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	private EditBox Worldname;
	private Button button_name;
	private Button button_teleport;
	private Button button_end;
	private static final ResourceLocation BACKGROUND = ResourceLocation.parse("blackbox:textures/screens/dimensional_workbench_gui.png");

	public DimensionalWorkbenchGUIScreen(DimensionalWorkbenchGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		if (elementType == 0 && elementState instanceof String stringState) {
			if (name.equals("Worldname"))
				Worldname.setValue(stringState);
		}
		menuStateUpdateActive = false;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		Worldname.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		if (Worldname.isFocused())
			return Worldname.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String WorldnameValue = Worldname.getValue();
		super.resize(minecraft, width, height);
		Worldname.setValue(WorldnameValue);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public void init() {
		super.init();
		Worldname = new EditBox(this.font, this.leftPos + 41, this.topPos + 12, 118, 18, Component.translatable("gui.blackbox.dimensional_workbench_gui.Worldname"));
		Worldname.setMaxLength(8192);
		Worldname.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "Worldname", content, false);
		});
		Worldname.setHint(Component.translatable("gui.blackbox.dimensional_workbench_gui.Worldname"));
		this.addWidget(this.Worldname);
		button_name = Button.builder(Component.translatable("gui.blackbox.dimensional_workbench_gui.button_name"), e -> {
			int x = DimensionalWorkbenchGUIScreen.this.x;
			int y = DimensionalWorkbenchGUIScreen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(0, x, y, z));
				DimensionalWorkbenchGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 11, this.topPos + 32, 46, 20).build();
		this.addRenderableWidget(button_name);
		button_teleport = Button.builder(Component.translatable("gui.blackbox.dimensional_workbench_gui.button_teleport"), e -> {
			int x = DimensionalWorkbenchGUIScreen.this.x;
			int y = DimensionalWorkbenchGUIScreen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(1, x, y, z));
				DimensionalWorkbenchGUIButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}).bounds(this.leftPos + 59, this.topPos + 32, 67, 20).build();
		this.addRenderableWidget(button_teleport);
		button_end = Button.builder(Component.translatable("gui.blackbox.dimensional_workbench_gui.button_end"), e -> {
			int x = DimensionalWorkbenchGUIScreen.this.x;
			int y = DimensionalWorkbenchGUIScreen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(2, x, y, z));
				DimensionalWorkbenchGUIButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}).bounds(this.leftPos + 129, this.topPos + 32, 40, 20).build();
		this.addRenderableWidget(button_end);
	}
}