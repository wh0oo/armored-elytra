package dorkix.armored.elytra;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.fabricmc.api.ModInitializer;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ArmoredElytra implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("ArmoredElytra");
	public static final String MOD_ID = "armored_elytra";

	public static final Identifier ELYTRA_DATA = id("elytra");
	public static final Identifier CHESTPLATE_DATA = id("chestplate");
	public static final Identifier TRIM_MATERIAL_DATA = id("trim_material");

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
	}

	public static ItemStack createArmoredElytra(ItemStack elytra, ItemStack armor,
			ScreenHandlerContext context, String newItemName) {
		// return on invalid items
		if (!(armor.isIn(ItemTags.CHEST_ARMOR) && elytra.isOf(Items.ELYTRA)))
			return armor;

		var newElytra = elytra.copy();

		NbtCompound customData = elytra.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();

		context.run((world, blockPos) -> {
			customData.put(ArmoredElytra.ELYTRA_DATA.toString(),
					ItemStack.save(world, elytra));
			customData.put(ArmoredElytra.CHESTPLATE_DATA.toString(),
					ItemStack.save(world, armor));
		});

		// Copy Attribute modifiers
		var armor_attr = armor.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
		var builder = AttributeModifiersComponent.builder();
		for (var aa : armor_attr.modifiers()) {
			builder.add(aa.attribute(), aa.modifier(), aa.slot());
		}
		var attr = builder.build();
		newElytra.applyComponentsFrom(
				ComponentMap.builder().add(DataComponentTypes.ATTRIBUTE_MODIFIERS,
						attr).build());

		// Copy Armor Trims
		var trims = armor.getComponentChanges().get(DataComponentTypes.TRIM);
		if (trims != null && trims.isPresent()) {
			customData.putString(ArmoredElytra.TRIM_MATERIAL_DATA.toString(), trims.get().material().getIdAsString());
		}

		var armorType = armor.getItem().toString();
		if (armorType.equals(Items.LEATHER_CHESTPLATE.toString())) {
			var color = armor.get(DataComponentTypes.DYED_COLOR);
			if (color != null) {
				newElytra.applyChanges(
						ComponentChanges.builder().add(DataComponentTypes.DYED_COLOR,
								color).build());
			}
		}

		// Copy Enchantments
		for (var ench : armor.getEnchantments().getEnchantments()) {
			int level = 1;
			var key = ench.getKey();
			if (key.isPresent()) {
				level = armor.getEnchantments().getLevel(ench);
			}
			newElytra.addEnchantment(ench, level);
		}

		// Set Armored elytra name or custom name from anvil
		Text name = Text.of(newItemName);
		boolean hasNewName = newItemName != null && !newItemName.isEmpty();
		if (!hasNewName) {
			name = Text.translatableWithFallback("item." + ArmoredElytra.MOD_ID + ".item_name", "Armored Elytra");
		}
		newElytra.applyComponentsFrom(
				ComponentMap.builder().add(DataComponentTypes.CUSTOM_NAME,
						name.copy().setStyle(
								Style.EMPTY.withItalic(hasNewName).withColor(Formatting.LIGHT_PURPLE)))
						.build());

		// Set description
		var armorHasCustomName = armor.get(DataComponentTypes.CUSTOM_NAME) != null;

		List<Text> loreTexts = Lists.newArrayList();

		if (trims != null && trims.isPresent()) {
			List<Text> trimTexts = Lists.newArrayList();
			trims.get().appendTooltip(Item.TooltipContext.DEFAULT, trimTexts::add, TooltipType.ADVANCED,
					armor.getComponents());

			var upgradeText = trimTexts.get(0).copy()
					.setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY));
			var trimText = trimTexts.get(1).copy()
					.setStyle(Style.EMPTY.withItalic(false));
			var materialText = trimTexts.get(2).copy()
					.setStyle(Style.EMPTY.withItalic(false));

			loreTexts.addAll(List.of(
					ScreenTexts.EMPTY,
					upgradeText,
					trimText,
					materialText));
		}

		loreTexts.addAll(List.of(
				ScreenTexts.EMPTY,
				Text.translatableWithFallback(
						"item." + ArmoredElytra.MOD_ID + ".item_lore_text", "With chestplate:")
						.copy()
						.setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)),
				ScreenTexts.space().append(armor.getName())
						.setStyle(Style.EMPTY.withItalic(armorHasCustomName)
								.withColor(Formatting.LIGHT_PURPLE))));

		var loreComponent = new LoreComponent(loreTexts);

		newElytra.applyComponentsFrom(
				ComponentMap.builder()
						.add(DataComponentTypes.LORE,
								loreComponent)
						.build());

		// Set Custom data
		newElytra.applyComponentsFrom(
				ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData)).build());

		newElytra.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(Collections.emptyList(),
				Collections.emptyList(), List.of(armorType), Collections.emptyList()));

		return newElytra;
	}

	public static boolean isArmoredElytra(ItemStack elytra) {
		if (!elytra.isOf(Items.ELYTRA)) {
			return false;
		}

		NbtCompound customData = elytra
				.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
				.copyNbt();

		Optional<NbtCompound> elytraDataNbt = customData.getCompound(ArmoredElytra.ELYTRA_DATA.toString());
		Optional<NbtCompound> armorDataNbt = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());

		if (elytraDataNbt.isEmpty() || armorDataNbt.isEmpty()) {
			return false;
		}

		NbtCompound elytraData = elytraDataNbt.get();
		NbtCompound armorData = armorDataNbt.get();

		if (elytraData.isEmpty() || armorData.isEmpty()) {
			return false;
		}

		return true;
	}
}
