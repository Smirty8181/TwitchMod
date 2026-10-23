package net.twitchcraft.hud;

import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Сайдбар листает категории по очереди: сначала все страницы Добро,
 * потом все страницы Урон, потом Пранк, потом свои.
 * Категория не сменяется, пока не показаны все её команды.
 */
public final class SidebarPages {
    private static final int CMDS_PER_ROW = 3;
    /** Строк команд на одну страницу (плюс заголовок). Влезает в HUD. */
    private static final int ROWS_PER_PAGE = 8;
    private static final int CMDS_PER_PAGE = CMDS_PER_ROW * ROWS_PER_PAGE;
    private static List<Page> cachedPages;
    private static int cachedRevision = Integer.MIN_VALUE;

    private SidebarPages() {}

    public static void invalidate() {
        cachedPages = null;
        cachedRevision = Integer.MIN_VALUE;
    }

    public static int pageCount(TwitchCraftConfig config) {
        return Math.max(1, allPages(config).size());
    }

    public static List<String> build(TwitchCraftConfig config, int elapsedSeconds) {
        return buildSidebar(config, elapsedSeconds);
    }

    public static List<String> buildSidebar(TwitchCraftConfig config, int elapsedSeconds) {
        List<String> lines = new ArrayList<>();
        if (!config.scoreboard.showCommands) return lines;

        List<Page> pages = allPages(config);
        if (pages.isEmpty()) {
            lines.add("§8нет вкл. команд");
            return lines;
        }
        int rotateSec = Math.max(3, config.scoreboard.pageRotateSeconds);
        int pageIndex = (elapsedSeconds / rotateSec) % pages.size();
        return render(pages.get(pageIndex));
    }

    public static List<String> featuredLine(TwitchCraftConfig config) {
        List<String> lines = new ArrayList<>();
        if (!config.scoreboard.showFeaturedCommand) return lines;

        String id = config.scoreboard.featuredCommandId;
        if (id == null || id.isBlank()) id = "lightning";

        ChatGameCommand cmd = ChatGameCommand.fromId(id).orElse(ChatGameCommand.LIGHTNING);
        var settings = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
        if (!settings.enabled) return lines;

        String star = GoodCommands.isGood(cmd) ? "§a§l★" : (EvilCommands.isEvil(cmd) ? "§c§l★" : "§e§l★");
        lines.add(star + " !" + cmd.getRussianName());
        return lines;
    }

    private static List<Page> allPages(TwitchCraftConfig config) {
        int rev = ConfigManager.revision();
        if (cachedPages != null && cachedRevision == rev) return cachedPages;
        List<Page> pages = new ArrayList<>();
        addCommandPages(pages, "§a§l+ Добро", collectGood(config));
        addCommandPages(pages, "§c§l⚔ Урон", collectHarm(config, true));
        addCommandPages(pages, "§e§l☺ Пранк", collectHarm(config, false));
        addCustomPages(pages, config);
        cachedPages = pages;
        cachedRevision = rev;
        return pages;
    }

    private static List<ChatGameCommand> collectGood(TwitchCraftConfig config) {
        List<ChatGameCommand> cmds = new ArrayList<>();
        for (ChatGameCommand cmd : GoodCommands.enabled(config)) {
            cmds.add(cmd);
        }
        return cmds;
    }

    private static List<ChatGameCommand> collectHarm(TwitchCraftConfig config, boolean damage) {
        List<ChatGameCommand> cmds = new ArrayList<>();
        List<ChatGameCommand> src = damage ? EvilCommands.damageAll() : EvilCommands.prankAll();
        for (ChatGameCommand cmd : src) {
            var settings = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
            if (settings.enabled) cmds.add(cmd);
        }
        return cmds;
    }

    private static void addCommandPages(List<Page> pages, String title, List<ChatGameCommand> cmds) {
        if (cmds.isEmpty()) return;
        int localTotal = (cmds.size() + CMDS_PER_PAGE - 1) / CMDS_PER_PAGE;
        for (int i = 0; i < localTotal; i++) {
            int from = i * CMDS_PER_PAGE;
            int to = Math.min(from + CMDS_PER_PAGE, cmds.size());
            pages.add(new Page(title, packCommandRows(cmds.subList(from, to)), i + 1, localTotal, cmds.size()));
        }
    }

    private static void addCustomPages(List<Page> pages, TwitchCraftConfig config) {
        List<String> names = new ArrayList<>();
        if (config.customCommands != null) {
            for (CustomCommandDefinition def : config.customCommands) {
                if (def == null || !def.enabled) continue;
                String color = def.positive ? "§a" : "§c";
                names.add(color + "!" + def.displayName());
            }
        }
        if (names.isEmpty()) return;
        int localTotal = (names.size() + CMDS_PER_PAGE - 1) / CMDS_PER_PAGE;
        for (int i = 0; i < localTotal; i++) {
            int from = i * CMDS_PER_PAGE;
            int to = Math.min(from + CMDS_PER_PAGE, names.size());
            pages.add(new Page("§b§l✦ Свои", packStrings(names.subList(from, to)), i + 1, localTotal, names.size()));
        }
    }

    private static List<String> render(Page page) {
        List<String> lines = new ArrayList<>();
        lines.add(page.title + " §8(" + page.localIndex + "/" + page.localTotal + " · " + page.totalCmds + ")");
        lines.addAll(page.rows);
        return lines;
    }

    private static List<String> packCommandRows(List<ChatGameCommand> cmds) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        int inRow = 0;
        for (ChatGameCommand cmd : cmds) {
            String color = GoodCommands.isGood(cmd) ? "§a" : (EvilCommands.isEvil(cmd) ? "§c" : "§e");
            String piece = color + "!" + cmd.getRussianName();
            if (inRow > 0) row.append(" §8· ");
            row.append(piece);
            inRow++;
            if (inRow >= CMDS_PER_ROW) {
                rows.add(row.toString());
                row.setLength(0);
                inRow = 0;
            }
        }
        if (inRow > 0) rows.add(row.toString());
        return rows;
    }

    private static List<String> packStrings(List<String> names) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        int inRow = 0;
        for (String name : names) {
            if (inRow > 0) row.append(" §8· ");
            row.append(name);
            inRow++;
            if (inRow >= CMDS_PER_ROW) {
                rows.add(row.toString());
                row.setLength(0);
                inRow = 0;
            }
        }
        if (inRow > 0) rows.add(row.toString());
        return rows;
    }

    private record Page(String title, List<String> rows, int localIndex, int localTotal, int totalCmds) {}
}
