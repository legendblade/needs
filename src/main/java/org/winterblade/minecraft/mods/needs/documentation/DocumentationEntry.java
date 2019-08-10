package org.winterblade.minecraft.mods.needs.documentation;

import java.util.List;

public class DocumentationEntry extends DocumentationBase {
    public String mod;

    public List<String> aliases;

    public List<DocumentationEntry> children;

    public boolean isRoot;

}
