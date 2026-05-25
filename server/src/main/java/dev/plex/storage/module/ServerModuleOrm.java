package dev.plex.storage.module;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import dev.plex.api.storage.ModuleOrm;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerModuleOrm implements ModuleOrm
{
    private final ConnectionSource connectionSource;
    private final ServerModuleStorage storage;
    private final Map<String, Dao<?, ?>> daos = new ConcurrentHashMap<>();

    public ServerModuleOrm(ConnectionSource connectionSource, ServerModuleStorage storage)
    {
        this.connectionSource = connectionSource;
        this.storage = storage;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, ID> Dao<T, ID> dao(Class<T> entityClass, String localTableName) throws SQLException
    {
        String key = entityClass.getName() + ":" + localTableName;
        Dao<?, ?> existing = daos.get(key);
        if (existing != null)
        {
            return (Dao<T, ID>) existing;
        }

        DatabaseTableConfig<T> tableConfig = DatabaseTableConfig.fromClass(connectionSource.getDatabaseType(), entityClass);
        tableConfig.setTableName(storage.table(localTableName));
        Dao<T, ID> dao = DaoManager.createDao(connectionSource, tableConfig);
        daos.put(key, dao);
        return dao;
    }
}
