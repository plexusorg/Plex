package dev.plex.storage.permission;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.permission.Permission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SQLPermissions
{
    private static final String SELECT = "SELECT * FROM `permissions` WHERE uuid=?";
    private static final String INSERT = "INSERT INTO `permissions` (`uuid`, `permission`, `allowed`) VALUES(?, ?, ?)";
    private static final String REMOVE_PERMISSION = "DELETE FROM `permissions` WHERE uuid=? AND permission=?";
    private static final String UPDATE_PERMISSION = "UPDATE `permissions` SET allowed=? WHERE uuid=? AND permission=?";

    public List<Permission> getPermissions(UUID uuid)
    {
        List<Permission> permissions = Lists.newArrayList();
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(SELECT);
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();
            while (set.next())
            {
                Permission permission = new Permission(UUID.fromString(set.getString("uuid")), set.getString("permission"));
                permission.setAllowed(set.getBoolean("allowed"));
                permissions.add(permission);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return permissions;
    }

    public void addPermission(Permission permission)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(INSERT);
            statement.setString(1, permission.getUuid().toString());
            statement.setString(2, permission.getPermission().toLowerCase(Locale.ROOT));
            statement.setBoolean(3, permission.isAllowed());
            statement.execute();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void updatePermission(Permission permission, boolean newValue)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(UPDATE_PERMISSION);
            statement.setBoolean(1, newValue);
            statement.setString(2, permission.getUuid().toString());
            statement.setString(3, permission.getPermission().toLowerCase(Locale.ROOT));
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void removePermission(Permission permission)
    {
        this.removePermission(permission.getUuid(), permission.getPermission());
    }

    public void removePermission(UUID uuid, String permission)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(REMOVE_PERMISSION);
            statement.setString(1, uuid.toString());
            statement.setString(2, permission.toLowerCase(Locale.ROOT));
            statement.execute();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

}
