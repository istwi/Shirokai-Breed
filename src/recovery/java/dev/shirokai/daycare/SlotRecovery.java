package dev.shirokai.daycare;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.shirokai.daycare.domain.TrainerState;
import dev.shirokai.daycare.integration.Custody;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.class_3222;

/** Administrative return transaction. Never discards an existing journal or pending egg. */
public final class SlotRecovery {
    public static final String KIND = "RESET_SLOT";
    private SlotRecovery() {}

    private static RecoveryAccess access(DaycareService service) { return (RecoveryAccess)(Object)service; }

    public static void register(LiteralArgumentBuilder<class_2168> admin, RecoveryHost host) {
        admin.then(class_2170.method_9247("resetslot")
            .then(class_2170.method_9244("player", StringArgumentType.word())
            .then(class_2170.method_9244("slot", IntegerArgumentType.integer(1, 6))
            .executes(ctx -> {
                class_2168 source = ctx.getSource();
                try {
                    DaycareService service = host.recoveryService();
                    if (service == null) throw new IllegalStateException("Daycare inicializando");
                    String name = StringArgumentType.getString(ctx, "player");
                    class_3222 player = source.method_9211().method_3760().method_14566(name);
                    if (player == null) throw new IllegalArgumentException("Jogador deve estar online; use o nick exato");
                    request(service, player.method_5667(), IntegerArgumentType.getInteger(ctx, "slot") - 1);
                    source.method_9226(() -> class_2561.method_43470("✦ Reset solicitado. Pais serão devolvidos ao PC; ovo pendente será preservado na chocadeira. Confira /daycareadmin info e o resultado no chat."), true);
                    ShirokaiDaycare.LOG.info("Admin {}: RESET_SLOT solicitado owner={} slot={}", source.method_9214(), player.method_5667(), IntegerArgumentType.getInteger(ctx, "slot"));
                    return 1;
                } catch (Exception e) {
                    ShirokaiDaycare.LOG.warn("RESET_SLOT recusado admin={} motivo={}", source.method_9214(), e.getMessage());
                    source.method_9213(class_2561.method_43470("✦ " + e.getMessage()));
                    return 0;
                }
            }))));
    }

    public static int firstFreeSlot(TrainerState state) {
        for (int slot = 0; slot < state.incubatorSlots; slot++) {
            boolean used = false;
            for (TrainerState.Egg egg : state.eggs) if (egg.slot == slot) used = true;
            if (!used) return slot;
        }
        throw new IllegalStateException("Chocadeira cheia; ovo preservado no casal");
    }

    public static void withdrawPair(DaycareService service, class_3222 player, int slot) {
        if (!service.available(player)) return;
        TrainerState state = service.state(player.method_5667());
        TrainerState.Couple couple = state.couple(slot);
        if (couple == null) return;
        if (state.operation != null) { tell(service, state.owner, "Operação pendente; aguarde ou consulte o administrador."); return; }
        if (couple.pendingEgg != null) { tell(service, state.owner, "Envie o ovo do casal para a chocadeira antes de retirar os pais."); return; }
        TrainerState.Operation op = new TrainerState.Operation();op.kind = "WITHDRAW";op.slot = slot;
        addParent(service, op, couple.first, couple.firstId);
        addParent(service, op, couple.second, couple.secondId);
        if (op.pokemon.isEmpty()) throw new IllegalStateException("Casal sem snapshots; retirada bloqueada");
        if (!StorageSafety.capacity(service.pokemon, state.owner, StorageSafety.requiredPcSpaces(service.pokemon, state.owner, op.pokemonIds))) {
            tell(service, state.owner, "PC sem espaço. Libere vagas para retirar os pais."); return;
        }
        state.operation = op;
        access(service).recoveryPersist(state, "WITHDRAW_PREPARED", op.id, () -> service.recover(state.owner));
    }

