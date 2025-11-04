package com.logic.superbrecruits.mixin;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VehicleEntity.class)
public interface VehicleEntityAccessor {

    @Accessor
    public NonNullList<ItemStack> getItems();
}
