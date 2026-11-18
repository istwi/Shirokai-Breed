package dev.shirokai.daycare;

import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.StorePosition;
import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.shirokai.daycare.integration.CobblemonGateway;
import dev.shirokai.daycare.integration.Custody;
import java.util.List;
import java.util.UUID;

/** Read actual slots, not only the UUID cache. Never force a refused removal. */
public final class StorageSafety {
    private StorageSafety() {}

    public static Pokemon find(CobblemonGateway gateway, UUID owner, UUID id) {
        Pokemon result = null;
        int count = 0;
        for (PokemonStore<?> store : gateway.stores(owner)) {
            for (Pokemon pokemon : store) {
                if (pokemon.getUuid().equals(id)) { result = pokemon; count++; }
            }
        }
        if (count > 1) throw new IllegalStateException("UUID repetido em equipe/PC; transferência bloqueada para conferência: " + id);
        return result;
    }

    public static boolean capacity(CobblemonGateway gateway, UUID owner, int count) {
        if (count < 0) throw new IllegalArgumentException("Capacidade negativa");
        int empty = 0;
        for (PokemonStore<?> store : gateway.stores(owner)) {
            if (!(store instanceof PCStore pc)) continue;
            for (PCBox box : pc.getBoxes()) for (int slot = 0; slot < 30; slot++) {
                if (box.get(slot) == null) empty++;
            }
        }
        return empty >= count;
    }

    public static void remove(CobblemonGateway gateway, UUID owner, UUID id) {
        // Detect duplicates before removing anything. A stale cache entry alone is not a Pokémon.
        Pokemon existing = find(gateway, owner, id);
        if (existing == null) return;
        Custody.internal(() -> {
            for (PokemonStore<?> store : gateway.stores(owner)) {
                boolean contains = false;
                for (Pokemon p : store) if (p == existing) contains = true;
                if (!contains) continue;
                if (!store.remove(existing)) throw new IllegalStateException("Remoção recusada; UUID=" + id + "; store=" + store.getClass().getSimpleName());
            }
            return null;
        });
        if (find(gateway, owner, id) != null) throw new IllegalStateException("Remoção não confirmada: " + id);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean deliver(CobblemonGateway gateway, UUID owner, String snapshot) {
        Pokemon incoming = gateway.restore(snapshot);
        UUID id = incoming.getUuid();
        Pokemon existing = find(gateway, owner, id);
        // An existing copy is an idempotent retry, not permission to create another.
        if (existing != null) {
            if (!com.google.gson.JsonParser.parseString(gateway.snapshot(existing)).equals(com.google.gson.JsonParser.parseString(snapshot))) {
                throw new IllegalStateException("Mesmo UUID com dados diferentes; devolução bloqueada: " + id);
            }
            for (PokemonStore<?> store : gateway.stores(owner)) {
                for (Pokemon p : store) if (p == existing && !(store instanceof PCStore)) {
                    // Destination is always PC. Do not silently accept a copy still in the party.
                    if (!capacity(gateway, owner, 1)) return false;
                    remove(gateway, owner, id);
                }
            }
            if (find(gateway, owner, id) != null) return true;
            incoming = existing;
        }
        final Pokemon pokemon = incoming;
        return Custody.internal(() -> {
            for (PokemonStore store : gateway.stores(owner)) {
                if (!(store instanceof PCStore)) continue;
                StorePosition position = store.getFirstAvailablePosition();
                if (position == null) continue;
                store.set(position, pokemon);
                if (find(gateway, owner, id) != pokemon || store.get(position) != pokemon) {
                    throw new IllegalStateException("Inserção no PC não confirmada: " + id);
                }
                return true;
            }
            return false;
        });
    }

    public static int requiredPcSpaces(CobblemonGateway gateway, UUID owner, List<UUID> ids) {
        int count = 0;
        for (UUID id : ids) {
            Pokemon p = find(gateway, owner, id);
            boolean inPc = false;
            if (p != null) for (PokemonStore<?> store : gateway.stores(owner)) {
                if (!(store instanceof PCStore)) continue;
                for (Pokemon stored : store) if (stored == p) inPc = true;
            }
            if (!inPc) count++;
        }
        return count;
    }
}
