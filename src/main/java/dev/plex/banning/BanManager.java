package dev.plex.banning;

import com.google.common.collect.Lists;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperators;
import dev.plex.Plex;
import dev.plex.storage.StorageType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BanManager
{
    private final String SELECT = "SELECT * FROM `bans` WHERE uuid=?";
    private final String INSERT = "INSERT INTO `bans` (`banID`, `uuid`, `banner`, `ip`, `reason`, `enddate`, `active`) VALUES (?, ?, ?, ?, ?, ?, ?);";

    public void executeBan(Ban ban)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            Plex.get().getMongoConnection().getDatastore().save(ban);
        }
        else
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement(INSERT);
                statement.setString(1, ban.getId());
                statement.setString(2, ban.getUuid().toString());
                statement.setString(3, ban.getBanner() == null ? "" : ban.getBanner().toString());
                statement.setString(4, ban.getIp());
                statement.setString(5, ban.getReason());
                statement.setLong(6, ban.getEndDate().toInstant().toEpochMilli());
                statement.setBoolean(7, ban.isActive());
                statement.execute();

            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
    }

    public boolean isBanned(UUID uuid)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return Plex.get().getMongoConnection().getDatastore().find(Ban.class)
                    .filter(Filters.eq("uuid", uuid.toString())).filter(Filters.eq("active", true)).first() != null;
        }
        else
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement(SELECT);
                statement.setString(1, uuid.toString());
                ResultSet set = statement.executeQuery();
                if (!set.next())
                {
                    return false;
                }
                while (set.next())
                {
                    if (set.getBoolean("active"))
                    {
                        return true;
                    }
                }
            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
        return false;
    }

    public void unban(UUID uuid)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            Query<Ban> query = Plex.get().getMongoConnection().getDatastore().find(Ban.class).filter(Filters.eq("uuid", uuid.toString())).filter(Filters.eq("active", true));
            if (query.first() != null)
            {
                query.update(UpdateOperators.set("active", false)).execute();
            }
        }
        else
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("UPDATE `bans` SET active=? WHERE uuid=?");
                statement.setBoolean(1, false);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
    }

    public void unban(String id)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            Query<Ban> query = Plex.get().getMongoConnection().getDatastore().find(Ban.class).filter(Filters.eq("_id", id)).filter(Filters.eq("active", true));
            if (query.first() != null)
            {
                query.update(UpdateOperators.set("active", false)).execute();
            }
        }
        else
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("UPDATE `bans` SET active=? WHERE banID=?");
                statement.setBoolean(1, false);
                statement.setString(2, id);
                statement.executeUpdate();
            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
    }

    public List<Ban> getActiveBans()
    {
        List<Ban> bans = Lists.newArrayList();
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            for (Ban ban : Plex.get().getMongoConnection().getDatastore().find(Ban.class).filter(Filters.eq("active", true)))
            {
                bans.add(ban);
            }
        }
        else
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("SELECT * FROM `bans`");
                ResultSet set = statement.executeQuery();
                while (set.next())
                {
                    if (set.getBoolean("active"))
                    {
                        String id = set.getString("banID");
                        UUID uuid = UUID.fromString(set.getString("uuid"));
                        UUID banner = set.getString("banner").isEmpty() ? null : UUID.fromString(set.getString("banner"));
                        String ip = set.getString("ip");
                        String reason = set.getString("reason");
                        Date endDate = set.getLong("enddate") != 0 ? new Date(set.getLong("enddate")) : null;
                        Ban ban = new Ban(id, uuid, banner, ip, reason, endDate);
                        bans.add(ban);
                    }
                }
            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
        return bans;
    }
}
