package com.aaa.combatperspective.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantment;

public class CPSword extends SwordItem{

    public CPSword(Tier tier, Properties properties) {
        super(Tiers.IRON, properties);
    }
}