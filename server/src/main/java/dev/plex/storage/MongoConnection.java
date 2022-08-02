package dev.plex.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import dev.plex.PlexBase;
import dev.plex.player.PlexPlayer;
import dev.plex.storage.codec.ZonedDateTimeCodec;
import dev.plex.util.PlexLog;
import org.bson.codecs.configuration.CodecRegistries;

public class MongoConnection implements PlexBase
{
    // USE MORPHIA API FOR MONGO <3

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

        String connectionString;
        if (username != null && password != null && !username.isEmpty() && !password.isEmpty())
        {
            if (database != null && !database.isEmpty())
            {
                connectionString = "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/?authSource=" + database + "&uuidRepresentation=STANDARD";
            }
            else
            {
                connectionString = "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/?uuidRepresentation=STANDARD";
            }
        }
        else
        {
            connectionString = "mongodb://" + host + ":" + port + "/?uuidRepresentation=STANDARD";
        }
        PlexLog.debug("Using mongo connection string: " + connectionString);
        MongoClient client = MongoClients.create(MongoClientSettings.builder().codecRegistry(CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromCodecs(new ZonedDateTimeCodec()))).applyConnectionString(new ConnectionString(connectionString)).build());
        Datastore datastore = Morphia.createDatastore(client, database == null ? "admin" : database, MapperOptions.DEFAULT);
        datastore.getMapper().map(PlexPlayer.class);
        datastore.ensureIndexes();
        plugin.setStorageType(StorageType.MONGODB);
        return datastore;
    }
}
