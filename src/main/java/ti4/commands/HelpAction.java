package ti4.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import ti4.ResourceHelper;
import ti4.helpers.Constants;
import ti4.helpers.Storage;
import ti4.message.MessageHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class HelpAction implements Command {

    @Override
    public String getActionID() {
        return Constants.HELP;
    }

    @Override
    public boolean accept(SlashCommandInteractionEvent event) {
        return event.getName().equals(getActionID());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageHelper.sendMessageToChannel(event.getChannel(), "Help information is in help file");
        Path helpFile = ResourceHelper.getInstance().getHelpFile("help.txt");
        if (helpFile != null){
            InputStream file;
            try {
                file = Files.newInputStream(helpFile);
                if (file.available()==0){
                    MessageHelper.sendMessageToChannel(event.getChannel(), "Could not find help file");
                    return;
                }
                MessageHelper.sendFileToChannel(event.getChannel(), file);
            } catch (IOException e) {
                // shavnote = fixthis
                e.printStackTrace();
            }

        } else {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Could not find help file");
        }
        MessageHelper.replyToMessageTI4Logo(event);
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void registerCommands(CommandListUpdateAction commands) {
        // Moderation commands with required options
        commands.addCommands(
                Commands.slash(getActionID(), "Help Action")

        );
    }
}
