package dev.plex.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public record VanishBridgeMessage(Action action, UUID playerId, boolean silent)
{
    public static final String CHANNEL = "plex:vanish";
    private static final int PROTOCOL_VERSION = 1;

    public static VanishBridgeMessage query(UUID playerId)
    {
        return new VanishBridgeMessage(Action.QUERY, playerId, false);
    }

    public static VanishBridgeMessage state(UUID playerId, boolean vanished)
    {
        return new VanishBridgeMessage(vanished ? Action.HIDDEN : Action.VISIBLE, playerId, false);
    }

    public static VanishBridgeMessage hide(UUID playerId, boolean silent)
    {
        return new VanishBridgeMessage(Action.HIDE, playerId, silent);
    }

    public static VanishBridgeMessage show(UUID playerId, boolean silent)
    {
        return new VanishBridgeMessage(Action.SHOW, playerId, silent);
    }

    public byte[] encode()
    {
        try
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes))
            {
                output.writeByte(PROTOCOL_VERSION);
                output.writeByte(action.ordinal());
                output.writeLong(playerId.getMostSignificantBits());
                output.writeLong(playerId.getLeastSignificantBits());
                output.writeBoolean(silent);
            }
            return bytes.toByteArray();
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to encode vanish bridge message", ex);
        }
    }

    public static VanishBridgeMessage decode(byte[] data) throws IOException
    {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data)))
        {
            int version = input.readUnsignedByte();
            if (version != PROTOCOL_VERSION)
            {
                throw new IOException("Unsupported vanish bridge protocol version: " + version);
            }

            int actionId = input.readUnsignedByte();
            Action[] actions = Action.values();
            if (actionId >= actions.length)
            {
                throw new IOException("Unknown vanish bridge action: " + actionId);
            }

            UUID playerId = new UUID(input.readLong(), input.readLong());
            boolean silent = input.readBoolean();
            if (input.available() != 0)
            {
                throw new IOException("Unexpected trailing vanish bridge data");
            }
            return new VanishBridgeMessage(actions[actionId], playerId, silent);
        }
    }

    public enum Action
    {
        QUERY,
        VISIBLE,
        HIDDEN,
        HIDE,
        SHOW
    }
}
