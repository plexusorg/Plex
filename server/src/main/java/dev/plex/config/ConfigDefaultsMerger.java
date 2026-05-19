package dev.plex.config;

import dev.plex.util.PlexLog;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ConfigDefaultsMerger
{
    private static final Pattern KEY = Pattern.compile("^(\\s*)([A-Za-z0-9_-]+):(?:\\s+.*)?$");

    private ConfigDefaultsMerger()
    {
    }

    static Result merge(File file, InputStream defaultsStream, String displayName) throws IOException, InvalidConfigurationException
    {
        if (defaultsStream == null)
        {
            PlexLog.warn("Unable to merge defaults into " + displayName + " because no default resource exists.");
            return new Result(load(file), List.of(), false);
        }

        String defaultsText = new String(defaultsStream.readAllBytes(), StandardCharsets.UTF_8);
        List<String> defaultLines = splitLines(defaultsText);
        List<String> currentLines = splitLines(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        Map<String, Entry> defaults = parse(defaultLines);
        Map<String, Entry> current = parse(currentLines);

        List<Insertion> insertions = new ArrayList<>();
        Set<String> coveredByMissingParent = new HashSet<>();

        int order = 0;
        for (Entry entry : defaults.values())
        {
            if (current.containsKey(entry.path) || isCovered(entry.path, coveredByMissingParent))
            {
                order++;
                continue;
            }

            String parent = parent(entry.path);
            if (parent != null && !current.containsKey(parent))
            {
                order++;
                continue;
            }

            insertions.add(new Insertion(findInsertionIndex(entry, defaults, current, currentLines.size()), order, entry.path, defaultLines.subList(entry.start, entry.end)));
            coveredByMissingParent.add(entry.path);
            order++;
        }

        if (insertions.isEmpty())
        {
            return new Result(load(file), List.of(), false);
        }

        applyInsertions(currentLines, insertions);
        Files.writeString(file.toPath(), String.join(System.lineSeparator(), currentLines) + System.lineSeparator(), StandardCharsets.UTF_8);
        return new Result(load(file), insertions.stream().map(Insertion::path).toList(), true);
    }

    private static int findInsertionIndex(Entry missing, Map<String, Entry> defaults, Map<String, Entry> current, int fallback)
    {
        String parent = parent(missing.path);
        for (Entry candidate : defaults.values())
        {
            if (candidate.start <= missing.start || !sameParent(missing.path, candidate.path))
            {
                continue;
            }
            Entry existingCandidate = current.get(candidate.path);
            if (existingCandidate != null)
            {
                return existingCandidate.start;
            }
        }

        if (parent != null && current.containsKey(parent))
        {
            return current.get(parent).end;
        }
        return fallback;
    }

    private static void applyInsertions(List<String> currentLines, List<Insertion> insertions)
    {
        insertions.sort(Comparator.comparingInt(Insertion::index).reversed().thenComparing(Comparator.comparingInt(Insertion::order).reversed()));
        for (Insertion insertion : insertions)
        {
            List<String> block = new ArrayList<>(insertion.lines);
            if (insertion.index > 0 && !currentLines.get(insertion.index - 1).isBlank() && !block.get(0).isBlank())
            {
                block.add(0, "");
            }
            if (insertion.index < currentLines.size() && !currentLines.get(insertion.index).isBlank() && !block.get(block.size() - 1).isBlank())
            {
                block.add("");
            }
            currentLines.addAll(insertion.index, block);
        }
    }

    private static Map<String, Entry> parse(List<String> lines)
    {
        Map<String, Entry> entries = new LinkedHashMap<>();
        List<StackEntry> stack = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++)
        {
            Matcher matcher = KEY.matcher(lines.get(i));
            if (!matcher.matches() || lines.get(i).trim().startsWith("#"))
            {
                continue;
            }

            int indent = matcher.group(1).length();
            String key = matcher.group(2);
            while (!stack.isEmpty() && stack.get(stack.size() - 1).indent >= indent)
            {
                stack.remove(stack.size() - 1);
            }

            String path = stack.isEmpty() ? key : stack.get(stack.size() - 1).path + "." + key;
            entries.put(path, new Entry(path, blockStart(lines, i), blockEnd(lines, i, indent), indent));
            stack.add(new StackEntry(path, indent));
        }
        return entries;
    }

    private static int blockStart(List<String> lines, int keyLine)
    {
        int start = keyLine;
        while (start > 0)
        {
            String previous = lines.get(start - 1).trim();
            if (!previous.isBlank() && !previous.startsWith("#"))
            {
                break;
            }
            start--;
        }
        return start;
    }

    private static int blockEnd(List<String> lines, int keyLine, int indent)
    {
        for (int i = keyLine + 1; i < lines.size(); i++)
        {
            Matcher matcher = KEY.matcher(lines.get(i));
            if (matcher.matches() && matcher.group(1).length() <= indent)
            {
                return blockStart(lines, i);
            }
        }
        return lines.size();
    }

    private static boolean sameParent(String first, String second)
    {
        String firstParent = parent(first);
        String secondParent = parent(second);
        return firstParent == null ? secondParent == null : firstParent.equals(secondParent);
    }

    private static boolean isCovered(String path, Set<String> covered)
    {
        return covered.stream().anyMatch(parent -> path.startsWith(parent + "."));
    }

    private static String parent(String path)
    {
        int index = path.lastIndexOf('.');
        return index == -1 ? null : path.substring(0, index);
    }

    private static List<String> splitLines(String text)
    {
        return new ArrayList<>(text.lines().toList());
    }

    static YamlConfiguration load(File file) throws IOException, InvalidConfigurationException
    {
        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        config.load(file);
        return config;
    }

    private record StackEntry(String path, int indent)
    {
    }

    private record Entry(String path, int start, int end, int indent)
    {
    }

    private record Insertion(int index, int order, String path, List<String> lines)
    {
    }

    record Result(YamlConfiguration config, List<String> addedKeys, boolean changed)
    {
    }
}
