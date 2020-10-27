package me.totalfreedom.plex.cache;

import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.dao.BasicDAO;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;
import me.totalfreedom.plex.player.PlexPlayer;

public class PlexPlayerDAO extends BasicDAO<PlexPlayer, Object> {


    public PlexPlayerDAO(Class<PlexPlayer> entityclass, Datastore ds)
    {
        super(entityclass, ds);
    }

    @Override
    public boolean exists(Query<PlexPlayer> query) {
        return super.exists(query);
    }

    @Override
    public PlexPlayer findOne(String key, Object value) {
        return super.findOne(key, value);
    }

    @Override
    public PlexPlayer get(Object id) {
        return super.get(id);
    }

    @Override
    public UpdateResults update(Query<PlexPlayer> query, UpdateOperations<PlexPlayer> ops) {
        return super.update(query, ops);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public Key<PlexPlayer> save(PlexPlayer entity) {
        return super.save(entity);
    }
}
