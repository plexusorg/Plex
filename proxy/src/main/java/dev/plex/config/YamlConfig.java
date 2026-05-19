package dev.plex.config;

import dev.plex.api.config.PlexConfiguration;
import dev.plex.settings.ServerSettings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public class YamlConfig implements PlexConfiguration
{
    private final File file;
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public YamlConfig(File dataFolder, String fileName)
    {
        this.file = new File(dataFolder, fileName);
        this.loader = YamlConfigurationLoader.builder()
                .path(this.file.toPath())
                .build();
    }

    public boolean create()
    {
        boolean created = false;
        try
        {
            Files.createDirectories(file.toPath().getParent());
            if (!file.exists())
            {
                copyDefault();
                created = true;
            }
            load();
            return created;
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Could not create configuration '" + file.getName() + "'", ex);
        }
    }

    public void load()
    {
        try
        {
            this.root = loader.load();
        }
        catch (ConfigurateException ex)
        {
            throw new IllegalStateException("Could not load configuration '" + file.getName() + "'", ex);
        }
    }

    public ServerSettings settings()
    {
        return new ServerSettings(this);
    }

    @Override
    public String getString(String path)
    {
        return node(path).getString();
    }

    @Override
    public String getString(String path, String fallback)
    {
        return node(path).getString(fallback);
    }

    @Override
    public boolean getBoolean(String path)
    {
        return getBoolean(path, false);
    }

    @Override
    public boolean getBoolean(String path, boolean fallback)
    {
        return node(path).getBoolean(fallback);
    }

    @Override
    public int getInt(String path)
    {
        return getInt(path, 0);
    }

    @Override
    public int getInt(String path, int fallback)
    {
        return node(path).getInt(fallback);
    }

    @Override
    public List<String> getStringList(String path)
    {
        return getStringList(path, List.of());
    }

    @Override
    public List<String> getStringList(String path, List<String> fallback)
    {
        try
        {
            return List.copyOf(node(path).getList(String.class, fallback));
        }
        catch (SerializationException ex)
        {
            return fallback;
        }
    }

    @Override
    public void set(String path, Object value)
    {
        try
        {
            node(path).set(value);
        }
        catch (SerializationException ex)
        {
            throw new IllegalArgumentException("Could not set configuration path '" + path + "'", ex);
        }
    }

    @Override
    public void setComments(String path, List<String> comments)
    {
        node(path).comment(comments == null || comments.isEmpty() ? null : String.join(System.lineSeparator(), comments));
    }

    @Override
    public void save()
    {
        try
        {
            loader.save(root);
        }
        catch (ConfigurateException ex)
        {
            throw new IllegalStateException("Could not save configuration '" + file.getName() + "'", ex);
        }
    }

    private void copyDefault() throws IOException
    {
        try (InputStream input = YamlConfig.class.getResourceAsStream("/" + file.getName()))
        {
            if (input == null)
            {
                throw new IllegalStateException("Missing bundled configuration '" + file.getName() + "'");
            }
            Files.copy(input, file.toPath());
        }
    }

    private CommentedConfigurationNode node(String path)
    {
        if (root == null)
        {
            throw new IllegalStateException("Configuration '" + file.getName() + "' has not been loaded");
        }
        if (path == null || path.isBlank())
        {
            return root;
        }
        return root.node((Object[]) path.split("\\."));
    }
}
