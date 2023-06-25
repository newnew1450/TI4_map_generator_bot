package ti4.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import ti4.helpers.Storage;
import ti4.model.InstallationModel;
import ti4.model.PlanetModel;
import ti4.model.TileModel;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class InstallationHelper {

    private static final java.util.Map<String, InstallationModel> allInstallations = new HashMap<>();

    public static void init() {
        initInstallationsFromJson();
    }


    public static java.util.Map<String, InstallationModel> getAllInstallations() {
        return allInstallations;
    }

    public static void initInstallationsFromJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String resourcePath = Storage.getResourcePath() + File.separator + "installations" + File.separator;
        String storagePath = Storage.getStoragePath() + File.separator + "installations" + File.separator;
        List<File> files = new java.util.ArrayList<>();
        File[] storedFiles = new File(storagePath).listFiles();

        if(Optional.ofNullable(storedFiles).isPresent() && CollectionUtils.isNotEmpty(List.of(storedFiles))) {
            files.addAll(Stream.of(storedFiles)
                    .filter(file -> !file.isDirectory())
                    .toList());
        }
        files.addAll(Stream.of(new File(resourcePath).listFiles())
                .filter(file -> !file.isDirectory())
                .toList());

        files.forEach(file -> {
            try {
                InstallationModel installation = objectMapper.readValue(new FileInputStream(file), InstallationModel.class);
                allInstallations.put(installation.getId(), installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    public static void addNewInstallationToList(InstallationModel installation) {
        allInstallations.put(installation.getId(), installation);
    }
}
