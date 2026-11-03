package dev.plex.punishment;

import dev.plex.punishment.PunishmentManager.IndefiniteBan;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

// Writes one labeled entry of indefbans.yml by replacing the lines of that entry alone, so every other byte of the file
// stays as it was. The entry is emitted from its edited nodes; comments are attached to nodes, so the entry's own
// comments, and the item nodes of values that remain, are kept.
final class IndefiniteBanYaml
{
    private final String contents;
    // The offset where each line starts, then the length of the contents. Lines break where SnakeYAML's reader counts
    // a line, so the line of a node mark indexes this list. A byte order mark is never part of a line.
    private final List<Integer> starts;
    private final String lineBreak;
    private final Yaml yaml;

    private IndefiniteBanYaml(String contents)
    {
        this.contents = contents;
        this.starts = lineStarts(contents, contents.startsWith("\uFEFF") ? 1 : 0);
        LineBreak lineBreak = contents.contains("\r\n") ? LineBreak.WIN : LineBreak.UNIX;
        this.lineBreak = lineBreak.getString();
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);
        // YamlConfiguration loads a file of any size.
        loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
        // The indentation of the shipped file.
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);
        dumperOptions.setIndent(2);
        dumperOptions.setIndicatorIndent(2);
        dumperOptions.setIndentWithIndicator(true);
        dumperOptions.setWidth(Integer.MAX_VALUE);
        dumperOptions.setLineBreak(lineBreak);
        // Other labels may alias an anchor of the entry. Only composed nodes carry anchors, each with its name.
        dumperOptions.setAnchorGenerator(Node::getAnchor);
        this.yaml = new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions,
                loaderOptions);
    }

    // Returns the new contents, or empty when the label's presence in the file is not the expected one. A null ban
    // deletes the entry.
    static Optional<String> write(String contents, String label, @Nullable IndefiniteBan ban, boolean existing)
    {
        IndefiniteBanYaml file = new IndefiniteBanYaml(contents);
        Node document = file.yaml.compose(new StringReader(contents));
        if (document != null && !(document instanceof MappingNode))
        {
            throw new IllegalStateException("The top level of indefbans.yml is not a map.");
        }
        // An empty file composes to no node, and a file holding only comments to an empty mapping.
        List<NodeTuple> entries = document == null ? List.of() : ((MappingNode) document).getValue();
        int index = labelIndex(entries, label);
        if ((index >= 0) != existing)
        {
            return Optional.empty();
        }
        String written;
        if (document instanceof MappingNode root && root.getFlowStyle() == FlowStyle.FLOW)
        {
            written = file.rewrite(root, index, label, ban);
        }
        else if (ban == null)
        {
            written = file.delete(entries.get(index));
        }
        else if (index < 0)
        {
            written = file.create(entries, label, ban);
        }
        else
        {
            written = file.update(entries.get(index), label, ban);
        }
        verify(contents, written, label, ban);
        return Optional.of(written);
    }

    // Replaces the lines from the label's line, which holds the "? " of an explicit key written on one line with it.
    // An explicit key whose "?" stands on a line of its own leaves that line, and verify rejects the write.
    private String update(NodeTuple entry, String label, IndefiniteBan ban)
    {
        Node key = entry.getKeyNode();
        int first = key.getStartMark().getLine();
        int last = lastLine(entry);
        fill(entryBlock(entry, label), ban);
        // The comments above the label are not part of the replaced lines.
        key.setBlockComments(null);
        String text = fit(indent(emit(entryDocument(key, entry.getValueNode())), indentation(first)),
                terminated(last));
        return contents.substring(0, starts.get(first)) + text + contents.substring(starts.get(last + 1));
    }

    // The comments above the label stay. One blank line is left where blank lines surrounded the entry.
    private String delete(NodeTuple entry)
    {
        int first = entry.getKeyNode().getStartMark().getLine();
        int next = lastLine(entry) + 1;
        int following = next;
        while (next < lineCount() && blank(next))
        {
            next++;
        }
        if (first > 0 && !blank(first - 1) && next > following)
        {
            next--;
        }
        return contents.substring(0, starts.get(first)) + contents.substring(starts.get(next));
    }

    // Inserts the entry after the last entry, before the comments that end the file, separated by one blank line.
    private String create(List<NodeTuple> entries, String label, IndefiniteBan ban)
    {
        MappingNode block = new MappingNode(Tag.MAP, new ArrayList<>(), FlowStyle.BLOCK);
        fill(block, ban);
        int column = entries.isEmpty() ? 0 : indentation(entries.getFirst().getKeyNode().getStartMark().getLine());
        String text = indent(emit(entryDocument(scalar(label, ScalarStyle.PLAIN), block)), column);
        int previous = entries.isEmpty() ? lineCount() - 1 : lastLine(entries.getLast());
        boolean terminated = previous < 0 || terminated(previous);
        StringBuilder inserted = new StringBuilder();
        if (!terminated)
        {
            inserted.append(lineBreak);
        }
        if (previous >= 0 && !blank(previous))
        {
            inserted.append(lineBreak);
        }
        inserted.append(fit(text, terminated));
        int at = starts.get(previous + 1);
        return contents.substring(0, at) + inserted + contents.substring(at);
    }

    // A flow mapping cannot take a block entry, so a file written as one is emitted whole.
    private String rewrite(MappingNode root, int index, String label, @Nullable IndefiniteBan ban)
    {
        if (ban == null)
        {
            root.getValue().remove(index);
        }
        else if (index < 0)
        {
            MappingNode block = new MappingNode(Tag.MAP, new ArrayList<>(), FlowStyle.BLOCK);
            fill(block, ban);
            add(root, scalar(label, ScalarStyle.PLAIN), block);
        }
        else
        {
            fill(entryBlock(root.getValue().get(index), label), ban);
        }
        return emit(root);
    }

    // Proves the written contents: the label reads as requested, and every other label reads exactly as it did.
    private static void verify(String previous, String written, String label, @Nullable IndefiniteBan ban)
    {
        YamlConfiguration before = load(previous);
        YamlConfiguration after = load(written);
        if (!others(before, label).equals(others(after, label)))
        {
            throw new IllegalStateException("Writing indefinite ban label '" + label + "' would change other labels.");
        }
        if (ban == null ? after.contains(label) : !reads(after, label, ban))
        {
            throw new IllegalStateException("Writing indefinite ban label '" + label
                    + "' would not store the requested values.");
        }
    }

    private static YamlConfiguration load(String contents)
    {
        YamlConfiguration file = new YamlConfiguration();
        try
        {
            file.loadFromString(contents);
        }
        catch (InvalidConfigurationException failure)
        {
            throw new IllegalStateException("Could not load indefbans.yml.", failure);
        }
        return file;
    }

    // Every value path outside the label. A section has identity equality, so only its presence is compared; its
    // values have paths of their own.
    private static Map<String, Object> others(YamlConfiguration file, String label)
    {
        Map<String, Object> values = new HashMap<>();
        file.getValues(true).forEach((path, value) ->
        {
            if (!path.equals(label) && !path.startsWith(label + "."))
            {
                values.put(path, value instanceof ConfigurationSection ? ConfigurationSection.class : value);
            }
        });
        return values;
    }

    // Reads the label the way PunishmentManager reads indefbans.yml.
    private static boolean reads(YamlConfiguration file, String label, IndefiniteBan ban)
    {
        return file.isConfigurationSection(label)
                && file.getString(label + ".reason", "").equals(ban.getReason())
                && file.getStringList(label + ".users").equals(ban.getUsernames())
                && file.getStringList(label + ".uuids").stream().map(UUID::fromString).toList().equals(ban.getUuids())
                && file.getStringList(label + ".ips").equals(ban.getIps());
    }

    // The last line holding the entry, trailing blank lines excluded. A block collection ends where the token after
    // it starts, past the comments and blank lines that follow it, so the end comes from the scalars, flow
    // collections, and inline comments inside it. The comments that follow belong to the next label, or end the file.
    // An alias composes to the anchored node, whose marks are at the anchor, so an alias list item on the entry's last
    // line is not counted; verify then rejects the write.
    private int lastLine(NodeTuple entry)
    {
        int first = entry.getKeyNode().getStartMark().getLine();
        int last = Math.max(first, Math.max(contentLine(entry.getKeyNode()), contentLine(entry.getValueNode())));
        while (last > first && blank(last))
        {
            last--;
        }
        return last;
    }

    private static int contentLine(Node node)
    {
        int line = -1;
        if (node.getInLineComments() != null)
        {
            for (CommentLine comment : node.getInLineComments())
            {
                line = Math.max(line, comment.getEndMark().getLine());
            }
        }
        if (node instanceof ScalarNode scalar)
        {
            line = Math.max(line, scalarLine(scalar));
        }
        else if (node instanceof CollectionNode<?> collection && collection.getFlowStyle() == FlowStyle.FLOW)
        {
            line = Math.max(line, collection.getEndMark().getLine());
        }
        else if (node instanceof SequenceNode sequence)
        {
            for (Node item : sequence.getValue())
            {
                line = Math.max(line, contentLine(item));
            }
        }
        else if (node instanceof MappingNode mapping)
        {
            for (NodeTuple tuple : mapping.getValue())
            {
                line = Math.max(line, Math.max(contentLine(tuple.getKeyNode()), contentLine(tuple.getValueNode())));
            }
        }
        return line;
    }

    // An absent value is marked where the token after it starts, which may be lines below, so it holds no line. A
    // block scalar ends after the line breaks that close it.
    private static int scalarLine(ScalarNode scalar)
    {
        if (scalar.getValue().isEmpty() && scalar.getScalarStyle() == ScalarStyle.PLAIN)
        {
            return -1;
        }
        Mark end = scalar.getEndMark();
        boolean closedByLineBreak = end.getColumn() == 0 && end.getLine() > scalar.getStartMark().getLine();
        return closedByLineBreak ? end.getLine() - 1 : end.getLine();
    }

    private int lineCount()
    {
        return starts.size() - 1;
    }

    private boolean blank(int line)
    {
        return contents.substring(starts.get(line), starts.get(line + 1)).isBlank();
    }

    // The column of the labels: the spaces before a label, or before the "?" of an explicit key.
    private int indentation(int line)
    {
        int at = starts.get(line);
        while (at < starts.get(line + 1) && contents.charAt(at) == ' ')
        {
            at++;
        }
        return at - starts.get(line);
    }

    private boolean terminated(int line)
    {
        return line < lineCount() - 1 || endsWithLineBreak(contents);
    }

    // Ends the emitted entry where the replaced lines end: at its last content line, with a line break unless the
    // file ends without one. Blank lines that close a kept block scalar stay in the file after it.
    private String fit(String text, boolean terminated)
    {
        List<Integer> lines = lineStarts(text, 0);
        int last = lines.size() - 2;
        while (last > 0 && text.substring(lines.get(last), lines.get(last + 1)).isBlank())
        {
            last--;
        }
        String fitted = text.substring(0, lines.get(last + 1));
        return terminated ? fitted : fitted.substring(0, fitted.length() - lineBreak.length());
    }

    // Indents the emitted entry to the column of the file's labels.
    private static String indent(String text, int column)
    {
        if (column == 0)
        {
            return text;
        }
        List<Integer> lines = lineStarts(text, 0);
        StringBuilder indented = new StringBuilder();
        for (int line = 0; line < lines.size() - 1; line++)
        {
            String content = text.substring(lines.get(line), lines.get(line + 1));
            if (!isLineBreak(content.charAt(0)))
            {
                indented.append(" ".repeat(column));
            }
            indented.append(content);
        }
        return indented.toString();
    }

    private static List<Integer> lineStarts(String text, int from)
    {
        List<Integer> starts = new ArrayList<>();
        starts.add(from);
        for (int i = from; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c == '\n' || c == '\u0085' || c == ' ' || c == ' '
                    || c == '\r' && (i + 1 == text.length() || text.charAt(i + 1) != '\n'))
            {
                starts.add(i + 1);
            }
        }
        if (starts.getLast() != text.length())
        {
            starts.add(text.length());
        }
        return starts;
    }

    private static boolean endsWithLineBreak(String text)
    {
        return !text.isEmpty() && isLineBreak(text.charAt(text.length() - 1));
    }

    static boolean isLineBreak(char c)
    {
        return c == '\n' || c == '\r' || c == '\u0085' || c == ' ' || c == ' ';
    }

    private static MappingNode entryDocument(Node key, Node value)
    {
        return new MappingNode(Tag.MAP, List.of(new NodeTuple(key, value)), FlowStyle.BLOCK);
    }

    private String emit(Node document)
    {
        StringWriter text = new StringWriter();
        yaml.serialize(document, text);
        return text.toString();
    }

    // Reads each label the way YamlConfiguration does, so a label such as 007 matches the label the view reports.
    private static int labelIndex(List<NodeTuple> entries, String label)
    {
        YamlConstructor constructor = new YamlConstructor(new LoaderOptions());
        for (int i = 0; i < entries.size(); i++)
        {
            if (String.valueOf(constructor.construct(entries.get(i).getKeyNode())).equals(label))
            {
                return i;
            }
        }
        return -1;
    }

    private static MappingNode entryBlock(NodeTuple entry, String label)
    {
        if (entry.getValueNode() instanceof MappingNode block)
        {
            return block;
        }
        throw new IllegalStateException("Indefinite ban label '" + label + "' must contain a ban block.");
    }

    private static void fill(MappingNode entry, IndefiniteBan ban)
    {
        setReason(entry, ban.getReason());
        setList(entry, "users", ban.getUsernames(), ScalarStyle.DOUBLE_QUOTED, UnaryOperator.identity());
        setList(entry, "uuids", ban.getUuids().stream().map(UUID::toString).toList(), ScalarStyle.PLAIN,
                uuid -> uuid.toLowerCase(Locale.ROOT));
        setList(entry, "ips", ban.getIps(), ScalarStyle.PLAIN, UnaryOperator.identity());
    }

    private static int keyIndex(List<NodeTuple> tuples, String key)
    {
        for (int i = 0; i < tuples.size(); i++)
        {
            if (tuples.get(i).getKeyNode() instanceof ScalarNode scalar && scalar.getValue().equals(key))
            {
                return i;
            }
        }
        return -1;
    }

    // An empty reason is the default, so it is not added to an entry that has none. The reason is kept when it reads
    // as requested the way PunishmentManager reads it.
    private static void setReason(MappingNode entry, String reason)
    {
        List<NodeTuple> tuples = entry.getValue();
        int index = keyIndex(tuples, "reason");
        if (index < 0)
        {
            if (!reason.isEmpty())
            {
                add(entry, scalar("reason", ScalarStyle.PLAIN), scalar(reason, ScalarStyle.DOUBLE_QUOTED));
            }
            return;
        }
        NodeTuple tuple = tuples.get(index);
        if (tuple.getValueNode() instanceof ScalarNode current && reason.equals(String.valueOf(
                Objects.requireNonNullElse(new YamlConstructor(new LoaderOptions()).construct(current), ""))))
        {
            return;
        }
        tuples.set(index, new NodeTuple(tuple.getKeyNode(),
                withComments(tuple.getValueNode(), scalar(reason, ScalarStyle.DOUBLE_QUOTED))));
    }

    // Keeps the item node of each remaining value, with its comments and quoting, in the order of the new values.
    private static void setList(MappingNode entry, String key, List<String> values, ScalarStyle style,
                                UnaryOperator<String> identity)
    {
        List<NodeTuple> tuples = entry.getValue();
        int index = keyIndex(tuples, key);
        if (index < 0)
        {
            if (!values.isEmpty())
            {
                add(entry, scalar(key, ScalarStyle.PLAIN), new SequenceNode(Tag.SEQ,
                        items(List.of(), values, style, identity), FlowStyle.BLOCK));
            }
            return;
        }
        NodeTuple tuple = tuples.get(index);
        if (tuple.getValueNode() instanceof SequenceNode sequence)
        {
            List<Node> items = items(sequence.getValue(), values, style, identity);
            if (sequence.getValue().isEmpty())
            {
                sequence.setFlowStyle(FlowStyle.BLOCK);
            }
            sequence.getValue().clear();
            sequence.getValue().addAll(items);
            return;
        }
        tuples.set(index, new NodeTuple(tuple.getKeyNode(), withComments(tuple.getValueNode(),
                new SequenceNode(Tag.SEQ, items(List.of(), values, style, identity), FlowStyle.BLOCK))));
    }

    // An item is matched by the string PunishmentManager reads from it, so a plain item that YAML reads as another
    // type, such as yes or 0123, is not kept for its text; a new item, quoted where YAML needs it, is written instead.
    private static List<Node> items(List<Node> existing, List<String> values, ScalarStyle style,
                                    UnaryOperator<String> identity)
    {
        YamlConstructor constructor = new YamlConstructor(new LoaderOptions());
        Map<String, Deque<Node>> unused = new HashMap<>();
        for (Node item : existing)
        {
            if (item instanceof ScalarNode && listString(constructor.construct(item)) instanceof String value)
            {
                unused.computeIfAbsent(identity.apply(value), read -> new ArrayDeque<>()).add(item);
            }
        }
        List<Node> items = new ArrayList<>(values.size());
        for (String value : values)
        {
            Deque<Node> same = unused.get(identity.apply(value));
            Node kept = same == null ? null : same.poll();
            items.add(kept == null ? scalar(value, style) : kept);
        }
        return items;
    }

    // The string MemorySection.getStringList reads from a list item, or null for an item it skips.
    @Nullable
    private static String listString(@Nullable Object item)
    {
        boolean read = item instanceof String || item instanceof Boolean || item instanceof Character
                || item instanceof Byte || item instanceof Short || item instanceof Integer || item instanceof Long
                || item instanceof Float || item instanceof Double;
        return read ? String.valueOf(item) : null;
    }

    // A collection that was empty was written in flow style ({} or []), so it switches to block style as it fills.
    private static void add(MappingNode mapping, Node key, Node value)
    {
        if (mapping.getValue().isEmpty())
        {
            mapping.setFlowStyle(FlowStyle.BLOCK);
        }
        mapping.getValue().add(new NodeTuple(key, value));
    }

    private static <T extends Node> T withComments(Node replaced, T replacement)
    {
        replacement.setBlockComments(replaced.getBlockComments());
        replacement.setInLineComments(replaced.getInLineComments());
        replacement.setEndComments(replaced.getEndComments());
        return replacement;
    }

    private static ScalarNode scalar(String value, ScalarStyle style)
    {
        return new ScalarNode(Tag.STR, value, null, null, style);
    }
}
