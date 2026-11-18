package dev.shirokai.daycare;

import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.StorePosition;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.shirokai.daycare.integration.Custody;

/** Block before subclass notification packets, not only inside the base store. */
public final class ReservationSafety {
    private ReservationSafety() {}
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean set(PokemonStore store, StorePosition position, Pokemon incoming) {
        Pokemon previous = (Pokemon)store.get(position);
        boolean blocked = incoming != null && Custody.blocked(incoming.getUuid()) || previous != null && Custody.blocked(previous.getUuid());
        if (blocked) RecoveryLog.write("BLOCKED_STORE_SET", store.getUuid(), null, null, store.getClass().getSimpleName() + " incoming=" + (incoming == null ? null : incoming.getUuid()) + " previous=" + (previous == null ? null : previous.getUuid()));
        return blocked;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean swap(PokemonStore store, StorePosition a, StorePosition b) {
        Pokemon first = (Pokemon)store.get(a), second = (Pokemon)store.get(b);
        boolean blocked = first != null && Custody.blocked(first.getUuid()) || second != null && Custody.blocked(second.getUuid());
        if (blocked) RecoveryLog.write("BLOCKED_STORE_SWAP", store.getUuid(), null, null, store.getClass().getSimpleName() + " first=" + (first == null ? null : first.getUuid()) + " second=" + (second == null ? null : second.getUuid()));
        return blocked;
    }
}
