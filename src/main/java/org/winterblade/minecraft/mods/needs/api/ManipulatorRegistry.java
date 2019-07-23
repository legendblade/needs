package org.winterblade.minecraft.mods.needs.api;

import org.winterblade.minecraft.mods.needs.manipulators.IManipulator;

import java.util.HashMap;
import java.util.Map;

public class ManipulatorRegistry {
    private static Map<String, Class<? extends IManipulator>> manipulators = new HashMap<>();

    public static void RegisterManipulator(String type, Class<? extends IManipulator> manipulator) {
        manipulators.put(type.toLowerCase(), manipulator);
    }

    public static Class<? extends IManipulator> GetType(String type) {
        return manipulators.get(type.toLowerCase());
    }
}
