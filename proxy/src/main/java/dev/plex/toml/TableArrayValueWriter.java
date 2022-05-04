package dev.plex.toml;

import java.util.Collection;

import static dev.plex.toml.ValueWriters.WRITERS;

class TableArrayValueWriter extends dev.plex.toml.ArrayValueWriter
{
  static final dev.plex.toml.ValueWriter TABLE_ARRAY_VALUE_WRITER = new TableArrayValueWriter();

  @Override
  public boolean canWrite(Object value) {
    return isArrayish(value) && !isArrayOfPrimitive(value);
  }

  @Override
  public void write(Object from, dev.plex.toml.WriterContext context) {
    Collection<?> values = normalize(from);

    dev.plex.toml.WriterContext subContext = context.pushTableFromArray();

    for (Object value : values) {
      WRITERS.findWriterFor(value).write(value, subContext);
    }
  }

  private TableArrayValueWriter() {}

  @Override
  public String toString() {
    return "table-array";
  }
}
