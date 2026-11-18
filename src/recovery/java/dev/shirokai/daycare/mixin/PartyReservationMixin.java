package dev.shirokai.daycare.mixin;

import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.shirokai.daycare.ReservationSafety;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PartyStore.class, remap = false)
public abstract class PartyReservationMixin {
    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("HEAD"), cancellable = true)
    private void zorykai$set(PartyPosition position, Pokemon incoming, CallbackInfo ci) {
        if (ReservationSafety.set((PartyStore)(Object)this, position, incoming)) ci.cancel();
    }
    @Inject(method = "swap(Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;)V", at = @At("HEAD"), cancellable = true)
    private void zorykai$swap(PartyPosition a, PartyPosition b, CallbackInfo ci) {
        if (ReservationSafety.swap((PartyStore)(Object)this, a, b)) ci.cancel();
    }
}
