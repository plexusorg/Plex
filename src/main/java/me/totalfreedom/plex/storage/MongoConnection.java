package me.totalfreedom.plex.storage;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.player.PlexPlayer;

public class MongoConnection
{
    // USE MORPHIA API FOR MONGO <3

    private final Plex plugin = Plex.get();

    public Datastore getDatastore()
    {
        if (!plugin.config.getString("data.central.storage").equalsIgnoreCase("mongodb"))
        {
            return null;
        }
        String host = plugin.config.getString("data.central.hostname");
        int port = plugin.config.getInt("data.central.port");
        String username = plugin.config.getString("data.central.user");
        String password = plugin.config.getString("data.central.password");
        String database = plugin.config.getString("data.central.db");

        String connectionString = "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/?authSource=" + database;
        MongoClient client = new MongoClient(new MongoClientURI(connectionString));
        Morphia morphia = new Morphia();
        Datastore datastore = morphia.createDatastore(client, database);
        datastore.getMapper().addMappedClass(PlexPlayer.class);
        datastore.ensureIndexes();
        plugin.setStorageType(StorageType.MONGO);
        return datastore;
    }
}
