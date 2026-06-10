package net.mcreator.simpleenhancement.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class NetheriteDiamondAlloyItem extends Item {
	public NetheriteDiamondAlloyItem() {
		super(new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));
	}
}