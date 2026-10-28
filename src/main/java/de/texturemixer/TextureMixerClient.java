package de.texturemixer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class TextureMixerClient implements ClientModInitializer {

    // Standardtaste: F7. Ändern: Optionen > Steuerung > Tastenbelegung > "Texture Pack Mixer"
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("texturemixer", "main"));

        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.texturemixer.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_F7,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new MixerScreen());
                }
            }
        });
    }
}
