package com.logic.superbrecruits.mixin;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.RecruitMountEntityGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(RecruitMountEntityGoal.class)
public abstract class MixinRecruitMountEntity extends Goal {
    @Shadow(remap = false)
    private AbstractRecruitEntity recruit;

    @Shadow(remap = false)
    private Entity mount;

    /**
     * @author
     * @reason
     */

    @Overwrite(remap = false)
    private void findMount() {
        this.recruit.getCommandSenderWorld().getEntitiesOfClass(Entity.class, this.recruit.getBoundingBox().inflate((double)32.0F), (mount) -> this.recruit.getMountUUID() != null && mount.getUUID().equals(this.recruit.getMountUUID()) && (((List) RecruitsServerConfig.MountWhiteList.get()).contains(mount.getEncodeId()) || mount instanceof AbstractHorse || mount instanceof VehicleEntity)).forEach((mount) -> this.mount = mount);
    }
}
