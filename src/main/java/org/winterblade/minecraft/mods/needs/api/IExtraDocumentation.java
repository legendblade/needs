package org.winterblade.minecraft.mods.needs.api;

import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.api.DocumentField;
import org.winterblade.minecraft.mods.needs.api.OptionalField;

import java.util.List;

/**
 * Marks the class as needing extra documentation beyond what is created by default.
 */
public interface IExtraDocumentation {
    /**
     * Return added documentation for the class itself; this will not be run through I18N
     * @return The text
     */
    String getAddedDocumentation();

    /**
     * Get extra fields to document; by default, all fields marked {@link Expose} will be included. You may
     * also instead use {@link OptionalField} or {@link DocumentField} annotations
     * @return The list of extra fields to document
     */
    List<String> getExtraDocumentedFieldNames();
}