    private static void addParent(DaycareService service, TrainerState.Operation op, String snapshot, UUID id) {
        if (snapshot == null && id == null) return;
        if (snapshot == null || id == null || !service.pokemon.id(snapshot).equals(id) || op.pokemonIds.contains(id)) {
            throw new IllegalStateException("Dados de pai inconsistentes; reset bloqueado");
        }
        op.pokemon.add(snapshot); op.pokemonIds.add(id);
    }

    public static void request(DaycareService service, UUID owner, int slot) {
        RecoveryLog.write("RESET_SLOT_REQUEST", owner, null, slot, null);
        RecoveryAccess a = access(service);
        if (!service.ready || a.recoveryBusy().contains(owner)) throw new IllegalStateException("Aguarde a operação em andamento");
        if (a.recoveryFailed().contains(owner)) throw new IllegalStateException("Dados bloqueados; use /daycareadmin recover antes do reset");
        if (service.server.method_3760().method_14602(owner) == null && !a.recoveryFixtures().contains(owner)) throw new IllegalStateException("Jogador deve estar online");
        TrainerState state = service.state(owner);
        if (state.operation != null) throw new IllegalStateException("Journal pendente; não será apagado. Use /daycareadmin recover");
        if (slot < 0 || slot >= state.breedSlots) throw new IllegalArgumentException("Vaga não comprada ou inválida (use 1 a " + state.breedSlots + ")");
        TrainerState.Couple couple = state.couple(slot);
        if (couple == null) throw new IllegalArgumentException("Essa vaga já está vazia");
        TrainerState.Operation op = new TrainerState.Operation();
        op.kind = KIND; op.slot = slot;
        addParent(service, op, couple.first, couple.firstId);
        addParent(service, op, couple.second, couple.secondId);
        if (op.pokemon.isEmpty()) throw new IllegalStateException("Casal sem snapshots; reset bloqueado");
        op.egg = couple.pendingEgg == null ? null : couple.pendingEgg.id;
        validateEgg(service, state, couple, op);
        service.pokemon.checkBackend(owner);
        if (!StorageSafety.capacity(service.pokemon, owner, StorageSafety.requiredPcSpaces(service.pokemon, owner, op.pokemonIds))) {
            throw new IllegalStateException("PC sem espaço; libere vagas antes do reset");
        }
        state.operation = op;
        state.schema = 4;
        a.recoveryPersist(state, "RESET_SLOT_PREPARED slot=" + (slot + 1), op.id, () -> service.recover(owner));
    }

    private static void validateEgg(DaycareService service, TrainerState state, TrainerState.Couple couple, TrainerState.Operation op) {
        UUID pending = couple.pendingEgg == null ? null : couple.pendingEgg.id;
        if (!Objects.equals(pending, op.egg)) throw new IllegalStateException("Ovo mudou durante o reset; journal preservado");
        if (pending == null) return;
        if (couple.pendingEgg.pokemon == null || !service.pokemon.id(couple.pendingEgg.pokemon).equals(pending) || op.pokemonIds.contains(pending) || couple.pendingEgg.remainingMs < 0) {
            throw new IllegalStateException("Ovo inconsistente; reset bloqueado");
        }
        if (state.eggs.stream().anyMatch(e -> e.id.equals(pending))) throw new IllegalStateException("UUID de ovo repetido; reset bloqueado");
        firstFreeSlot(state);
    }

