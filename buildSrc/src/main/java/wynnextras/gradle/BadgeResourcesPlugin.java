package wynnextras.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class BadgeResourcesPlugin implements Plugin<Project> {
    private static final String GENERATED_RESOURCES = "generated/resources/badges";
    private static final String BADGE_DEFINITION = "src/main/java/julianh06/wynnextras/features/badges/CustomBadgeIcon.java";
    private static final String BADGE_SOURCES = "src/main/badge-icons";

    @Override
    public void apply(Project project) {
        var outputDirectory = project.getLayout().getBuildDirectory().dir(GENERATED_RESOURCES);
        TaskProvider<GenerateBadgeTextures> generateTask = project.getTasks().register(
                "generateBadgeTextures",
                GenerateBadgeTextures.class,
                task -> {
                    task.getDefinitionFile().set(project.getLayout().getProjectDirectory().file(BADGE_DEFINITION));
                    task.getSourceDirectory().set(project.getLayout().getProjectDirectory().dir(BADGE_SOURCES));
                    task.getOutputDirectory().set(outputDirectory);
                }
        );

        project.getPlugins().withType(JavaPlugin.class, plugin -> {
            SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            generateTask.configure(task -> {
                task.dependsOn(main.getCompileJavaTaskName());
                task.getClassDirectories().from(main.getOutput().getClassesDirs());
            });
            main.getResources().srcDir(outputDirectory);
            project.getTasks().named(main.getProcessResourcesTaskName()).configure(task -> task.dependsOn(generateTask));
        });
    }

    public abstract static class GenerateBadgeTextures extends DefaultTask {
        private static final String DEFINITION_CLASS = "julianh06.wynnextras.features.badges.CustomBadgeIcon";

        @Classpath
        public abstract ConfigurableFileCollection getClassDirectories();

        @InputFile
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract RegularFileProperty getDefinitionFile();

        @InputDirectory
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract DirectoryProperty getSourceDirectory();

        @OutputDirectory
        public abstract DirectoryProperty getOutputDirectory();

        @TaskAction
        public void generate() throws Exception {
            Path outputRoot = getOutputDirectory().get().getAsFile().toPath();
            getProject().delete(outputRoot.toFile());

            URL[] classUrls = getClassDirectories().getFiles().stream()
                    .map(File::toURI)
                    .map(uri -> {
                        try {
                            return uri.toURL();
                        } catch (Exception exception) {
                            throw new GradleException("Could not resolve compiled badge definitions", exception);
                        }
                    })
                    .toArray(URL[]::new);

            Object[] badges;
            try (URLClassLoader classLoader = new URLClassLoader(classUrls, ClassLoader.getPlatformClassLoader())) {
                badges = classLoader.loadClass(DEFINITION_CLASS).getEnumConstants();
            }

            if (badges == null) {
                throw new GradleException(DEFINITION_CLASS + " must be an enum");
            }

            Set<String> seenIds = new HashSet<>();
            StringBuilder providers = new StringBuilder("{\n    \"providers\": [\n");
            boolean firstProvider = true;

            for (Object badge : badges) {
                String iconId = invokeString(badge, "id");
                String fileName = invokeString(badge, "fileName");
                int textureSize = invokeInt(badge, "textureSize");

                if (!iconId.matches("[a-z0-9_]+") || !seenIds.add(iconId)) {
                    throw new GradleException("Custom badge id '" + iconId + "' must be unique and contain only lowercase letters, numbers, and underscores");
                }
                if (textureSize < 16 || textureSize > 256 || Integer.bitCount(textureSize) != 1) {
                    throw new GradleException("Custom badge texture size for " + iconId + " must be a power of two between 16 and 256");
                }

                Path input = getSourceDirectory().file(fileName).get().getAsFile().toPath();
                BufferedImage source = ImageIO.read(input.toFile());
                if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
                    throw new GradleException("Custom badge texture " + input + " must be a readable image");
                }

                BufferedImage normalized = resize(source, textureSize);
                writeTexture(outputRoot, iconId + "_original.png", normalized);
                writeTexture(outputRoot, iconId + "_tinted.png", grayscale(normalized));

                firstProvider = appendProvider(providers, firstProvider, iconId, "tinted", invokeString(badge, "tintedGlyph"));
                firstProvider = appendProvider(providers, firstProvider, iconId, "original", invokeString(badge, "originalGlyph"));
            }

            providers.append("\n    ]\n}\n");
            Path fontFile = outputRoot.resolve("assets/wynnextras/font/badges.json");
            Files.createDirectories(fontFile.getParent());
            Files.writeString(fontFile, providers, StandardCharsets.UTF_8);
        }

        private static BufferedImage resize(BufferedImage source, int textureSize) {
            double scale = Math.min((double) textureSize / source.getWidth(), (double) textureSize / source.getHeight());
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            int offsetX = (textureSize - width) / 2;
            int offsetY = (textureSize - height) / 2;

            BufferedImage normalized = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = normalized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics.drawImage(source, offsetX, offsetY, width, height, null);
            graphics.dispose();
            return normalized;
        }

        private static BufferedImage grayscale(BufferedImage source) {
            BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    int red = (argb >>> 16) & 0xFF;
                    int green = (argb >>> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    int luminance = Math.round(0.2126f * red + 0.7152f * green + 0.0722f * blue);
                    result.setRGB(x, y, (alpha << 24) | (luminance << 16) | (luminance << 8) | luminance);
                }
            }
            return result;
        }

        private static void writeTexture(Path outputRoot, String fileName, BufferedImage image) throws Exception {
            Path output = outputRoot.resolve("assets/wynnextras/textures/font/badges").resolve(fileName);
            Files.createDirectories(output.getParent());
            if (!ImageIO.write(image, "png", output.toFile())) {
                throw new GradleException("Could not write custom badge texture " + output);
            }
        }

        private static boolean appendProvider(StringBuilder output, boolean first, String iconId, String variant, String glyph) {
            if (!first) output.append(",\n");
            output.append(("        {\n" +
                    "            \"type\": \"bitmap\",\n" +
                    "            \"file\": \"wynnextras:font/badges/%s_%s.png\",\n" +
                    "            \"height\": 9,\n" +
                    "            \"ascent\": 8,\n" +
                    "            \"chars\": [\n" +
                    "                \"%s\"\n" +
                    "            ]\n" +
                    "        }").formatted(iconId, variant, escapeJson(glyph)));
            return false;
        }

        private static String escapeJson(String value) {
            StringBuilder escaped = new StringBuilder();
            for (int index = 0; index < value.length(); index++) {
                char character = value.charAt(index);
                switch (character) {
                    case '"' -> escaped.append("\\\"");
                    case '\\' -> escaped.append("\\\\");
                    case '\b' -> escaped.append("\\b");
                    case '\f' -> escaped.append("\\f");
                    case '\n' -> escaped.append("\\n");
                    case '\r' -> escaped.append("\\r");
                    case '\t' -> escaped.append("\\t");
                    default -> {
                        if (character < 0x20 || character > 0x7E) {
                            escaped.append(String.format("\\u%04x", (int) character));
                        } else {
                            escaped.append(character);
                        }
                    }
                }
            }
            return escaped.toString();
        }

        private static String invokeString(Object target, String methodName) throws Exception {
            Method method = target.getClass().getMethod(methodName);
            return (String) method.invoke(target);
        }

        private static int invokeInt(Object target, String methodName) throws Exception {
            Method method = target.getClass().getMethod(methodName);
            return (int) method.invoke(target);
        }
    }
}