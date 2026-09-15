package dev.plex.punishment;

import com.google.common.net.InetAddresses;
import dev.plex.punishment.admission.BanDecisionService;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IndefiniteIpRange
{
    private final byte[] network;
    private final byte[] mask;
    private final String notation;

    private IndefiniteIpRange(byte[] network, byte[] mask, String notation)
    {
        this.network = network;
        this.mask = mask;
        this.notation = notation;
    }

    public static IndefiniteIpRange parse(String input)
    {
        String value = input.trim();
        if (value.contains("*")) return parseWildcard(value);
        String[] parts = value.split("/", -1);
        if (parts.length > 2) throw new IllegalArgumentException("Invalid IP range: " + input);
        byte[] address = InetAddresses.forString(BanDecisionService.canonicalIp(parts[0])).getAddress();
        int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : address.length == 16 ? 64 : 32;
        if (prefix < 0 || prefix > address.length * 8)
        {
            throw new IllegalArgumentException("Invalid IP prefix: " + input);
        }
        byte[] mask = new byte[address.length];
        for (int bit = 0; bit < prefix; bit++)
        {
            mask[bit / 8] |= (byte) (1 << (7 - bit % 8));
        }
        for (int i = 0; i < address.length; i++) address[i] &= mask[i];
        try
        {
            String notation = InetAddresses.toAddrString(InetAddress.getByAddress(address)) + "/" + prefix;
            return new IndefiniteIpRange(address, mask, notation);
        }
        catch (UnknownHostException exception)
        {
            throw new IllegalStateException(exception);
        }
    }

    private static IndefiniteIpRange parseWildcard(String value)
    {
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) throw new IllegalArgumentException("Use four IPv4 octets: " + value);
        byte[] mask = new byte[4];
        for (int i = 0; i < octets.length; i++)
        {
            if (octets[i].equals("*")) octets[i] = "0";
            else mask[i] = (byte) 0xff;
        }
        byte[] address = InetAddresses.forString(String.join(".", octets)).getAddress();
        if (address.length != 4) throw new IllegalArgumentException("Use IPv4 octets: " + value);
        return new IndefiniteIpRange(address, mask, value);
    }

    public boolean contains(byte[] address)
    {
        if (address.length != network.length) return false;
        for (int i = 0; i < network.length; i++)
        {
            if (((network[i] ^ address[i]) & mask[i]) != 0) return false;
        }
        return true;
    }

    public boolean contains(IndefiniteIpRange other)
    {
        if (!contains(other.network)) return false;
        for (int i = 0; i < mask.length; i++)
        {
            if ((mask[i] & ~other.mask[i]) != 0) return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return notation;
    }
}
