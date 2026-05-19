package dev.plex.storage;

import dev.plex.Plex;
import dev.plex.storage.database.Database;

/**
 * Database bootstrap and connection holder.
 *
 * <p>The historical name is kept so existing module-facing accessors do not break.</p>
 */
public class SQLConnection extends Database
{
    public SQLConnection(Plex plugin)
    {
        super(plugin);
    }

    public java.sql.Connection getCon() throws java.sql.SQLException
    {
        return getConnection();
    }
}
