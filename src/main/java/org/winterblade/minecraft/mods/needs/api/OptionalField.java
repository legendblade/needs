package org.winterblade.minecraft.mods.needs.api;

import java.lang.annotation.*;

/**
 * Specifies to the documentation builder that the field is optional
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OptionalField {
    String defaultValue();
}
