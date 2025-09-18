package xyz.destiall.sgcraftlink.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.destiall.sgcraftlink.SGCraftLinkFabric;

@Mixin(MinecraftDedicatedServer.class)
public class SGCraftLinkMixin {

    @Inject(at = @At("HEAD"), method = "shutdown")
    private void sgclink_shutdown(CallbackInfo ci) {
        SGCraftLinkFabric.SHUTTING_DOWN = true;
        SGCraftLinkFabric.getInst().disable();
    }

    @Inject(at = @At("HEAD"), method = "setupServer")
    private void sgclink_setupserver(CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = (MinecraftServer)(Object) this;
        SGCraftLinkFabric.setServer(server);
        SGCraftLinkFabric.getInst().enable();
    }
}
