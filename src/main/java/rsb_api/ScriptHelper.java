package rsb_api;

import net.runelite.rsb.internal.globval.GlobalConfiguration;

import rsb_api.wrappers.common.CacheProvider;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.File;

@Slf4j
public class ScriptHelper {
    /**
     * Checks if the cache exists and if it does, loads it
     * if not it creates a new cache and saves it
     *
     * @throws IOException If the file isn't found or is inaccessible then an IOException has occurred.
     */
    public static void checkForCacheAndLoad() {
        try {
            String gameCacheLocation = GlobalConfiguration.Paths.getRuneLiteGameCacheDirectory();
            String objectCacheLocation = GlobalConfiguration.Paths.getObjectsCacheDirectory();
            String itemCacheLocation = GlobalConfiguration.Paths.getItemsCacheDirectory();
            String npcCacheLocation = GlobalConfiguration.Paths.getNPCsCacheDirectory();
            String spriteCacheLocation = GlobalConfiguration.Paths.getSpritesCacheDirectory();

            //TODO Some sort of better validation here
            //Add a version check
            if (!new File(itemCacheLocation).exists() && new File(itemCacheLocation).getTotalSpace() < 100) {
                String[] itemArgs = {"--cache", gameCacheLocation, "--items", itemCacheLocation};
                String[] objectArgs = {"--cache", gameCacheLocation, "--objects", objectCacheLocation};
                String[] npcArgs = {"--cache", gameCacheLocation, "--npcs", npcCacheLocation};
                String[] spriteArgs = {"--cache", gameCacheLocation, "--sprites", spriteCacheLocation};

                net.runelite.cache.Cache.main(itemArgs);
                net.runelite.cache.Cache.main(objectArgs);
                net.runelite.cache.Cache.main(npcArgs);

                if (!new File(spriteCacheLocation).exists()) {
                    new File(spriteCacheLocation).mkdir();
                    net.runelite.cache.Cache.main(spriteArgs);
                }
            }

            CacheProvider.fillFileCache();
            log.info("checkForCacheAndLoad() complete");

        } catch (Exception e) {
            log.warn("checkForCacheAndLoad failed " + e);
            e.printStackTrace();
        }
    }
}