    /** Return true when this journal kind was handled, including a safe wait. */
    public static boolean recover(DaycareService service, UUID owner) {
        TrainerState state = service.state(owner);
        TrainerState.Operation op = state.operation;
        if (op == null) return false;
        if (!KIND.equals(op.kind)) {
            if (op.kind == null || !java.util.Set.of("PURCHASE", "DEPOSIT", "DEPOSIT_PARENT", "WITHDRAW", "WITHDRAW_PARENT", "COLLECT").contains(op.kind)) {
                fail(service, owner, new IllegalStateException("Tipo de journal desconhecido; não será apagado: " + op.kind));
                return true;
            }
            return false;
        }
        RecoveryAccess a = access(service);
        if (!service.ready || a.recoveryBusy().contains(owner) || a.recoveryFailed().contains(owner)) return true;
        if (service.server.method_3760().method_14602(owner) == null && !a.recoveryFixtures().contains(owner)) return true;
        try {
            TrainerState.Couple couple = state.couple(op.slot);
            if (couple == null) throw new IllegalStateException("Casal ausente; journal preservado");
            TrainerState.Operation expected = new TrainerState.Operation();
            addParent(service, expected, couple.first, couple.firstId);
            addParent(service, expected, couple.second, couple.secondId);
            if (!expected.pokemonIds.equals(op.pokemonIds) || !expected.pokemon.equals(op.pokemon)) throw new IllegalStateException("Casal divergente do journal; reset bloqueado");
            validateEgg(service, state, couple, op);
            service.pokemon.checkBackend(owner);
            if (!StorageSafety.capacity(service.pokemon, owner, StorageSafety.requiredPcSpaces(service.pokemon, owner, op.pokemonIds))) {
                tell(service, owner, "PC cheio; reset pendente preservado. Libere espaço e use /daycareadmin recover.");
                return true;
            }
            a.recoveryBusy().add(owner);
            for (String snapshot : op.pokemon) if (!StorageSafety.deliver(service.pokemon, owner, snapshot)) throw new IllegalStateException("PC ficou cheio; journal preservado");
            service.pokemon.durable(owner).whenComplete((v, error) -> service.server.execute(() -> {
                a.recoveryBusy().remove(owner);
                if (error != null) { fail(service, owner, error); return; }
                if (a.recoveryFailAfterDurable().remove(owner)) { a.recoveryFailed().add(owner); return; }
                try {
                    if (state.operation != op) throw new IllegalStateException("Journal substituído");
                    validateEgg(service, state, couple, op);
                    for (int i = 0; i < op.pokemonIds.size(); i++) {
                        var p = StorageSafety.find(service.pokemon, owner, op.pokemonIds.get(i));
                        if (p == null || StorageSafety.requiredPcSpaces(service.pokemon, owner, java.util.List.of(op.pokemonIds.get(i))) != 0) throw new IllegalStateException("Devolução ao PC não confirmada");
                    }
                    if (couple.pendingEgg != null) {
                        couple.pendingEgg.slot = firstFreeSlot(state);
                        state.eggs.add(couple.pendingEgg);
                        couple.pendingEgg = null;
                    }
                    state.couples.remove(couple);
                    state.replacementCooldowns.remove(op.slot);
                    state.operation = null;
                    a.recoveryPersist(state, "RESET_SLOT_CONFIRMED slot=" + (op.slot + 1) + " pokemon=" + op.pokemonIds, op.id, () -> {
                        for (UUID id : op.pokemonIds) Custody.release(id, owner);
                        tell(service, owner, "Vaga " + (op.slot + 1) + " liberada. Pais devolvidos ao PC; ovo preservado na chocadeira. Vagas compradas mantidas.");
                    });
                } catch (Exception e) { fail(service, owner, e); }
            }));
        } catch (Exception e) { a.recoveryBusy().remove(owner); fail(service, owner, e); }
        return true;
    }

    private static void tell(DaycareService service, UUID owner, String text) {
        class_3222 player = service.server.method_3760().method_14602(owner);
        if (player != null) player.method_7353(class_2561.method_43470("✦ " + text), false);
    }

    private static void fail(DaycareService service, UUID owner, Throwable error) {
        access(service).recoveryFailed().add(owner);
        ShirokaiDaycare.LOG.error("RESET_SLOT bloqueado; journal preservado owner=" + owner, error);
        tell(service, owner, "Reset bloqueado; journal preservado para recuperação. Consulte o administrador.");
    }
}
