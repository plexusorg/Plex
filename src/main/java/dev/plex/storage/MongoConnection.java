package dev.plex.storage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import dev.plex.Plex;
import dev.plex.player.PlexPlayer;

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
        MongoClient client = MongoClients.create(connectionString);
        Datastore datastore = Morphia.createDatastore(client, database, MapperOptions.DEFAULT);
        datastore.getMapper().map(PlexPlayer.class);
        datastore.ensureIndexes();
        plugin.setStorageType(StorageType.MONGODB);
        return datastore;
    }
}
