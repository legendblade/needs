package org.winterblade.minecraft.mods.needs.documentation;

import java.util.List;
import java.util.Map;

public class DocumentationEntry {
    public String id;

    public String mod;

    public String description;

    public List<String> aliases;

    public List<Field> fields;

    public List<DocumentationEntry> children;

    public boolean isRoot;

    public static class Field {
        public String name;

        public String type;

        public String description;

        public boolean isOptional = false;

        public boolean isExpression = false;

        public boolean isArray = false;

        public boolean isMap = false;

        public Map<String,String> expressionVars;
    }
}
