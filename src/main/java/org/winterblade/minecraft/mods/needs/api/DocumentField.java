package org.winterblade.minecraft.mods.needs.api;

import com.google.gson.annotations.Expose;

import java.lang.annotation.*;

/**
 * Specifies to the documentation builder that the field should be added to the documentation; this is an alternative
 * to {@link Expose}, which will also add it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DocumentField {
}
