package net.twitchcraft.commands;

import java.util.Map;

/**
 * Одна опечатка в !команде: буква не та, лишняя, пропущенная или две соседние перепутаны.
 * Короткое слово (3 буквы) — только лишняя/пропущенная, чтобы «лук» не становился «мук».
 */
final class CommandTypo {
    private CommandTypo() {}

    static ChatGameCommand resolve(String typed, Map<String, ChatGameCommand> aliases) {
        if (typed == null || typed.length() < 3 || typed.length() > 24) return null;
        ChatGameCommand hit = null;
        for (Map.Entry<String, ChatGameCommand> e : aliases.entrySet()) {
            String alias = e.getKey();
            if (alias == null || !isTypoOf(typed, alias)) continue;
            ChatGameCommand cmd = e.getValue();
            if (hit == null) {
                hit = cmd;
            } else if (hit != cmd) {
                return null;
            }
        }
        return hit;
    }

    static boolean isTypoOf(String typed, String alias) {
        int n = typed.length();
        int m = alias.length();
        if (Math.abs(n - m) > 1) return false;
        int min = Math.min(n, m);
        if (min < 3) return false;
        if (n == m) {
            if (min <= 3) return false;
            return oneSubstitution(typed, alias) || oneTransposition(typed, alias);
        }
        return n > m ? oneInsertion(alias, typed) : oneInsertion(typed, alias);
    }

    private static boolean oneSubstitution(String a, String b) {
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i) && ++diff > 1) return false;
        }
        return diff == 1;
    }

    private static boolean oneTransposition(String a, String b) {
        int i = 0;
        while (i < a.length() && a.charAt(i) == b.charAt(i)) i++;
        if (i >= a.length() - 1) return false;
        if (a.charAt(i) != b.charAt(i + 1) || a.charAt(i + 1) != b.charAt(i)) return false;
        for (int j = i + 2; j < a.length(); j++) {
            if (a.charAt(j) != b.charAt(j)) return false;
        }
        return true;
    }

    /** {@code longer} = {@code shorter} плюс один символ. */
    private static boolean oneInsertion(String shorter, String longer) {
        int i = 0;
        int j = 0;
        boolean skipped = false;
        while (i < shorter.length() && j < longer.length()) {
            if (shorter.charAt(i) == longer.charAt(j)) {
                i++;
                j++;
                continue;
            }
            if (skipped) return false;
            skipped = true;
            j++;
        }
        return i == shorter.length() && (skipped || j == longer.length() - 1);
    }
}
