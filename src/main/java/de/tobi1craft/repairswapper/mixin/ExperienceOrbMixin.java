package de.tobi1craft.repairswapper.mixin;

import de.tobi1craft.repairswapper.RepairSwapperClient;
import de.tobi1craft.repairswapper.RepairSwapperConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbMixin {

    @Inject(at = @At("HEAD"), method = "onPlayerCollision")
    private void swapRepairable(PlayerEntity playerEntity, CallbackInfo ci) {
        if (!(playerEntity instanceof ClientPlayerEntity)) {
            return;
        }
        if (RepairSwapperConfig.auto) RepairSwapperClient.enable(MinecraftClient.getInstance());
    }
}