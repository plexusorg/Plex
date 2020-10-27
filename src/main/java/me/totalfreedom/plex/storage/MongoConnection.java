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

    private Plex plugin = Plex.get();

    public Datastore getDatastore()
    {
        if (!plugin.getConfig().getString("data.central.storage").equalsIgnoreCase("mongodb"))
        {
            return null;
        }
        String host = plugin.getConfig().getString("data.central.hostname");
        int port = plugin.getConfig().getInt("data.central.port");
        String username = plugin.getConfig().getString("data.central.user");
        String password = plugin.getConfig().getString("data.central.password");
        String database = plugin.getConfig().getString("data.central.db");

        String connectionString = "mongodb://" + username  + ":" + password + "@" + host + ":" + port + "/?authSource=" + database;

        MongoClient client = new MongoClient(new MongoClientURI(connectionString));

        Morphia morphia = new Morphia();
        Datastore datastore = morphia.createDatastore(client, database);

        datastore.getMapper().addMappedClass(PlexPlayer.class);
        datastore.ensureIndexes();

        plugin.setStorageType(StorageType.MONGO);

        return datastore;
    }

}
