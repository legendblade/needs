package org.winterblade.minecraft.mods.needs.documentation;

import com.google.gson.annotations.Expose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.DocumentField;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.LevelActionRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DocumentationBuilder {
    private static final Path path = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "needs", "docs");
    private static final Pattern templateLiteralParser = Pattern.compile("##([a-z])##", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    public static void buildDocumentation() {
        try {
            if (!Files.exists(path)) Files.createDirectory(path);

            document("needs", NeedRegistry.INSTANCE);
//            document("mixins", MixinRegistry.INSTANCE, DocumentationBuilder::documentMixins);
//            document("manipulators", ManipulatorRegistry.INSTANCE, DocumentationBuilder::documentManipulators);
//            document("levelActions", LevelActionRegistry.INSTANCE, DocumentationBuilder::documentLevelActions);
        } catch (final IOException e) {
            NeedsMod.LOGGER.warn("Error creating documentation.", e);
        }
    }

    private static Map<String, List<TranslationEntry>> documentNeeds(Class<? extends Need> clazz) {
        return null;
    }

    private static String getTemplate(final String template) throws IOException {
        final ResourceLocation location = new ResourceLocation(NeedsMod.MODID, "templates/" + template + ".txt");
        final IResource resource = Minecraft.getInstance().getResourceManager().getResource(location);
        return IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());
    }

    private static <T> void document(final String documentType, final TypedRegistry<T> registry) throws IOException {
        final String docTag = NeedsMod.MODID + "." + documentType + ".";
        final Path subPath = DocumentationBuilder.path.resolve(documentType);
        if (!Files.exists(subPath)) Files.createDirectory(subPath);

        final String[] template = {getTemplate(documentType)};

        // Set up our regex
        final Matcher literals = templateLiteralParser.matcher(template[0]);
        final Set<String> literalTokens = new HashSet<>();

        while (literals.find()) {
            literalTokens.add(literals.group(0));
        }

        literalTokens.forEach((t) -> template[0] = template[0].replaceAll("##" + t + "##", I18n.format(docTag + t)));

        registry.getRegistry().entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue)).forEach((clazz, list) -> {
            final String className = clazz.getSimpleName();
            final Map<String, List<TranslationEntry>> translationKeys = new HashMap<>();

            translationKeys.put("description", Collections.singletonList(new TranslationEntry(docTag + className)));

            translationKeys.put("names", list
                    .stream()
                    .map(kv -> new TranslationEntry(docTag + "name").addParam(kv.getKey(), false))
                    .collect(Collectors.toList()));

            translationKeys.put("fields", Arrays.stream(clazz.getFields())
                .filter((f) -> f.getAnnotation(Expose.class) != null
                        || f.getAnnotation(DocumentField.class) != null
                        || f.getAnnotation(OptionalField.class) != null)
                .map((f) ->
                    new TranslationEntry(docTag + "fields")
                            .addParam(f.getName(), false)
                            .addParam("needs.documentation." + documentType + "." + className + "." + f.getName(), true)
                ).collect(Collectors.toList()));

            final String[] output = {template[0]};
            translationKeys.forEach((k, vs) -> {
                if (vs.isEmpty()) return;
                if (vs.size() == 1) {
                    output[0] = output[0].replaceAll("\\{\\{" + k + "}}", I18n.format(vs.get(0).getKey()));
                    return;
                }

                output[0] = output[0].replaceAll("\\{\\{" + k + "}}",
                        String.join("\n",
                                vs.stream()
                                        .map((tk) -> I18n.format(tk.getKey(), tk.getParams()))
                                        .collect(Collectors.toList())
                        )
                );
            });

            try (final PrintWriter wr = new PrintWriter(subPath.resolve(className + ".txt").toString())) {
                wr.println(output[0]);
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    private static class TranslationEntry {
        private final String key;
        private final List<String> params = new ArrayList<>();

        TranslationEntry(final String key) {
            this.key = key;
        }

        TranslationEntry addParam(final String param, final boolean translate) {
            params.add(translate ? I18n.format(param) : param);
            return this;
        }

        Object[] getParams() {
            return params.toArray(new String[0]);
        }

        String getKey() {
            return key;
        }
    }
}
