package xyz.destiall.sgcraftlink.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.destiall.sgcraftlink.SGCraftLinkFabric;
import xyz.destiall.sgcraftlink.modules.Module;

@Mixin(MinecraftDedicatedServer.class)
public class SGCraftLinkMixin {

    @Inject(at = @At("HEAD"), method = "shutdown")
    private void shutdown(CallbackInfo ci) {
        for (Module module : SGCraftLinkFabric.getInst().getModules()) {
            module.disable();
        }

        SGCraftLinkFabric.getInst().getModules().clear();
    }

    @Inject(at = @At("HEAD"), method = "setupServer")
    private void setupServer(CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = (MinecraftServer)(Object) this;
        SGCraftLinkFabric.setServer(server);
    }
}
