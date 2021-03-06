package com.l1sk1sh.vladikbot.commands.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.l1sk1sh.vladikbot.services.backup.BackupMediaService;
import com.l1sk1sh.vladikbot.services.backup.BackupTextChannelService;
import com.l1sk1sh.vladikbot.services.backup.DockerService;
import com.l1sk1sh.vladikbot.settings.BotSettingsManager;
import com.l1sk1sh.vladikbot.settings.Const;
import com.l1sk1sh.vladikbot.utils.CommandUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author l1sk1sh
 */
@Service
public class BackupMediaCommand extends AdminCommand {
    private static final Logger log = LoggerFactory.getLogger(BackupMediaCommand.class);

    @Qualifier("backupThreadPool")
    private final ScheduledExecutorService backupThreadPool;
    private final BotSettingsManager settings;
    private final DockerService dockerService;
    private String beforeDate;
    private String afterDate;
    private boolean useExistingBackup;

    @Autowired
    public BackupMediaCommand(ScheduledExecutorService backupThreadPool, BotSettingsManager settings, DockerService dockerService) {
        this.backupThreadPool = backupThreadPool;
        this.settings = settings;
        this.dockerService = dockerService;
        this.name = "savemedia";
        this.help = "exports all attachments of the current channel\r\n"
                + "\t\t `-b, --before <mm/dd/yyyy>` - specifies date till which export would be done\r\n"
                + "\t\t `-a, --after  <mm/dd/yyyy>` - specifies date from which export would be done\r\n"
                + "\t\t `-z, --zip` - zip flag that creates local copy of files from media links\r\n"
                + "\t\t `--format <csv|html>` - desired format of backup";
        this.arguments = "-a, -b, -f, -a, -z";
        this.guildOnly = true;
        this.useExistingBackup = true;
    }

    @Override
    public void execute(CommandEvent event) {
        if (!settings.get().isDockerRunning()) {
            return;
        }

        if (settings.get().isLockedBackup()) {
            event.replyWarning("Can't backup media - another backup is in progress!");
            return;
        }
        event.reply("Getting attachments. Be patient...");

        if (!processArguments(event.getArgs().split(" "))) {
            event.replyError(String.format("Failed to processes provided arguments: [%1$s].", event.getArgs()));
        }

        BackupTextChannelService backupTextChannelService = new BackupTextChannelService(
                settings,
                dockerService,
                event.getChannel().getId(),
                Const.BackupFileType.HTML_DARK,
                settings.get().getLocalTmpFolder(),
                beforeDate,
                afterDate,
                useExistingBackup
        );

        backupThreadPool.execute(() -> {

            /* Creating new thread from text backup service and waiting for it to finish */
            Thread backupTextChannelServiceThread = new Thread(backupTextChannelService);
            log.info("Starting backupTextChannelService...");
            backupTextChannelServiceThread.start();
            try {
                backupTextChannelServiceThread.join();
            } catch (InterruptedException e) {
                log.error("BackupTextChannel was interrupted:", e);
                event.replyError("Text channel backup process was interrupted!");
                return;
            }

            if (backupTextChannelService.hasFailed()) {
                log.error("BackupTextChannelService has failed: [{}].", backupTextChannelService.getFailMessage());
                event.replyError(String.format("Text channel backup has failed! `[%1$s]`", backupTextChannelService.getFailMessage()));
                return;
            }

            File exportedTextFile = backupTextChannelService.getBackupFile();

            BackupMediaService backupMediaService = new BackupMediaService(
                    settings,
                    event.getChannel().getId(),
                    exportedTextFile,
                    settings.get().getLocalTmpFolder(),
                    event.getArgs().split(" ")
            );

            /* Creating new thread from media backup service and waiting for it to finish */
            Thread backupMediaServiceThread = new Thread(backupMediaService);
            log.info("Starting backupMediaService...");
            backupMediaServiceThread.start();
            try {
                backupMediaServiceThread.join();
            } catch (InterruptedException e) {
                event.replyError("Media backup process was interrupted!");
                return;
            }

            if (backupMediaService.hasFailed()) {
                log.error("BackupMediaService has failed: [{}].", backupTextChannelService.getFailMessage());
                event.replyError(String.format("Media backup has filed! `[%1$s]`", backupMediaService.getFailMessage()));
                return;
            }

            File attachmentHtmlFile = backupMediaService.getAttachmentHtmlFile();
            File attachmentTxtFile = backupMediaService.getAttachmentsTxtFile();

            if (!attachmentHtmlFile.exists() || !attachmentTxtFile.exists()) {
                log.error("Media files are absent, however services reported success.");
                event.replyError("Failed to find media files!");
                return;
            }

            if (attachmentHtmlFile.length() < Const.EIGHT_MEGABYTES_IN_BYTES) {
                event.getTextChannel().sendFile(attachmentHtmlFile, attachmentHtmlFile.getName()).queue();
            } else if (attachmentTxtFile.length() < Const.EIGHT_MEGABYTES_IN_BYTES) {
                event.getTextChannel().sendFile(attachmentTxtFile, attachmentTxtFile.getName()).queue();
            } else {
                event.replyWarning("File is too big! Max file-size is 8 MiB for normal and 50 MiB for nitro users!\r\n" +
                        "Limit executed command with period: --before <mm/dd/yy> --after <mm/dd/yy>");
            }

            if (!backupMediaService.doZip()) {
                return;
            }

            File archive = backupMediaService.getZipWithAttachmentsFile();

            if (archive == null) {
                event.replyWarning("Zip wasn't complete. Clear tmp folder, or try tomorrow.");
                return;
            }

            if (archive.length() < Const.EIGHT_MEGABYTES_IN_BYTES) {
                event.getTextChannel().sendFile(backupMediaService.getZipWithAttachmentsFile(), attachmentTxtFile.getName()).queue();
            } else {
                event.replySuccess("Zip with uploaded media files could now be downloaded from local storage.");
            }
        });
    }

    private boolean processArguments(String... args) {
        if (args.length == 0) {
            return true;
        }

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-b":
                    case "-before":
                        if (CommandUtils.validateBackupDateFormat(args[i + 1])) {
                            beforeDate = (args[i + 1]);
                        } else {
                            return false;
                        }
                        break;
                    case "-a":
                    case "--after":
                        if (CommandUtils.validateBackupDateFormat(args[i + 1])) {
                            afterDate = (args[i + 1]);
                        } else {
                            return false;
                        }
                        break;
                    case "-f":
                    case "--force":

                        /* If force is specified - do not ignore existing files  */
                        useExistingBackup = false;
                        break;
                }
            }
        } catch (IndexOutOfBoundsException iobe) {
            return false;
        }

        return CommandUtils.validatePeriod(beforeDate, afterDate);
    }
}
