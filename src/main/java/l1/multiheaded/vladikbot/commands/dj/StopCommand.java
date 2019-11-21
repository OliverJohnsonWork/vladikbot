package l1.multiheaded.vladikbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import l1.multiheaded.vladikbot.Bot;
import l1.multiheaded.vladikbot.services.audio.AudioHandler;

/**
 * @author Oliver Johnson
 * Changes from original source:
 * - Reformating code
 * @author John Grosh
 */
public class StopCommand extends DJCommand {
    public StopCommand(Bot bot) {
        super(bot);
        this.name = "stop";
        this.help = "stops the current song and clears the queue";
        this.bePlaying = false;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler audioHandler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        audioHandler.stopAndClear();
        event.getGuild().getAudioManager().closeAudioConnection();
        event.replySuccess("The player has stopped and the queue has been cleared.");
    }
}