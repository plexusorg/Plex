package dev.plex.util.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.plex.punishment.PunishmentType;
import dev.plex.storage.annotation.*;
import dev.plex.util.PlexLog;
import dev.plex.util.ReflectionsUtil;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Taah
 * @since 4:28 AM [25-08-2023]
 */
public class SQLUtil
{
    public static final List<Table> TABLES = Lists.newArrayList();

    public static List<String> createTable(List<String> result, Class<?> clazz)
    {
        if (!clazz.isAnnotationPresent(SQLTable.class))
        {
            PlexLog.error("Unable to map {0} to a table, it is missing the SQLTable's annotation", clazz.getName());
            return null;
        }
        final List<Field> collectionFields = Lists.newArrayList();

        final Table table = new Table(clazz.getAnnotation(SQLTable.class).value());

        final StringBuilder mainResult = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + table.name() + "` (");
        final List<Field> declaredFields = Arrays.stream(clazz.getDeclaredFields()).filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())).collect(Collectors.toList());
        final List<Field> iterating = declaredFields.stream().toList();
        for (Field value : iterating)
        {
            if (Collection.class.isAssignableFrom(value.getType()))
            {
                collectionFields.add(value);
                declaredFields.remove(value);
            }
        }
        Field primaryKey = null;

        for (int i = 0; i < declaredFields.size(); i++)
        {
            Field declaredField = declaredFields.get(i);
            final Mapper mapped = Mapper.getByClass(declaredField.getType());
            if (mapped == null)
            {
                PlexLog.warn("Could not map field {0} for class {1}", declaredField.getName(), clazz.getName());
                continue;
            }
            if (declaredField.isAnnotationPresent(PrimaryKey.class))
            {
                if (primaryKey != null)
                {
                    PlexLog.error("You can only have one primary key for a table! The class {0} has more than one!", clazz.getName());
                    return ImmutableList.of();
                }
                primaryKey = declaredField;
            }

            writeFieldToSQL(table, mainResult, mapped, declaredField);
            if (i < declaredFields.size() - 1)
            {
                mainResult.append(", ");
            }
        }
        if (primaryKey != null && !primaryKey.getAnnotation(PrimaryKey.class).dontSet())
        {
            mainResult.append(", PRIMARY KEY (`").append(primaryKey.getName()).append("`)");
        }
        mainResult.append(");");
        result.add(mainResult.toString());

        TABLES.add(table);

        if (primaryKey == null && !collectionFields.isEmpty())
        {
            PlexLog.error("You must define a primary key to point to if you wish to have a list saved. You can use @PrimaryKey(dontSet = true) to make sure that SQL does not save it as a primary key.");
            return ImmutableList.of();
        }

        Field finalPrimaryKey = primaryKey;
        collectionFields.forEach(field ->
        {
            final String tableName = field.getName() + "To" + StringUtils.capitalize(clazz.getSimpleName());
            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + tableName + "` (");
            if (field.isAnnotationPresent(MapObjectList.class))
            {
                createTable(result, ReflectionsUtil.getGenericField(field));
                return;
            }
            final Mapper mapped = Mapper.getByClass(ReflectionsUtil.getGenericField(field));
            if (mapped == null)
            {
                PlexLog.warn("Could not map collection field {0} for class {1}", field.getName(), clazz.getName());
                return;
            }
            final Table listTable = new Table(tableName);
            writeFieldToSQL(listTable, sql, mapped, field);
            sql.append(", ");
            writeFieldToSQL(listTable, sql, Mapper.getByClass(finalPrimaryKey.getType()), finalPrimaryKey);
            sql.append(");");
            result.add(sql.toString());
            table.mappedTables().add(listTable);
        });
        return result;
    }

    private static void writeFieldToSQL(Table table, StringBuilder sb, Mapper mapped, Field field)
    {

        sb.append("`").append(field.getName()).append("` ");
        if (mapped == Mapper.VARCHAR)
        {
            if (field.isAnnotationPresent(NoLimit.class))
            {
                sb.append("TEXT");
                table.columns().put(field.getName(), Mapper.TEXT);
            }
            else
            {
                sb.append(mapped.name());
                table.columns().put(field.getName(), mapped);
            }
        }
        else
        {
            sb.append(mapped.name());
            table.columns().put(field.getName(), mapped);
        }
        if (mapped == Mapper.VARCHAR && !field.isAnnotationPresent(NoLimit.class))
        {
            if (UUID.class.isAssignableFrom(field.getType()))
            {
                sb.append(" (").append(36).append(")");
            }
            else if (field.isAnnotationPresent(VarcharLimit.class))
            {
                int limit = field.getAnnotation(VarcharLimit.class).value();
                sb.append(" (").append(limit).append(")");
            }
            else
            {
                sb.append("(65535)");
            }
        }
        if (field.isAnnotationPresent(NotNull.class))
        {
            sb.append(" NOT NULL");
        }
    }

    @Accessors(fluent = true)
    public enum Mapper
    {
        VARCHAR(String.class, UUID.class, PunishmentType.class),
        BOOLEAN(Boolean.class, boolean.class),
        BIGINT(Long.class, long.class, ZonedDateTime.class),
        INT(Integer.class, int.class),
        TEXT;

        private final Class<?>[] clazz;

        Mapper(Class<?>... clazz)
        {
            this.clazz = clazz;
        }

        public static Mapper getByClass(Class<?> clazz)
        {
            return Arrays.stream(values()).filter(mapper -> mapper.clazz != null && Arrays.asList(mapper.clazz).contains(clazz)).findFirst().orElse(null);
        }
    }
}
