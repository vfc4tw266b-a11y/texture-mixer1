package de.texturemixer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixerScreen extends Screen {

    private static final int ROWS = 6;

    private final List<Path> packs = new ArrayList<>();
    // 0 = "nicht ändern", 1..n = Pack
    private final int[] selection = new int[Mixer.CATEGORIES.size()];
    private volatile String status = "";

    public MixerScreen() {
        super(Component.literal("Texture Pack Mixer"));
        try {
            packs.addAll(Mixer.listPacks());
        } catch (IOException e) {
            status = "Packs konnten nicht gelesen werden: " + e.getMessage();
        }
    }

    private String label(int i) {
        String cat = Mixer.CATEGORIES.get(i).name();
        String value = selection[i] == 0 ? "-" : packs.get(selection[i] - 1).getFileName().toString();
        if (value.toLowerCase().endsWith(".zip")) value = value.substring(0, value.length() - 4);
        if (value.length() > 16) value = value.substring(0, 15) + "…";
        return cat + ": " + value;
    }

    @Override
    protected void init() {
        int colW = 200;
        int left = this.width / 2 - colW - 5;
        int right = this.width / 2 + 5;
        int top = 40;

        for (int i = 0; i < Mixer.CATEGORIES.size(); i++) {
            final int idx = i;
            int x = (i / ROWS == 0) ? left : right;
            int y = top + (i % ROWS) * 24;
            Button button = Button.builder(Component.literal(label(i)), b -> {
                selection[idx] = (selection[idx] + 1) % (packs.size() + 1);
                b.setMessage(Component.literal(label(idx)));
            }).bounds(x, y, colW, 20).build();
            this.addRenderableWidget(button);
        }

        int btnY = top + ROWS * 24 + 12;
        this.addRenderableWidget(Button.builder(Component.literal("Speichern"), b -> save())
                .bounds(left, btnY, colW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Schließen"), b -> this.onClose())
                .bounds(right, btnY, colW, 20).build());
    }

    private void save() {
        Map<Mixer.Category, Path> chosen = new HashMap<>();
        for (int i = 0; i < selection.length; i++) {
            if (selection[i] > 0) chosen.put(Mixer.CATEGORIES.get(i), packs.get(selection[i] - 1));
        }
        if (chosen.isEmpty()) {
            status = "Bitte wähle mindestens eine Kategorie aus.";
            return;
        }
        status = "Mische ...";
        new Thread(() -> {
            try {
                Mixer.mix(chosen);
                status = "Fertig! Drücke jetzt F3+T. Beim ersten Mal: Pack \"MixedPack\" aktivieren.";
            } catch (Exception e) {
                status = "Fehler: " + e.getMessage();
            }
        }, "TextureMixer").start();
    }

    private void centered(GuiGraphicsExtractor g, String text, int y, int color) {
        g.text(this.font, text, this.width / 2 - this.font.width(text) / 2, y, color, true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        centered(graphics, "Texture Pack Mixer", 12, 0xFFFFFFFF);
        centered(graphics, "Klick auf einen Button = nächstes Pack. \"-\" = nicht ändern.", 24, 0xFFAAAAAA);
        if (packs.isEmpty()) {
            centered(graphics, "Keine Texture Packs im resourcepacks-Ordner gefunden.", this.height - 40, 0xFFFF5555);
        }
        if (!status.isEmpty()) {
            centered(graphics, status, this.height - 24, 0xFFFFFF55);
        }
    }
}
