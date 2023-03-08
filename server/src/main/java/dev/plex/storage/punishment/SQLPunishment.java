package dev.plex.storage.punishment;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLPunishment
{
    private static final String SELECT = "SELECT * FROM `punishments` WHERE punished=?";
    private static final String SELECT_BY = "SELECT * FROM `punishments` WHERE punisher=?";

    private static final String INSERT = "INSERT INTO `punishments` (`punished`, `punisher`, `punishedUsername`, `ip`, `type`, `reason`, `customTime`, `active`, `endDate`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_BAN = "UPDATE `punishments` SET active=? WHERE active=? AND punished=? AND type=?";

    public CompletableFuture<List<Punishment>> getPunishments()
    {
        return CompletableFuture.supplyAsync(() ->
        {
            List<Punishment> punishments = Lists.newArrayList();
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("SELECT * FROM `punishments`");
                ResultSet set = statement.executeQuery();
                while (set.next())
                {
                    Punishment punishment = new Punishment(UUID.fromString(set.getString("punished")), set.getString("punisher") != null && set.getString("punisher").isEmpty() ? UUID.fromString(set.getString("punisher")) : null);
                    punishment.setActive(set.getBoolean("active"));
                    punishment.setType(PunishmentType.valueOf(set.getString("type")));
                    punishment.setCustomTime(set.getBoolean("customTime"));
                    punishment.setPunishedUsername(set.getString("punishedUsername"));
                    punishment.setEndDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(set.getLong("endDate")), ZoneId.of(TimeUtils.TIMEZONE)));
                    punishment.setReason(set.getString("reason"));
                    punishment.setIp(set.getString("ip"));
                    punishments.add(punishment);
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return punishments;
            }
            return punishments;
        });
    }

    public List<Punishment> getPunishments(UUID uuid)
    {
        List<Punishment> punishments = Lists.newArrayList();
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(SELECT);
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();
            while (set.next())
            {
                Punishment punishment = new Punishment(UUID.fromString(set.getString("punished")), set.getString("punisher") == null ? null : UUID.fromString(set.getString("punisher")));
                punishment.setActive(set.getBoolean("active"));
                punishment.setType(PunishmentType.valueOf(set.getString("type")));
                punishment.setCustomTime(set.getBoolean("customTime"));
                punishment.setPunishedUsername(set.getString("punishedUsername"));
                punishment.setEndDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(set.getLong("endDate")), ZoneId.of(TimeUtils.TIMEZONE)));
                punishment.setReason(set.getString("reason"));
                punishment.setIp(set.getString("ip"));
                punishments.add(punishment);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return punishments;
    }

    public CompletableFuture<Void> insertPunishment(Punishment punishment)
    {

        return CompletableFuture.runAsync(() ->
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PlexLog.debug("Running execute punishment on " + punishment.getPunished().toString());
                PreparedStatement statement = con.prepareStatement(INSERT);
                statement.setString(1, punishment.getPunished().toString());
                statement.setString(2, punishment.getPunisher() == null ? null : punishment.getPunisher().toString());
                statement.setString(3, punishment.getPunishedUsername());
                statement.setString(4, punishment.getIp());
                statement.setString(5, punishment.getType().name());
                statement.setString(6, punishment.getReason());
                statement.setBoolean(7, punishment.isCustomTime());
                statement.setBoolean(8, punishment.isActive());
                statement.setLong(9, punishment.getEndDate().toInstant().toEpochMilli());
                PlexLog.debug("Executing punishment");
                statement.execute();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        });
    }

    public void syncRemoveBan(UUID uuid)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(UPDATE_BAN);
            statement.setBoolean(1, false);
            statement.setBoolean(2, true);
            statement.setString(3, uuid.toString());
            statement.setString(4, PunishmentType.BAN.name());

            PreparedStatement statement1 = con.prepareStatement(UPDATE_BAN);
            statement1.setBoolean(1, false);
            statement1.setBoolean(2, true);
            statement1.setString(3, uuid.toString());
            statement1.setString(4, PunishmentType.TEMPBAN.name());
            statement1.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Void> removeBan(UUID uuid)
    {
        return CompletableFuture.runAsync(() ->
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement(UPDATE_BAN);
                statement.setBoolean(1, false);
                statement.setBoolean(2, true);
                statement.setString(3, uuid.toString());
                statement.setString(4, PunishmentType.BAN.name());
                statement.executeUpdate();

                PreparedStatement statement1 = con.prepareStatement(UPDATE_BAN);
                statement1.setBoolean(1, false);
                statement1.setBoolean(2, true);
                statement1.setString(3, uuid.toString());
                statement1.setString(4, PunishmentType.TEMPBAN.name());
                statement1.executeUpdate();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        });
    }
}
