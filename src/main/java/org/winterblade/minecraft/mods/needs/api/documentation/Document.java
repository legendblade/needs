package org.winterblade.minecraft.mods.needs.api.documentation;

import com.google.gson.annotations.Expose;

import java.lang.annotation.*;

/**
 * Specifies to the documentation builder that the field should be added to the documentation; this is an alternative
 * to {@link Expose}, which will also add it.
 *
 * If you specify a description here, it will output that into the documentation; when used for this purpose, it can
 * also be added to a class.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Document {
    /**
     * A description to use for this field
     * @return The field's description
     */
    String description() default "";

    /**
     * The type of the list elements or map values in this variable; not necessary for regular fields
     * @return The type of a list or the values in a map; the key for a map will always be string
     */
    Class<?> type() default Object.class;
}
