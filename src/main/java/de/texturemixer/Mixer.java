package de.texturemixer;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class Mixer {

    public static final String OUTPUT_NAME = "MixedPack";

    /** Eine Kategorie, z. B. "Hotbar". Der Test bekommt den Dateipfad in Kleinbuchstaben. */
    public record Category(String name, Predicate<String> test) {}

    // WICHTIG: Reihenfolge! Spezielle Kategorien zuerst, allgemeine zuletzt.
    // Wenn eine Datei zu mehreren Kategorien passt, gewinnt die erste.
    public static final List<Category> CATEGORIES = List.of(
            new Category("Rüstung", p -> p.contains("/textures/models/armor/")
                    || p.contains("/textures/entity/equipment/")
                    || p.contains("/textures/trims/")
                    || (itemFile(p) && isArmorName(base(p)))),
            new Category("Werkzeuge/Waffen", p -> itemFile(p) && isToolName(base(p))),
            new Category("Hotbar", p -> p.contains("/textures/gui/sprites/hud/hotbar")
                    || p.endsWith("/textures/gui/widgets.png")),
            new Category("HUD (Herzen, Hunger)", p -> p.contains("/textures/gui/sprites/hud/")
                    || p.endsWith("/textures/gui/icons.png")),
            new Category("Schrift", p -> p.contains("/font/")),
            new Category("Blöcke", p -> p.contains("/textures/block/")
                    || p.contains("/models/block/")
                    || p.contains("/blockstates/")),
            new Category("Items", Mixer::itemFile),
            new Category("Menüs/GUI", p -> p.contains("/textures/gui/")),
            new Category("Mobs/Tiere", p -> p.contains("/textures/entity/")
                    || p.contains("/models/entity/")),
            new Category("Himmel/Farben", p -> p.contains("/textures/environment/")
                    || p.contains("/textures/colormap/")),
            new Category("Partikel", p -> p.contains("/textures/particle/")),
            new Category("Sounds", p -> p.endsWith("/sounds.json") || p.contains("/sounds/"))
    );

    private static boolean itemFile(String p) {
        return p.contains("/textures/item/") || p.contains("/models/item/") || p.contains("/items/");
    }

    private static String base(String p) {
        String n = p.substring(p.lastIndexOf('/') + 1);
        int d = n.indexOf('.');
        return d < 0 ? n : n.substring(0, d);
    }

    private static boolean isArmorName(String b) {
        return b.endsWith("_helmet") || b.endsWith("_chestplate")
                || b.endsWith("_leggings") || b.endsWith("_boots")
                || b.equals("elytra") || b.endsWith("_horse_armor") || b.endsWith("_armor_trim");
    }

    private static boolean isToolName(String b) {
        return b.endsWith("_sword") || b.endsWith("_pickaxe") || b.endsWith("_axe")
                || b.endsWith("_shovel") || b.endsWith("_hoe")
                || b.equals("bow") || b.startsWith("bow_")
                || b.equals("crossbow") || b.startsWith("crossbow_")
                || b.equals("trident") || b.equals("shield") || b.equals("shears")
                || b.equals("fishing_rod") || b.startsWith("fishing_rod_") || b.equals("mace");
    }

    public static Path packsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
    }

    /** Alle installierten Packs (Ordner oder .zip), ohne das Ausgabe-Pack. */
    public static List<Path> listPacks() throws IOException {
        Path dir = packsDir();
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) return result;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> !p.getFileName().toString().equals(OUTPUT_NAME))
                    .filter(p -> Files.isDirectory(p) || p.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(result::add);
        }
        return result;
    }

    /** Baut resourcepacks/MixedPack aus der Auswahl (Kategorie -> Pack). */
    public static void mix(Map<Category, Path> selection) throws IOException {
        Path out = packsDir().resolve(OUTPUT_NAME);
        deleteRecursively(out);
        Files.createDirectories(out);

        Set<String> written = new HashSet<>();
        Map<Path, PackSource> open = new HashMap<>();
        try {
            for (Category cat : CATEGORIES) {
                Path packPath = selection.get(cat);
                if (packPath == null) continue; // "nicht ändern"
                PackSource src = open.get(packPath);
                if (src == null) {
                    src = new PackSource(packPath);
                    open.put(packPath, src);
                }
                for (String name : src.entries()) {
                    String lower = name.toLowerCase();
                    if (!lower.startsWith("assets/")) continue;
                    if (!cat.test().test("/" + lower)) continue;
                    if (!written.add(lower)) continue; // schon von einer spezielleren Kategorie belegt
                    Path target = out.resolve(name).normalize();
                    if (!target.startsWith(out)) continue; // Sicherheit
                    Files.createDirectories(target.getParent());
                    try (InputStream in = src.open(name)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } finally {
            for (PackSource s : open.values()) {
                try { s.close(); } catch (IOException ignored) {}
            }
        }

        String mcmeta = "{\"pack\":{\"description\":\"Mixed Pack (Texture Pack Mixer)\",\"min_format\":[75,0],\"max_format\":[100,0]}}";
        Files.writeString(out.resolve("pack.mcmeta"), mcmeta, StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (Stream<Path> s = Files.walk(p)) {
            s.sorted(Comparator.reverseOrder()).forEach(x -> {
                try { Files.delete(x); } catch (IOException ignored) {}
            });
        }
    }
}
