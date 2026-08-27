package dev.plex.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * A vanish message sent between Plex servers and proxies.
 *
 * @param action requested action or reported state
 * @param playerId player UUID
 * @param silent whether the action must not send a join or leave message
 */
public record VanishBridgeMessage(Action action, UUID playerId, boolean silent)
{
    /** Plugin message channel used for vanish messages. */
    public static final String CHANNEL = "plex:vanish";
    private static final int PROTOCOL_VERSION = 1;

    /**
     * Creates and validates a vanish message.
     */
    public VanishBridgeMessage
    {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(playerId, "playerId");
    }

    /**
     * Creates a request for the player's current vanish state.
     *
     * @param playerId player UUID
     * @return query message
     */
    public static VanishBridgeMessage query(UUID playerId)
    {
        return new VanishBridgeMessage(Action.QUERY, playerId, false);
    }

    /**
     * Creates a message that reports the player's vanish state.
     *
     * @param playerId player UUID
     * @param vanished whether the player is vanished
     * @return state message
     */
    public static VanishBridgeMessage state(UUID playerId, boolean vanished)
    {
        return new VanishBridgeMessage(vanished ? Action.HIDDEN : Action.VISIBLE, playerId, false);
    }

    /**
     * Creates a request to hide a player.
     *
     * @param playerId player UUID
     * @param silent whether to hide the player without a leave message
     * @return hide message
     */
    public static VanishBridgeMessage hide(UUID playerId, boolean silent)
    {
        return new VanishBridgeMessage(Action.HIDE, playerId, silent);
    }

    /**
     * Creates a request to show a player.
     *
     * @param playerId player UUID
     * @param silent whether to show the player without a join message
     * @return show message
     */
    public static VanishBridgeMessage show(UUID playerId, boolean silent)
    {
        return new VanishBridgeMessage(Action.SHOW, playerId, silent);
    }

    /**
     * Encodes this message for the plugin message channel.
     *
     * @return encoded message
     */
    public byte[] encode()
    {
        try
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes))
            {
                output.writeByte(PROTOCOL_VERSION);
                output.writeByte(action.id());
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

    /**
     * Decodes a message from the plugin message channel.
     *
     * @param data encoded message
     * @return decoded message
     * @throws IOException if the message is invalid or uses an unsupported version
     */
    public static VanishBridgeMessage decode(byte[] data) throws IOException
    {
        Objects.requireNonNull(data, "data");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data)))
        {
            int version = input.readUnsignedByte();
            if (version != PROTOCOL_VERSION)
            {
                throw new IOException("Unsupported vanish bridge protocol version: " + version);
            }

            int actionId = input.readUnsignedByte();
            Action action = Action.fromId(actionId);
            if (action == null)
            {
                throw new IOException("Unknown vanish bridge action: " + actionId);
            }

            UUID playerId = new UUID(input.readLong(), input.readLong());
            boolean silent = input.readBoolean();
            if (input.available() != 0)
            {
                throw new IOException("Unexpected trailing vanish bridge data");
            }
            return new VanishBridgeMessage(action, playerId, silent);
        }
    }

    /**
     * Actions supported by the vanish message protocol.
     */
    public enum Action
    {
        /** Requests the player's current state. */
        QUERY(0),
        /** Reports that the player is visible. */
        VISIBLE(1),
        /** Reports that the player is hidden. */
        HIDDEN(2),
        /** Requests that the player be hidden. */
        HIDE(3),
        /** Requests that the player be shown. */
        SHOW(4);

        private final int id;

        Action(int id)
        {
            this.id = id;
        }

        private int id()
        {
            return id;
        }

        private static Action fromId(int id)
        {
            return switch (id)
            {
                case 0 -> QUERY;
                case 1 -> VISIBLE;
                case 2 -> HIDDEN;
                case 3 -> HIDE;
                case 4 -> SHOW;
                default -> null;
            };
        }
    }
}
