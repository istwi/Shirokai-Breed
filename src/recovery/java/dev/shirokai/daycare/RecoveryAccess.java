package dev.shirokai.daycare;

import dev.shirokai.daycare.domain.TrainerState;
import java.util.Set;
import java.util.UUID;

/** Explicit bridges to the existing service's journal; installed by PatchRecovery. */
public interface RecoveryAccess {
    Set<UUID> recoveryBusy();
    Set<UUID> recoveryFailed();
    Set<UUID> recoveryFixtures();
    Set<UUID> recoveryFailAfterDurable();
    void recoveryPersist(TrainerState state, String event, UUID operation, Runnable next);
}
