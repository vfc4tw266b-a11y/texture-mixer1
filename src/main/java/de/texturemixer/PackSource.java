package de.texturemixer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Ein Texture Pack, entweder als .zip oder als Ordner. */
public final class PackSource implements AutoCloseable {
    private final Path path;
    private ZipFile zip;

    public PackSource(Path path) throws IOException {
        this.path = path;
        if (Files.isRegularFile(path)) {
            this.zip = new ZipFile(path.toFile());
        }
    }

    /** Alle Dateien im Pack, mit "/" als Trenner (z. B. assets/minecraft/textures/block/stone.png). */
    public List<String> entries() throws IOException {
        List<String> result = new ArrayList<>();
        if (zip != null) {
            var e = zip.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory()) result.add(entry.getName());
            }
        } else {
            try (Stream<Path> s = Files.walk(path)) {
                s.filter(Files::isRegularFile).forEach(p ->
                        result.add(path.relativize(p).toString().replace('\\', '/')));
            }
        }
        return result;
    }

    public InputStream open(String name) throws IOException {
        if (zip != null) return zip.getInputStream(zip.getEntry(name));
        return Files.newInputStream(path.resolve(name));
    }

    @Override
    public void close() throws IOException {
        if (zip != null) zip.close();
    }
}
