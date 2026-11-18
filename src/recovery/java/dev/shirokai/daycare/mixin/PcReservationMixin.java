package dev.shirokai.daycare.mixin;

import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.shirokai.daycare.ReservationSafety;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PCStore.class, remap = false)
public abstract class PcReservationMixin {
    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/pc/PCPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("HEAD"), cancellable = true)
    private void zorykai$set(PCPosition position, Pokemon incoming, CallbackInfo ci) {
        if (ReservationSafety.set((PCStore)(Object)this, position, incoming)) ci.cancel();
    }
    @Inject(method = "swap(Lcom/cobblemon/mod/common/api/storage/pc/PCPosition;Lcom/cobblemon/mod/common/api/storage/pc/PCPosition;)V", at = @At("HEAD"), cancellable = true)
    private void zorykai$swap(PCPosition a, PCPosition b, CallbackInfo ci) {
        if (ReservationSafety.swap((PCStore)(Object)this, a, b)) ci.cancel();
    }
}
