package dev.plex.api.impl;

import dev.plex.api.ApiCompatibility;

final class DefaultApiCompatibility implements ApiCompatibility
{
    private final int version;

    DefaultApiCompatibility(int version)
    {
        this.version = version;
    }

    @Override
    public int version()
    {
        return version;
    }
}
