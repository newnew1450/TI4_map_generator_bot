package ti4.helpers;

import org.jetbrains.annotations.Nullable;

import ti4.ResourceHelper;

import javax.annotation.CheckForNull;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class Storage {

    public static final String MAPS_UNDO = "maps/undo/";
    public static final String MAPS = "maps/";
    public static final String DELETED_MAPS = "deletedmaps/";
    private static Font TI_FONT_20 = null;
    private static Font TI_FONT_24 = null;
    private static Font TI_FONT_26 = null;
    private static Font TI_FONT_32 = null;
    private static Font TI_FONT_50 = null;
    private static Font TI_FONT_64 = null;
    private static Path tempDir = null;

    public static Font getFont20() {
        if (TI_FONT_20 != null) {
            return TI_FONT_20;
        }
        TI_FONT_20 = getFont(20f);
        return TI_FONT_20;
    }

    public static Font getFont26() {
        if (TI_FONT_26 != null) {
            return TI_FONT_26;
        }
        TI_FONT_26 = getFont(26f);
        return TI_FONT_26;
    }

    public static Font getFont24() {
        if (TI_FONT_24 != null) {
            return TI_FONT_24;
        }
        TI_FONT_24 = getFont(24f);
        return TI_FONT_24;
    }

    public static Font getFont32() {
        if (TI_FONT_32 != null) {
            return TI_FONT_32;
        }
        TI_FONT_32 = getFont(32f);
        return TI_FONT_32;
    }

    public static Font getFont64() {
        if (TI_FONT_64 != null) {
            return TI_FONT_64;
        }
        TI_FONT_64 = getFont(64f);
        return TI_FONT_64;
    }

    public static Font getFont50() {
        if (TI_FONT_50 != null) {
            return TI_FONT_50;
        }
        TI_FONT_50 = getFont(50f);
        return TI_FONT_50;
    }

    private static Font getFont(float size) {
        Font tiFont = null;
        Path file = ResourceHelper.getInstance().getResource("/font/SLIDER.TTF");
        try (InputStream inputStream = Files.newInputStream(file)) {
            tiFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            tiFont = tiFont.deriveFont(size);
        } catch (Exception e) {
            System.err.println("Unable to load font");
            LoggerHandler.log("Could not load font", e);
        }
        return tiFont;
    }

    @CheckForNull
    public static File getMapUndoStorage(String mapName) {
        Path resource = getTempDirPath("Could not find temp directories");
        if (resource == null) return null;
        return new File(getStoragePath(resource) + MAPS_UNDO + mapName);
    }

    @CheckForNull
    public static File getMapUndoDirectory() {
        Path resource = getTempDirPath("Could not find temp directories");
        if (resource == null) return null;
        return new File(getStoragePath(resource) + MAPS_UNDO);
    }

    @CheckForNull
    public static File getMapImageStorage(String mapName) {
        Path resource = getTempDirPath("Could not find temp directories");
        if (resource == null) return null;
        System.out.println("Map image storage solved for " + getStoragePath(resource) + MAPS + mapName );
        return new File(getStoragePath(resource) + MAPS + mapName);
    }

    @CheckForNull
    public static File getMapImageDirectory() {
        Path resource = getTempDirPath("Could not find temp directories");
        if (resource == null) return null;
        return new File(getStoragePath(resource) + MAPS);
    }

    @Nullable
    private static Path getTempDirPath(String Could_not_find_temp_directories) {
        if(tempDir == null) {        
            try {
                tempDir = Files.createTempDirectory("ti4bot");
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (tempDir == null) {
                LoggerHandler.log(Could_not_find_temp_directories);
                System.err.println(Could_not_find_temp_directories);
                return null;
            }
        }
        return tempDir;
    }

    @CheckForNull
    public static File getMapStorage(String mapName) {
        Path resource = getTempDirPath("Could not find temp directories for maps");
        if (resource == null) return null;
        return new File(resource + MAPS + mapName);
    }

    @CheckForNull
    public static File getDeletedMapStorage(String mapName) {
        Path resource = getTempDirPath("Could not find temp directories for maps");
        if (resource == null) return null;
        return new File(resource + DELETED_MAPS + mapName);
    }

    public static void init() {
        Path resource = getTempDirPath("Could not find temp directories for maps");
        createDirectory(resource, DELETED_MAPS);
        createDirectory(resource, MAPS);
    }

    private static void createDirectory(Path resource, String directoryName) {
        File directory = new File(resource + directoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }


    @CheckForNull
    public static File getLoggerFile() {
        Path resource = getTempDirPath("Could not find temp directories");
        if (resource == null) return null;
        return new File(getStoragePath(resource) + "/log.txt");
    }

    private static String getStoragePath(Path resource) {
        String envPath = System.getenv("DB_PATH");
        return envPath != null ? envPath : resource.toString();
    }
}
