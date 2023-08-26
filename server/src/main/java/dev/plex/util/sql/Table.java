package dev.plex.util.sql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

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
    private final List<Table> mappedTables = Lists.newArrayList();
}
