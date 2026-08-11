package net.mcreator.blackbox.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.init.BlackboxModScreens;
import net.mcreator.blackbox.network.DimensionalWorkbenchGUIButtonMessage;
import net.mcreator.blackbox.network.FarmNameUpdateMessage;
import net.mcreator.blackbox.util.FarmCoreData;
import net.mcreator.blackbox.util.FarmAccessMode;
import net.mcreator.blackbox.world.inventory.DimensionalWorkbenchGUIMenu;

public class DimensionalWorkbenchGUIScreen extends AbstractContainerScreen<DimensionalWorkbenchGUIMenu> implements BlackboxModScreens.ScreenAccessor {
	private Button enterButton;
	private Button saveNameButton;
	private Button accessButton;
	private EditBox farmName;
	private boolean nameInitialized;

	public DimensionalWorkbenchGUIScreen(DimensionalWorkbenchGUIMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 196;
		this.imageHeight = 243;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
	}

	@Override
	protected void init() {
		super.init();
		this.farmName = new EditBox(this.font, this.leftPos + 30, this.topPos + 21, 95, 20, Component.translatable("gui.blackbox.workbench.name"));
		this.farmName.setMaxLength(32);
		this.farmName.setHint(Component.translatable("gui.blackbox.workbench.name"));
		this.addRenderableWidget(this.farmName);
		this.saveNameButton = this.addRenderableWidget(Button.builder(Component.literal("\u2713"), button -> saveName()).bounds(this.leftPos + 129, this.topPos + 21, 25, 20).build());
		this.saveNameButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.save_name")));
		this.enterButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.workbench.enter_short"), button -> {
			saveName();
			PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(0, this.menu.x, this.menu.y, this.menu.z));
		}).bounds(this.leftPos + 158, this.topPos + 21, 30, 20).build());
		this.enterButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.start")));
		this.accessButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(1, this.menu.x, this.menu.y, this.menu.z));
		}).bounds(this.leftPos + 8, this.topPos + 47, 58, 18).build());
		this.accessButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.access_tooltip")));
	}

	private void saveName() {
		PacketDistributor.sendToServer(new FarmNameUpdateMessage(this.menu.x, this.menu.y, this.menu.z, this.farmName.getValue()));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.farmName != null && this.farmName.isFocused()) {
			if (this.farmName.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
			if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		ItemStack core = this.menu.getSlot(0).getItem();
		boolean hasCore = core.is(BlackboxModItems.DIMENSION_CORE.get());
		this.enterButton.active = hasCore;
		this.saveNameButton.active = hasCore;
		this.farmName.setEditable(hasCore);
		this.accessButton.active = hasCore;
		FarmAccessMode access = hasCore ? FarmCoreData.getAccessMode(core) : FarmAccessMode.PRIVATE;
		this.accessButton.setMessage(Component.translatable("gui.blackbox.access." + access.id()));
		if (hasCore && !this.nameInitialized) {
			this.farmName.setValue(FarmCoreData.getFarmName(core));
			this.nameInitialized = true;
		} else if (!hasCore) {
			this.nameInitialized = false;
			this.farmName.setValue("");
		}
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawPanel(graphics);
		drawSlot(graphics, this.leftPos + 8, this.topPos + 24, 0xFF77CC88);
		drawSlot(graphics, this.leftPos + 170, this.topPos + 51, 0xFFD5B85A);
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 75, 0xFF5E8FB8);
		}
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 103 + row * 18, 0xFF77A96E);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 161 + row * 18, 0xFF68727D);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 219, 0xFF68727D);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.title"), 8, 6, 0xFFE8EDF2, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.upgrade_short"), 169, 42, 0xFFD8C37A, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.inputs"), 70, 64, 0xFF83BCE6, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.outputs"), 8, 93, 0xFF8DDB9B, false);
		graphics.drawString(this.font, Component.translatable("container.inventory"), 8, 150, 0xFFAAB4C0, false);

		Component status;
		int color;
		if (!this.menu.getSlot(0).getItem().is(BlackboxModItems.DIMENSION_CORE.get())) {
			status = Component.translatable("gui.blackbox.workbench.missing_core");
			color = 0xFFFF7373;
		} else if (this.menu.getCalculationPhase() == 1) {
			status = Component.translatable("gui.blackbox.workbench.analysing");
			color = 0xFFFFC866;
		} else if (this.menu.getCalculationPhase() == 2) {
			status = Component.translatable("gui.blackbox.workbench.measuring", this.menu.getCalculationSecondsRemaining());
			color = 0xFF73C7FF;
		} else if (FarmCoreData.isProgrammed(this.menu.getSlot(0).getItem())) {
			if (this.menu.getSlot(28).getItem().is(BlackboxModItems.STABILITY_UPGRADE.get())) {
				status = Component.translatable("gui.blackbox.machine.stable");
				color = 0xFF73C7FF;
			} else {
				status = Component.translatable("gui.blackbox.machine.timeline");
				color = 0xFF6EDC8C;
			}
		} else {
			status = Component.translatable("gui.blackbox.workbench.ready");
			color = 0xFFB8C2CC;
		}
		graphics.drawString(this.font, status, 70, 51, color, false);
		ItemStack core = this.menu.getSlot(0).getItem();
		if (core.is(BlackboxModItems.DIMENSION_CORE.get())) {
			int size = FarmCoreData.getCellSizeChunks(core);
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.cell_size", size, size), 130, 7, 0xFFA8B2BD, false);
		}
	}

	private void drawPanel(GuiGraphics graphics) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF171B21);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 2, 0xFF4C9B70);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF596572);
		graphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF080A0D);
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}
}
