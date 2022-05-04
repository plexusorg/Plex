package dev.plex.toml;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class Results {
  
  static class Errors {
    
    private final StringBuilder sb = new StringBuilder();
    
    void duplicateTable(String table, int line) {
      sb.append("Duplicate table definition on line ")
        .append(line)
        .append(": [")
        .append(table)
        .append("]");
    }

    public void tableDuplicatesKey(String table, AtomicInteger line) {
      sb.append("Key already exists for table defined on line ")
        .append(line.get())
        .append(": [")
        .append(table)
        .append("]");
    }

    public void keyDuplicatesTable(String key, AtomicInteger line) {
      sb.append("Table already exists for key defined on line ")
        .append(line.get())
        .append(": ")
        .append(key);
    }
    
    void emptyImplicitTable(String table, int line) {
      sb.append("Invalid table definition due to empty implicit table name: ")
        .append(table);
    }
    
    void invalidTable(String table, int line) {
      sb.append("Invalid table definition on line ")
        .append(line)
        .append(": ")
        .append(table)
        .append("]");
    }
    
    void duplicateKey(String key, int line) {
      sb.append("Duplicate key");
      if (line > -1) {
        sb.append(" on line ")
          .append(line);
      }
      sb.append(": ")
        .append(key);
    }
    
    void invalidTextAfterIdentifier(dev.plex.toml.Identifier identifier, char text, int line) {
      sb.append("Invalid text after key ")
        .append(identifier.getName())
        .append(" on line ")
        .append(line)
        .append(". Make sure to terminate the value or add a comment (#).");
    }
    
    void invalidKey(String key, int line) {
      sb.append("Invalid key on line ")
        .append(line)
        .append(": ")
        .append(key);
    }
    
    void invalidTableArray(String tableArray, int line) {
      sb.append("Invalid table array definition on line ")
        .append(line)
        .append(": ")
        .append(tableArray);
    }
    
    void invalidValue(String key, String value, int line) {
      sb.append("Invalid value on line ")
        .append(line)
        .append(": ")
        .append(key)
        .append(" = ")
        .append(value);
    }
    
    void unterminatedKey(String key, int line) {
      sb.append("Key is not followed by an equals sign on line ")
        .append(line)
        .append(": ")
        .append(key);
    }
    
    void unterminated(String key, String value, int line) {
      sb.append("Unterminated value on line ")
        .append(line)
        .append(": ")
        .append(key)
        .append(" = ")
        .append(value.trim());
    }

    public void heterogenous(String key, int line) {
      sb.append(key)
        .append(" becomes a heterogeneous array on line ")
        .append(line);
    }
    
    boolean hasErrors() {
      return sb.length() > 0;
    }
    
    @Override
    public String toString() {
      return sb.toString();
    }

    public void add(Errors other) {
      sb.append(other.sb);
    }
  }
  
  final Errors errors = new Errors();
  private final Set<String> tables = new HashSet<String>();
  private final Deque<dev.plex.toml.Container> stack = new ArrayDeque<dev.plex.toml.Container>();

  Results() {
    stack.push(new dev.plex.toml.Container.Table(""));
  }

  void addValue(String key, Object value, AtomicInteger line) {
    dev.plex.toml.Container currentTable = stack.peek();
    
    if (value instanceof Map) {
      String path = getInlineTablePath(key);
      if (path == null) {
        startTable(key, line);
      } else if (path.isEmpty()) {
        startTables(dev.plex.toml.Identifier.from(key, null), line);
      } else {
        startTables(dev.plex.toml.Identifier.from(path, null), line);
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> valueMap = (Map<String, Object>) value;
      for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
        addValue(entry.getKey(), entry.getValue(), line);
      }
      stack.pop();
    } else if (currentTable.accepts(key)) {
      currentTable.put(key, value);
    } else {
      if (currentTable.get(key) instanceof dev.plex.toml.Container) {
        errors.keyDuplicatesTable(key, line);
      } else {
        errors.duplicateKey(key, line != null ? line.get() : -1);
      }
    }
  }

  void startTableArray(dev.plex.toml.Identifier identifier, AtomicInteger line) {
    String tableName = identifier.getBareName();
    while (stack.size() > 1) {
      stack.pop();
    }

    dev.plex.toml.Keys.Key[] tableParts = dev.plex.toml.Keys.split(tableName);
    for (int i = 0; i < tableParts.length; i++) {
      String tablePart = tableParts[i].name;
      dev.plex.toml.Container currentContainer = stack.peek();

      if (currentContainer.get(tablePart) instanceof dev.plex.toml.Container.TableArray) {
        dev.plex.toml.Container.TableArray currentTableArray = (dev.plex.toml.Container.TableArray) currentContainer.get(tablePart);
        stack.push(currentTableArray);

        if (i == tableParts.length - 1) {
          currentTableArray.put(tablePart, new dev.plex.toml.Container.Table());
        }

        stack.push(currentTableArray.getCurrent());
        currentContainer = stack.peek();
      } else if (currentContainer.get(tablePart) instanceof dev.plex.toml.Container.Table && i < tableParts.length - 1) {
        dev.plex.toml.Container nextTable = (dev.plex.toml.Container) currentContainer.get(tablePart);
        stack.push(nextTable);
      } else if (currentContainer.accepts(tablePart)) {
        dev.plex.toml.Container newContainer = i == tableParts.length - 1 ? new dev.plex.toml.Container.TableArray() : new dev.plex.toml.Container.Table();
        addValue(tablePart, newContainer, line);
        stack.push(newContainer);

        if (newContainer instanceof dev.plex.toml.Container.TableArray) {
          stack.push(((dev.plex.toml.Container.TableArray) newContainer).getCurrent());
        }
      } else {
        errors.duplicateTable(tableName, line.get());
        break;
      }
    }
  }

  void startTables(dev.plex.toml.Identifier id, AtomicInteger line) {
    String tableName = id.getBareName();
    
    while (stack.size() > 1) {
      stack.pop();
    }

    dev.plex.toml.Keys.Key[] tableParts = dev.plex.toml.Keys.split(tableName);
    for (int i = 0; i < tableParts.length; i++) {
      String tablePart = tableParts[i].name;
      dev.plex.toml.Container currentContainer = stack.peek();
      if (currentContainer.get(tablePart) instanceof dev.plex.toml.Container) {
        dev.plex.toml.Container nextTable = (dev.plex.toml.Container) currentContainer.get(tablePart);
        if (i == tableParts.length - 1 && !nextTable.isImplicit()) {
          errors.duplicateTable(tableName, line.get());
          return;
        }
        stack.push(nextTable);
        if (stack.peek() instanceof dev.plex.toml.Container.TableArray) {
          stack.push(((dev.plex.toml.Container.TableArray) stack.peek()).getCurrent());
        }
      } else if (currentContainer.accepts(tablePart)) {
        startTable(tablePart, i < tableParts.length - 1, line);
      } else {
        errors.tableDuplicatesKey(tablePart, line);
        break;
      }
    }
  }

  /**
   * Warning: After this method has been called, this instance is no longer usable.
   */
  Map<String, Object> consume() {
    dev.plex.toml.Container values = stack.getLast();
    stack.clear();

    return ((dev.plex.toml.Container.Table) values).consume();
  }

  private dev.plex.toml.Container startTable(String tableName, AtomicInteger line) {
    dev.plex.toml.Container newTable = new dev.plex.toml.Container.Table(tableName);
    addValue(tableName, newTable, line);
    stack.push(newTable);

    return newTable;
  }

  private dev.plex.toml.Container startTable(String tableName, boolean implicit, AtomicInteger line) {
    dev.plex.toml.Container newTable = new dev.plex.toml.Container.Table(tableName, implicit);
    addValue(tableName, newTable, line);
    stack.push(newTable);

    return newTable;
  }
  
  private String getInlineTablePath(String key) {
    Iterator<dev.plex.toml.Container> descendingIterator = stack.descendingIterator();
    StringBuilder sb = new StringBuilder();
    
    while (descendingIterator.hasNext()) {
      dev.plex.toml.Container next = descendingIterator.next();
      if (next instanceof dev.plex.toml.Container.TableArray) {
        return null;
      }
      
      dev.plex.toml.Container.Table table = (dev.plex.toml.Container.Table) next;
      
      if (table.name == null) {
        break;
      }
      
      if (sb.length() > 0) {
        sb.append('.');
      }
      
      sb.append(table.name);
    }
    
    if (sb.length() > 0) {
      sb.append('.');
    }

    sb.append(key)
      .insert(0, '[')
      .append(']');
    
    return sb.toString();
  }
}