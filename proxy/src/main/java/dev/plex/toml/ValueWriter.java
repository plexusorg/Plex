package dev.plex.toml;

interface ValueWriter {
  boolean canWrite(Object value);

  void write(Object value, dev.plex.toml.WriterContext context);

  boolean isPrimitiveType();
}
