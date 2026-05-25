package dev.plex.api.storage;

import com.j256.ormlite.dao.Dao;
import java.sql.SQLException;

/**
 * Creates ORMLite DAOs for module-scoped tables.
 */
public interface ModuleOrm
{
    /**
     * Creates or returns a cached DAO for a module-local table.
     *
     * @param entityClass ORMLite entity class
     * @param localTableName module-local table name
     * @param <T> entity type
     * @param <ID> entity ID type
     * @return ORMLite DAO using the module-prefixed physical table
     * @throws SQLException if the DAO cannot be created
     */
    <T, ID> Dao<T, ID> dao(Class<T> entityClass, String localTableName) throws SQLException;
}
