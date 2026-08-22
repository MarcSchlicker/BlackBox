package net.mcreator.blackbox.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.world.inventory.HandbookMenu;

import java.util.ArrayList;
import java.util.List;

public class HandbookScreen extends AbstractContainerScreen<HandbookMenu> {
	private static final int GUIDE_PAGE_COUNT = 8;
	private static final int PAGE_COUNT = 16;
	private static final int[] SECTION_STARTS = {0, 2, 5, 6, 8};
	private static final String[] SECTION_KEYS = {"start", "measure", "mobs", "library", "recipes"};
	private static final int[] PAGE_ACCENTS = {
			0xFF5BA57B, 0xFF5C91BD, 0xFF4FAE91, 0xFFD6A84F,
			0xFF8E77C7, 0xFFC47C9A, 0xFF4B9EB5, 0xFFB17878
	};

	private int page;
	private Button previousButton;
	private Button nextButton;
	private final List<Button> sectionButtons = new ArrayList<>();

	public HandbookScreen(HandbookMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 286;
		this.imageHeight = 214;
	}

	@Override
	protected void init() {
		super.init();
		this.sectionButtons.clear();
		for (int index = 0; index < SECTION_STARTS.length; index++) {
			final int targetPage = SECTION_STARTS[index];
			Button button = this.addRenderableWidget(Button.builder(
					Component.translatable("gui.blackbox.handbook.section." + SECTION_KEYS[index]), ignored -> setPage(targetPage))
					.bounds(this.leftPos + 8 + index * 55, this.topPos + 31, 51, 18).build());
			button.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.handbook.section." + SECTION_KEYS[index] + ".tooltip")));
			this.sectionButtons.add(button);
		}
		this.previousButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> setPage(this.page - 1))
				.bounds(this.leftPos + 8, this.topPos + 188, 26, 18).build());
		this.nextButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> setPage(this.page + 1))
				.bounds(this.leftPos + this.imageWidth - 34, this.topPos + 188, 26, 18).build());
		updateButtons();
	}

	private void setPage(int target) {
		this.page = Math.max(0, Math.min(PAGE_COUNT - 1, target));
		updateButtons();
	}

	private void updateButtons() {
		this.previousButton.active = this.page > 0;
		this.nextButton.active = this.page < PAGE_COUNT - 1;
		int section = sectionForPage(this.page);
		for (int index = 0; index < this.sectionButtons.size(); index++) {
			this.sectionButtons.get(index).active = index != section;
		}
	}

	private int sectionForPage(int targetPage) {
		int section = 0;
		for (int index = 0; index < SECTION_STARTS.length; index++) {
			if (targetPage >= SECTION_STARTS[index]) {
				section = index;
			}
		}
		return section;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF12171C);
		graphics.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + this.imageWidth - 1, this.topPos + 27, 0xFF24323A);
		graphics.fill(this.leftPos + 8, this.topPos + 55, this.leftPos + this.imageWidth - 8, this.topPos + 181, 0xFFF0F3F2);
		graphics.fill(this.leftPos + 8, this.topPos + 55, this.leftPos + 12, this.topPos + 181,
				this.page < GUIDE_PAGE_COUNT ? PAGE_ACCENTS[this.page] : 0xFF5BA57B);
		graphics.fill(this.leftPos + 1, this.topPos + this.imageHeight - 2, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, 0xFF4D6571);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.handbook.title"), 10, 10, 0xFFF2F5F6, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.handbook.subtitle"), 176, 10, 0xFF9DB2BD, false);
		if (this.page < GUIDE_PAGE_COUNT) {
			renderGuidePage(graphics);
		} else {
			renderRecipePage(graphics, this.page - GUIDE_PAGE_COUNT);
		}
		graphics.drawCenteredString(this.font, Component.literal((this.page + 1) + " / " + PAGE_COUNT), this.imageWidth / 2, 194, 0xFF98A5AD);
	}

	private void renderGuidePage(GuiGraphics graphics) {
		int accent = PAGE_ACCENTS[this.page];
		graphics.fill(19, 63, 39, 83, accent);
		graphics.drawCenteredString(this.font, Component.literal(Integer.toString(this.page + 1)), 29, 69, 0xFFFFFFFF);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.handbook.page." + this.page + ".title"), 47, 64, 0xFF172229, false);
		graphics.drawWordWrap(this.font, Component.translatable("gui.blackbox.handbook.page." + this.page + ".body"), 19, 91, 248, 0xFF2E3A40);
		graphics.fill(19, 151, 267, 173, 0xFFDCE5E2);
		graphics.fill(19, 151, 22, 173, accent);
		graphics.drawWordWrap(this.font, Component.translatable("gui.blackbox.handbook.page." + this.page + ".tip"), 27, 157, 234, 0xFF31443C);
		graphics.renderItem(guideIcon(this.page), 247, 64);
	}

	private ItemStack guideIcon(int guidePage) {
		return switch (guidePage) {
			case 0 -> stack(BlackboxModBlocks.DIMENSIONAL_WORKBENCH);
			case 1 -> stack(BlackboxModBlocks.INPUTBLOCK);
			case 2 -> stack(BlackboxModBlocks.OUTPUT_BLOCK);
			case 3 -> stack(BlackboxModItems.DIMENSION_CORE);
			case 4 -> stack(BlackboxModItems.STABILITY_UPGRADE);
			case 5 -> stack(Items.VILLAGER_SPAWN_EGG);
			case 6 -> stack(BlackboxModItems.BLUEPRINT);
			default -> stack(BlackboxModItems.ADMIN_BOOK);
		};
	}

	private void renderRecipePage(GuiGraphics graphics, int recipePage) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.handbook.page." + this.page + ".title"), 19, 64, 0xFF172229, false);
		RecipeView[] recipes = recipesForPage(recipePage);
		for (int index = 0; index < recipes.length; index++) {
			renderRecipe(graphics, index == 0 ? 15 : 151, recipes[index]);
		}
	}

	private void renderRecipe(GuiGraphics graphics, int x, RecipeView recipe) {
		graphics.drawCenteredString(this.font, recipe.title(), x + 55, 80, 0xFF26312A);
		for (int slot = 0; slot < 9; slot++) {
			int slotX = x + (slot % 3) * 18;
			int slotY = 95 + (slot / 3) * 18;
			graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF89958D);
			graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFFE3E7E1);
			ItemStack ingredient = recipe.ingredients()[slot];
			if (!ingredient.isEmpty()) {
				graphics.renderItem(ingredient, slotX + 1, slotY + 1);
			}
		}
		graphics.drawString(this.font, Component.literal("->"), x + 60, 114, 0xFF41614D, false);
		graphics.fill(x + 81, 112, x + 101, 132, 0xFF4F9A6C);
		graphics.fill(x + 83, 114, x + 99, 130, 0xFFF4F6F2);
		graphics.renderItem(recipe.result(), x + 83, 114);
	}

	private RecipeView[] recipesForPage(int recipePage) {
		return switch (recipePage) {
			case 0 -> new RecipeView[]{
					shaped("gui.blackbox.handbook.recipe.workbench", stack(BlackboxModBlocks.DIMENSIONAL_WORKBENCH), Items.COPPER_INGOT, Items.REDSTONE, Items.COPPER_INGOT, Items.IRON_INGOT, Items.CRAFTING_TABLE, Items.IRON_INGOT, Items.COPPER_INGOT, Items.COPPER_INGOT, Items.COPPER_INGOT),
					shaped("gui.blackbox.handbook.recipe.core", stack(BlackboxModItems.DIMENSION_CORE), Items.COPPER_INGOT, Items.REDSTONE, Items.COPPER_INGOT, Items.REDSTONE, Items.EMERALD, Items.REDSTONE, Items.COPPER_INGOT, Items.REDSTONE, Items.COPPER_INGOT)
			};
			case 1 -> new RecipeView[]{
					shaped("gui.blackbox.handbook.recipe.input", stack(BlackboxModBlocks.INPUTBLOCK), null, Items.COPPER_INGOT, null, Items.BLUE_DYE, Items.HOPPER, Items.BLUE_DYE, null, Items.COPPER_INGOT, null),
					shaped("gui.blackbox.handbook.recipe.output", stack(BlackboxModBlocks.OUTPUT_BLOCK), null, Items.COPPER_INGOT, null, Items.GREEN_DYE, Items.HOPPER, Items.GREEN_DYE, null, Items.COPPER_INGOT, null)
			};
			case 2 -> new RecipeView[]{
					shaped("gui.blackbox.handbook.recipe.blackbox", stack(BlackboxModBlocks.BLACKBOX_BLOCK), Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT, Items.OBSIDIAN, Items.CHEST, Items.OBSIDIAN, Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT),
					shaped("gui.blackbox.handbook.recipe.stability", stack(BlackboxModItems.STABILITY_UPGRADE), null, Items.IRON_INGOT, null, Items.CLOCK, Items.COMPARATOR, Items.CLOCK, null, Items.IRON_INGOT, null)
			};
			case 3 -> new RecipeView[]{
					shaped("gui.blackbox.handbook.recipe.medium", stack(BlackboxModItems.MEDIUM_CELL_UPGRADE), Items.COPPER_INGOT, null, Items.COPPER_INGOT, null, Items.REDSTONE, null, Items.COPPER_INGOT, null, Items.COPPER_INGOT),
					shapeless("gui.blackbox.handbook.recipe.large", stack(BlackboxModItems.LARGE_CELL_UPGRADE), BlackboxModItems.MEDIUM_CELL_UPGRADE, BlackboxModItems.MEDIUM_CELL_UPGRADE)
			};
			case 4 -> new RecipeView[]{
					shapeless("gui.blackbox.handbook.recipe.standard", stack(BlackboxModItems.STANDARD_ENVIRONMENT_UPGRADE), Items.COMPASS, Items.DEEPSLATE, Items.EMERALD),
					shapeless("gui.blackbox.handbook.recipe.overworld", stack(BlackboxModItems.OVERWORLD_ENVIRONMENT_UPGRADE), Items.COMPASS, Items.GRASS_BLOCK, Items.OAK_SAPLING)
			};
			case 5 -> new RecipeView[]{
					shapeless("gui.blackbox.handbook.recipe.nether", stack(BlackboxModItems.NETHER_ENVIRONMENT_UPGRADE), Items.COMPASS, Items.NETHERRACK, Items.NETHER_WART),
					shapeless("gui.blackbox.handbook.recipe.end", stack(BlackboxModItems.END_ENVIRONMENT_UPGRADE), Items.COMPASS, Items.END_STONE, Items.ENDER_PEARL)
			};
			case 6 -> new RecipeView[]{
					shapeless("gui.blackbox.handbook.recipe.mob_spawn", stack(BlackboxModItems.MOB_SPAWN_UPGRADE), Items.EMERALD, Items.ROTTEN_FLESH, Items.BONE, Items.GUNPOWDER, Items.SPIDER_EYE),
					shaped("gui.blackbox.handbook.recipe.blueprint", stack(BlackboxModItems.BLUEPRINT), Items.PAPER, Items.PAPER, Items.PAPER, Items.PAPER, Items.INK_SAC, Items.PAPER, Items.PAPER, Items.PAPER, Items.PAPER)
			};
			default -> new RecipeView[]{
					shapeless("gui.blackbox.handbook.recipe.handbook", stack(BlackboxModItems.HANDBOOK), Items.BOOK, Items.EMERALD),
					shaped("gui.blackbox.handbook.recipe.sword", stack(BlackboxModItems.OPFERSCHWERT), Items.EMERALD, Items.IRON_INGOT, Items.EMERALD, Items.EMERALD, Items.IRON_INGOT, Items.EMERALD, Items.EMERALD, Items.STICK, Items.EMERALD)
			};
		};
	}

	private RecipeView shaped(String titleKey, ItemStack result, ItemLike... ingredients) {
		ItemStack[] stacks = new ItemStack[9];
		for (int index = 0; index < stacks.length; index++) {
			stacks[index] = ingredients[index] == null ? ItemStack.EMPTY : new ItemStack(ingredients[index]);
		}
		return new RecipeView(Component.translatable(titleKey), stacks, result);
	}

	private RecipeView shapeless(String titleKey, ItemStack result, ItemLike... ingredients) {
		ItemStack[] stacks = new ItemStack[9];
		for (int index = 0; index < stacks.length; index++) {
			stacks[index] = index < ingredients.length ? new ItemStack(ingredients[index]) : ItemStack.EMPTY;
		}
		return new RecipeView(Component.translatable(titleKey), stacks, result);
	}

	private ItemStack stack(ItemLike item) {
		return new ItemStack(item);
	}

	private record RecipeView(Component title, ItemStack[] ingredients, ItemStack result) {
	}
}
