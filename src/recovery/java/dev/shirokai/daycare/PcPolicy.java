package dev.shirokai.daycare;

import dev.shirokai.daycare.config.Settings;

/** Keep the shipped capacity wording consistent without overwriting custom messages. */
public final class PcPolicy {
    private PcPolicy() {}
    public static void configure(Settings settings) {
        if (settings.messages != null && settings.messages.text != null) {
            settings.messages.text.replace("full", "Equipe e PC sem espaço. O Pokémon permanece guardado.", "PC sem espaço. O Pokémon permanece guardado.");
        }
    }
}
