package dev.shirokai.daycare.mixin;

import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.shirokai.daycare.ReservationSafety;
import dev.shirokai.daycare.integration.Custody;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerPartyStore.class, remap = false)
public abstract class PlayerPartyReservationMixin {
    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("HEAD"), cancellable = true)
    private void zorykai$set(PartyPosition position, Pokemon incoming, CallbackInfo ci) {
        if (ReservationSafety.set((PlayerPartyStore)(Object)this, position, incoming)) ci.cancel();
    }
    @Inject(method = "swap(Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;)V", at = @At("HEAD"), cancellable = true)
    private void zorykai$swap(PartyPosition a, PartyPosition b, CallbackInfo ci) {
        if (ReservationSafety.swap((PlayerPartyStore)(Object)this, a, b)) ci.cancel();
    }
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void zorykai$add(Pokemon pokemon, CallbackInfoReturnable<Boolean> ci) {
        if (Custody.blocked(pokemon.getUuid())) ci.setReturnValue(false);
    }
}
