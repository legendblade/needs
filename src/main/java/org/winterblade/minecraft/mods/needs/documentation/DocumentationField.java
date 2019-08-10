package org.winterblade.minecraft.mods.needs.documentation;

import java.util.Map;

public class DocumentationField {
    public String name;

    public String type;

    public String description;

    public boolean isOptional = false;

    public boolean isExpression = false;

    public boolean isArray = false;

    public boolean isMap = false;

    public Map<String,String> expressionVars;

    public String defaultValue;

    public DocumentationBase listOrMapClass;
}
