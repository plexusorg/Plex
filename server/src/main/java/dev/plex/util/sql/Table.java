package dev.plex.util.sql;

import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Taah
 * @since 5:30 AM [26-08-2023]
 */

@Data
@Accessors(fluent = true)
public class Table
{
    private final String name;
    private final Map<String, SQLUtil.Mapper> columns = Maps.newHashMap();
    private final Map<Field, Table> mappedTables = Maps.newHashMap();
}
