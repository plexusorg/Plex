package me.totalfreedom.plex.cache;

import me.totalfreedom.plex.player.PlexPlayer;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

public class PlexPlayerDAO extends BasicDAO<PlexPlayer, Object>
{
    public PlexPlayerDAO(Class<PlexPlayer> entityclass, Datastore ds)
    {
        super(entityclass, ds);
    }

    @Override
    public boolean exists(Query<PlexPlayer> query)
    {
        return super.exists(query);
    }

    @Override
    public PlexPlayer findOne(String key, Object value)
    {
        return super.findOne(key, value);
    }

    @Override
    public PlexPlayer get(Object id)
    {
        return super.get(id);
    }

    @Override
    public UpdateResults update(Query<PlexPlayer> query, UpdateOperations<PlexPlayer> ops)
    {
        return super.update(query, ops);
    }

    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj);
    }

    @Override
    public Key<PlexPlayer> save(PlexPlayer entity)
    {
        return super.save(entity);
    }
}
