package com.ldtteam.common.language;

import net.minecraft.client.Minecraft;

public class ClientLocale
{
    public static String getLocale()
    {
        // Trust me, Minecraft.getInstance() can be null, when you run Data Generators!
        // FABRIC-PORT: Fabric runs mod initializers before the client options exist; the cache falls back to en_us and is replaced by
        // the vanilla language system once that has loaded (LanguageHandler#setMClanguageLoaded).
        final Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.options == null ? "en_us" : mc.options.languageCode;
    }
}
