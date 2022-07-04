package ti4;

import ti4.helpers.LoggerHandler;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;

public class ResourceHelper {
    private static ResourceHelper resourceHelper = null;
    private HashMap<String, Path> unitCache = new HashMap<>();
    private HashMap<String, Path> tileCache = new HashMap<>();
    private HashMap<String, Path> ccCache = new HashMap<>();
    private HashMap<String, Path> attachmentCache = new HashMap<>();
    private HashMap<String, Path> tokenCache = new HashMap<>();
    private HashMap<String, Path> factionCache = new HashMap<>();
    private HashMap<String, Path> generalCache = new HashMap<>();
    private HashMap<String, Path> planetCache = new HashMap<>();
    private HashMap<String, Path> paCache = new HashMap<>();
    
    private FileSystem fs = null;

    private ResourceHelper() {
    }

    public static ResourceHelper getInstance() {
        if (resourceHelper == null) {
            resourceHelper = new ResourceHelper();
        }
        return resourceHelper;
    }

    public Path getResource(String name) {
        Path resourcePath = null;
        URI resource;
        try {
            resource = getClass().getClassLoader().getResource("resources/" + name).toURI();
            String[] array = resource.toString().split("!");
            if(fs==null) {
                fs = FileSystems.newFileSystem(URI.create(array[0]),Collections.emptyMap());
            }
            if (resource != null) {
                System.out.println("URI for " + name + " is "+resource);
                Path path = fs.getPath(array[1]);
                System.out.println("Obtained path for " +  name + " and it is " + path);
                resourcePath = fs.getPath(array[1]);
            }else {
                System.out.println("Resource is null for " + name);
            }
        } catch (URISyntaxException e1) {
            
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        

        return resourcePath != null ? resourcePath : null;
    }

    @CheckForNull
    public Path getPositionFile(String name)
    {
        return getResourceFromFolder("positions/", name, "Could not find position files");
    }

    @CheckForNull
    public Path getTileFile(String name)
    {
        Path unitPath = tileCache.get(name);
        if (unitPath != null)
        {
            return unitPath;
        }
        Path tile = getResourceFromFolder("tiles/", name, "Could not find tile file");
        tileCache.put(name, tile);
        return tile;
    }

    @CheckForNull
    public Path getFactionFile(String name)
    {
        Path unitPath = factionCache.get(name);
        if (unitPath != null)
        {
            return unitPath;
        }
        Path tile = getResourceFromFolder("factions/", name, "Could not find faction file");
        factionCache.put(name, tile);
        return tile;
    }

    @CheckForNull
    public Path getGeneralFile(String name)
    {
        Path unitPath = generalCache.get(name);
        if (unitPath != null)
        {
            return unitPath;
        }
        Path tile = getResourceFromFolder("general/", name, "Could not find faction file");
        generalCache.put(name, tile);
        return tile;
    }

    @CheckForNull
    public Path getUnitFile(String name) {
        Path unitPath = unitCache.get(name);
        if (unitPath != null) {
            return unitPath;
        }
        Path unit = getResourceFromFolder("units/new_units/", name, "Could not find unit file");
        unitCache.put(name, unit);
        return unit;
    }
    @CheckForNull
    public Path getCCFile(String name)
    {
        Path ccPath = ccCache.get(name);
        if (ccPath != null)
        {
            return ccPath;
        }
        Path cc = getResourceFromFolder("command_token/", name, "Could not find command token file");
        ccCache.put(name, cc);
        return cc;
    }

    @CheckForNull
    public Path getAttachmentFile(String name)
    {
        Path tokenPath = attachmentCache.get(name);
        if (tokenPath != null)
        {
            return tokenPath;
        }
        Path token = getResourceFromFolder("attachment_token/", name, "Could not find attachment token file");
        attachmentCache.put(name, token);
        return token;
    }

    @CheckForNull
    public Path getPlanetResource(String name)
    {
        Path planetInfoPath = planetCache.get(name);
        if (planetInfoPath != null)
        {
            return planetInfoPath;
        }
        Path token = getResourceFromFolder("planet_cards/", name, "Could not find planet token file");
        planetCache.put(name, token);
        return token;
    }

    @CheckForNull
    public Path getPAResource(String name)
    {
        Path paInfoPath = paCache.get(name);
        if (paInfoPath != null)
        {
            return paInfoPath;
        }
        Path token = getResourceFromFolder("player_area/", name, "Could not find player area token file");
        paCache.put(name, token);
        return token;
    }

    @CheckForNull
    public Path getTokenFile(String name)
    {
        Path tokenPath = tokenCache.get(name);
        if (tokenPath != null)
        {
            return tokenPath;
        }
        Path token = getResourceFromFolder("tokens/", name, "Could not find token file");
        tokenCache.put(name, token);
        return token;
    }

    public Path getResourceFromFolder(String folder, String name, String errorDescription) {
        Path resourcePath = null;
        URI resource;
        try {
            resource = getClass().getClassLoader().getResource("resources/" + folder + name).toURI();
            String[] array = resource.toString().split("!");
            if(fs==null) {
                fs = FileSystems.newFileSystem(URI.create(array[0]),Collections.emptyMap());
            }
            if (resource != null) {
                System.out.println("URI for " + folder + name + " is "+resource);
                Path path = fs.getPath(array[1]);
                System.out.println("Obtained path for " + folder + name + " and it is " + path);
                resourcePath = fs.getPath(array[1]);
            }else {
                System.out.println("Resource is null for " + folder + name);
            }
        } catch (URISyntaxException e1) {
            
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        

        return resourcePath != null ? resourcePath : null;
    }

    @CheckForNull
    public Path getInfoFile(String name)
    {
        return getResourceFromFolder("info/", name, "Could not find info file");
    }

    @CheckForNull
    public Path getAliasFile(String name)
    {
        return getResourceFromFolder("alias/", name, "Could not find alias file");
    }

    @CheckForNull
    public Path getHelpFile(String name)
    {
        return getResourceFromFolder("help/", name, "Could not find alias file");
    }
}
