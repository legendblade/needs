package org.winterblade.minecraft.mods.needs.config;

import org.winterblade.minecraft.mods.needs.api.needs.Need;

public class NeedDefinition {
    private final Need need;
    private final String content;
    private final byte[] digest;

    public NeedDefinition(final Need need, final String content, final byte[] digest) {
        this.need = need;
        this.content = content;
        this.digest = digest;
    }

    public Need getNeed() {
        return need;
    }

    public String getContent() {
        return content;
    }

    public byte[] getDigest() {
        return digest;
    }
}
