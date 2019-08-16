package org.winterblade.minecraft.mods.needs.documentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentationEntry extends DocumentationBase {
    public String mod;

    public List<String> aliases;

    public List<DocumentationEntry> children;

    public boolean isRoot;

    public Map<String, Object> extras = new HashMap<>();
}
