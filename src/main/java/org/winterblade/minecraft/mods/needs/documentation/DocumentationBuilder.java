package org.winterblade.minecraft.mods.needs.documentation;

import com.google.common.collect.RangeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.actions.ILevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.LevelAction;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;
import org.winterblade.minecraft.mods.needs.api.mixins.IMixin;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.LevelActionRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.api.mixins.BaseMixin;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;
import org.winterblade.minecraft.mods.needs.util.blocks.IBlockPredicate;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;

import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentationBuilder {
    private static final GsonBuilder builder = new GsonBuilder();
    private static final Path path = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "needs");
    private static Gson gson;

    public static void buildDocumentation() {
        try {
            if (!Files.exists(path)) Files.createDirectory(path);

            //final String[] template = {getTemplate(documentType)};

            final DocumentationRoot root = new DocumentationRoot();
            root.needs = document("needs", NeedRegistry.INSTANCE, Need.class, null);
            root.mixins = document("mixins", MixinRegistry.INSTANCE, BaseMixin.class, IMixin.class);
            root.manipulators = document("manipulators", ManipulatorRegistry.INSTANCE, IManipulator.class, IManipulator.class,
                    DocumentationBuilder::tagManipulatorExtras);
            root.actions = document("levelActions", LevelActionRegistry.INSTANCE, LevelAction.class, ILevelAction.class);

            // If we're in dev, output the docs to our directory for upload
            final String json = getGson().toJson(root, DocumentationRoot.class);
            if (System.getProperty("devBuild") != null) {
                try (final PrintWriter wr = new PrintWriter(path.resolve("../../../docs-app/public/data.json").toString())) {
                    wr.println(json);
                } catch (final FileNotFoundException e) {
                    e.printStackTrace();
                }
                return;
            }

            final StringBuilder result = new StringBuilder();
            final HttpURLConnection conn = (HttpURLConnection) new URL("https://legendblade.github.io/needs/index.html").openConnection();
            conn.setRequestMethod("GET");

            try(final BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }

            final String output = result.toString().replaceAll("localData:\"###LOCALDATA###\"", "localData:" + json);
            try (final PrintWriter wr = new PrintWriter(path.resolve("readme.html").toString())) {
                wr.println(output);
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (final IOException e) {
            NeedsMod.LOGGER.warn("Error creating documentation.", e);
        }
    }

    private static <T> void tagManipulatorExtras(final Class<? extends T> clazz, final DocumentationEntry entry) {
        entry.extras.put("isCondition", ICondition.class.isAssignableFrom(clazz));
        entry.extras.put("isTrigger", ITrigger.class.isAssignableFrom(clazz));
    }

    private static <T> List<DocumentationEntry> document(
            final String documentType,
            final TypedRegistry<T> registry,
            final Class<? extends T> root,
            final Class<T> intf) {
        return document(documentType, registry, root, intf, (c,d) -> {});
    }

    private static <T> List<DocumentationEntry> document(
            final String documentType,
            final TypedRegistry<T> registry,
            final Class<? extends T> root,
            final Class<T> intf,
            final BiConsumer<Class<? extends T>,DocumentationEntry> extras) {
        final String docTag = NeedsMod.MODID + "." + documentType + ".";

        final Map<Class<? extends T>, DocumentationEntry> entries = new HashMap<>();

        registry.getRegistrants().forEach((id, clazz) -> {
            final DocumentationEntry entry = getEntry(registry, root, intf, id, clazz, entries, docTag);
            extras.accept(clazz, entry);
        });

        return entries.values()
                .stream()
                .peek((f) -> f.children.sort((a, b) -> a.id.compareTo(b.id)))
                .filter((f) -> f.isRoot)
                .sorted(Comparator.comparing(a -> a.id))
                .collect(Collectors.toList());
    }

    /**
     * Adds a single documentation entry, to the entry list before recursively calling itself all the way up the chain
     * @param registry The registry to pull entries from
     * @param root     The root class
     * @param intf     The root interface, if the root class should not be checked
     * @param id       The ID of the given entry
     * @param clazz    The class of the entry
     * @param entries  The entry list
     * @param docTag   The document tag
     * @param <T>      The type of classes to process
     */
    private static <T> DocumentationEntry getEntry(
            final TypedRegistry<T> registry,
            final Class<? extends T> root,
            final Class<? extends T> intf,
            final ResourceLocation id,
            final Class<? extends T> clazz,
            final Map<Class<? extends T>, DocumentationEntry> entries,
            final String docTag) {
        // If we've already gotten here from elsewhere, set the ID:
        if (id != null && entries.containsKey(clazz)) {
            final DocumentationEntry entry = entries.get(clazz);
            entry.id = id.getPath();
            entry.mod = getMod(id);
            return entry;
        }

        // Create this entry:
        final DocumentationEntry entry = new DocumentationEntry();
        if(id != null) {
            entry.id = id.getPath();
            entry.mod = getMod(id);
        } else {
            entry.id = clazz.getSimpleName();
        }

        // Check if we have documentation on the class:
        final Document anno = clazz.getAnnotation(Document.class);
        if (anno != null && !anno.description().isEmpty()) {
            entry.description = anno.description();
        } else {
            final String key = docTag + entry.id;
            if (I18n.hasKey(key)) {
                entry.description = I18n.format(key);
            }
        }

        // Check for aliases
        entry.aliases = registry.getRegistry().entrySet()
                .stream()
                .filter((kv) -> kv.getValue().getB().equals(clazz))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Populate fields
        entry.fields = Arrays.stream(clazz.getDeclaredFields())
                .filter((f) -> f.getAnnotation(Expose.class) != null
                        || f.getAnnotation(OptionalField.class) != null
                        || f.getAnnotation(Document.class) != null)
                .map(documentField(clazz, docTag, entry))
                .collect(Collectors.toList());

        // Add child storage and shove the whole thing into the class map:
        entry.children = new ArrayList<>();
        entries.put(clazz, entry);

        // Check if we've reached the top:
        final Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || (intf != null && Arrays.stream(clazz.getInterfaces()).anyMatch((i) -> i.equals(intf)))
                || superclass.equals(root) || !root.isAssignableFrom(superclass)) {
            entry.isRoot = true;
            return entry;
        }

        // We have actually checked this.
        @SuppressWarnings("unchecked") final Class<? extends T> sc = (Class<? extends T>) superclass;
        if (!entries.containsKey(sc)) {
            getEntry(registry, root, intf, null, sc, entries, docTag);
        }

        // This should always be true, but just in case?
        if (entries.containsKey(sc)) entries.get(sc).children.add(entry);
        return entry;
    }

    /**
     * Attempts to locate the mod by its namespace
     * @param id The ID of the resource to locate by
     * @return The mod name, or its namespace if it couldn't be found.
     */
    private static String getMod(final ResourceLocation id) {
        return ModList
            .get()
                .getMods()
                .stream()
                .filter((m) -> m.getModId().equals(id.getNamespace()) || m.getNamespace().equals(id.getNamespace()))
                .findFirst()
                .map(ModInfo::getDisplayName)
                .orElse(id.getNamespace());
    }

    /**
     * Generate a function to process fields
     * @param clazz  The parent class
     * @param docTag The document tag
     * @param entry  The documentation entry
     * @param <T>    The type of the parent class
     * @return  A function to process the fields with
     */
    private static <T> Function<Field, DocumentationField> documentField(final Class<? extends T> clazz, final String docTag, final DocumentationBase entry) {
        return (f) -> {
            final DocumentationField field = new DocumentationField();

            field.name = f.getName();

            // Check if we have documentation on the field:
            final Document fieldAnno = f.getAnnotation(Document.class);
            if (fieldAnno != null && !fieldAnno.description().isEmpty()) {
                field.description = fieldAnno.description();
            } else {
                String key = docTag + entry.id + "." + field.name;
                if (I18n.hasKey(key)) {
                    field.description = I18n.format(key);
                } else {
                    key = docTag + field.name;
                    if (I18n.hasKey(key)) {
                        field.description = I18n.format(key);
                    }
                }
            }

            // Figure out our type values:
            Class<?> fieldType = f.getType();
            if(ExpressionContext.class.isAssignableFrom(fieldType)) {
                field.isExpression = true;
                try {
                    final ExpressionContext ctx = (ExpressionContext) fieldType.getConstructor().newInstance();
                    field.expressionVars = new HashMap<>(ctx.getElementDocumentation());
                    field.expressionVars.put("need(Name)", "The current value of the given need by its name.");
                } catch (
                        final Exception | NoClassDefFoundError e) {
                    NeedsMod.LOGGER.warn("Unable to create new expression context " + fieldType.getCanonicalName());
                    field.expressionVars = new HashMap<>();
                }
            }

            final OptionalField opt = f.getAnnotation(OptionalField.class);
            if (opt != null) {
                field.isOptional = true;
                field.defaultValue = opt.defaultValue();
            }


            if (fieldType.isAssignableFrom(Map.class) || fieldType.isAssignableFrom(RangeMap.class)) {
                field.isMap = true;
                fieldType = fieldAnno != null ? fieldAnno.type() : Object.class;
                if (fieldType == Object.class) {
                    field.type = "Map";
                    NeedsMod.LOGGER.warn(clazz.getCanonicalName() + "#" + f.getName() + " is a map without type information");
                    return field;
                }
            }

            if (fieldType.isAssignableFrom(List.class)) {
                field.isArray = true;
                fieldType = fieldAnno != null ? fieldAnno.type() : Object.class;
                if (fieldType == Object.class) {
                    field.type = "Array";
                    NeedsMod.LOGGER.warn(clazz.getCanonicalName() + "#" + f.getName() + " is a list without type information");
                    return field;
                }
            }

            // Raw types:
            if (fieldType == double.class || Double.class.isAssignableFrom(fieldType)) field.type = "Number";
            else if (fieldType == float.class || Float.class.isAssignableFrom(fieldType)) field.type = "Number";
            else if (fieldType == int.class || Integer.class.isAssignableFrom(fieldType)) field.type = "Number";
            else if (fieldType == short.class || Short.class.isAssignableFrom(fieldType)) field.type = "Number";
            else if (fieldType == long.class || Long.class.isAssignableFrom(fieldType)) field.type = "Number";
            else if (fieldType == boolean.class || Boolean.class.isAssignableFrom(fieldType)) field.type = "Boolean";

            // Class types:
            else if (String.class.isAssignableFrom(fieldType)) field.type = "String";
            else if (ExpressionContext.class.isAssignableFrom(fieldType)) field.type = "Number";

            else if (Need.class.isAssignableFrom(fieldType)) field.type = "Need";
            else if (LazyNeed.class.isAssignableFrom(fieldType)) field.type = "Need";

            else if (IManipulator.class.isAssignableFrom(fieldType)) field.type = "Manipulator";
            else if (IMixin.class.isAssignableFrom(fieldType)) field.type = "Mixin";
            else if (ILevelAction.class.isAssignableFrom(fieldType)) field.type = "Action";

            else if (IBlockPredicate.class.isAssignableFrom(fieldType)) field.type = "Block/Tag";
            else if (IIngredient.class.isAssignableFrom(fieldType)) field.type = "Item/Tag";

            else if (ICondition.class.isAssignableFrom(fieldType)) field.type = "Condition";
            else if (ITrigger.class.isAssignableFrom(fieldType)) field.type = "Trigger";

            // Give up:
            else field.type = fieldType.getSimpleName();

            final DocumentationBase subclass = new DocumentationBase();
            subclass.id = entry.id + "." + field.name;

            final Document subdoc = fieldType.getAnnotation(Document.class);
            final List<DocumentationField> fieldList = Arrays.stream(fieldType.getDeclaredFields())
                    .filter((f2) -> f2.getAnnotation(Expose.class) != null
                            || f2.getAnnotation(OptionalField.class) != null
                            || f2.getAnnotation(Document.class) != null)
                    .map(documentField(fieldType, docTag, subclass))
                    .collect(Collectors.toList());

            if (fieldList.isEmpty() && subdoc == null) return field;

            subclass.description = subdoc != null ? subdoc.description() : "";
            subclass.fields = fieldList;
            field.listOrMapClass = subclass;

            return field;
        };
    }

    private static Gson getGson() {
        if (gson == null) gson = builder.create();

        return gson;
    }
}
