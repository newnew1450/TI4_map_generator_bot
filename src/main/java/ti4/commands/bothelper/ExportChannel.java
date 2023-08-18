package ti4.commands.bothelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListContainersParam;
import com.spotify.docker.client.DockerClient.ListImagesParam;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.Info;
import com.spotify.docker.client.messages.Version;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.MapGenerator;
import ti4.ResourceHelper;
import ti4.helpers.Constants;
import ti4.helpers.Storage;

public class ExportChannel extends BothelperSubcommandData {
        public ExportChannel(){
        super(Constants.EXPORT_CHANNEL, "Exports a channel to a file");
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
            .forEach(consumer);
        }
    }

    public void execute(SlashCommandInteractionEvent event) {
        ExportChannel(event.getChannel().getId());
        sendMessage("Channel exported!");
    }

    public void ExportChannel(String channelID) {
        try {
            System.out.println("START WITH DOCKER");
            DockerClient docker = DefaultDockerClient.fromEnv().build();
            System.out.println(docker.getHost());
            Info info = docker.info();
            Version version = docker.version();
            String pingResponse = docker.ping();

            List<Container> containers = docker.listContainers(ListContainersParam.allContainers());
            System.out.println("Containers: " + containers.size());
            for (Container container : containers) {
                System.out.println(container.names() + " image: " + container.image());
            }

            List<Image> images = docker.listImages(ListImagesParam.allImages());
            System.out.println("Images: " + images.size());
            for (Image image : images) {
                System.out.println(image.repoDigests());
            }
            System.out.println("DONE WITH DOCKER");
        } catch (DockerCertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DockerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        ProcessBuilder builder = new ProcessBuilder();
        String path = Storage.getStoragePath() + "/exported_channels";
        builder.directory(new File(path));
        System.out.println("directory: " + builder.directory());
        builder.inheritIO();
        builder.environment();
        if (isWindows) {
            String windowsCommand = "docker run --rm -v " + path + ":/out tyrrrz/discordchatexporter:stable export -t " + MapGenerator.botToken + " -c " + channelID;
            builder.command(windowsCommand);
        } else {
            String otherCommand =   "docker run --rm -v " + path + ":/out tyrrrz/discordchatexporter:stable export -t " + MapGenerator.botToken + " -c " + channelID;
            builder.command(otherCommand);
        }
        
        Process process;
        try {
            process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            // Future<?> future = executorService.submit(streamGobbler);
            int exitCode = process.waitFor();
            // assertDoesNotThrow(() -> future.get(10, TimeUnit.SECONDS));
            // assertEquals(0, exitCode);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

