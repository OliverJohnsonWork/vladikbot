package l1.multiheaded.vladikbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import l1.multiheaded.vladikbot.Bot;
import l1.multiheaded.vladikbot.commands.music.MusicCommand;
import l1.multiheaded.vladikbot.settings.GuildSettings;
import l1.multiheaded.vladikbot.settings.GuildSettingsManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

import java.util.Objects;

/**
 * @author Oliver Johnson
 * Changes from original source:
 * - Reformating code
 * @author John Grosh
 */
public abstract class DJCommand extends MusicCommand {
    DJCommand(Bot bot) {
        super(bot);
        this.category = new Category("DJ", DJCommand::checkDJPermission);
    }

    public static boolean checkDJPermission(CommandEvent event) {
        if (event.getAuthor().getId().equals(event.getClient().getOwnerId())) {
            return true;
        }
        if (event.getGuild() == null) {
            return true;
        }
        if (event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            return true;
        }

        /* Intentionally calling GuildSettingsManager instead of `bot` due to strange bug in help output */
        GuildSettings settings = (GuildSettings) new GuildSettingsManager().getSettings(event.getGuild());
        Role djRole = Objects.requireNonNull(settings).getDjRole(event.getGuild());
        return djRole != null && (event.getMember().getRoles().contains(djRole)
                || djRole.getIdLong() == event.getGuild().getIdLong());
    }
}