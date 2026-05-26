package com.logic.superbrecruits.mixin;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageMountEntity;
import de.maxhenkel.recruits.corelib.net.Message;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mixin(MessageMountEntity.class)
public abstract class MixinMessageMountEntity implements Message<MessageMountEntity> {

    @Shadow(remap = false)
    private UUID target;

    @Shadow(remap = false)
    private UUID uuid;

    @Shadow(remap = false)
    private UUID group;

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = (ServerPlayer)Objects.requireNonNull(context.getSender());
        List<Entity> entityList = player.getCommandSenderWorld().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate((double)100.0F), (mount) -> mount.getUUID().equals(this.target) && ((List)RecruitsServerConfig.MountWhiteList.get()).contains(mount.getEncodeId()) || mount.getUUID().equals(this.target) && mount instanceof VehicleEntity);
        if (!entityList.isEmpty()) {
            player.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate((double)100.0F), (recruit) -> recruit.isEffectedByCommand(this.uuid, this.group)).forEach((recruit) -> CommandEvents.onMountButton(this.uuid, recruit, this.target, this.group));
        }
    }
}
