// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — MapService (= Services.Map, phase 8b).
 *
 * Registers DATA_STATIC_MAPS (id "dataStaticMaps" in urls.json) via the net stack, parses maps.json
 * into tile descriptors, then downloads + caches each PNG tile and loads it as a NativeImage-backed
 * texture. getMapsForBoundingBox is used by AbstractMapScreen.renderMap.
 *
 * SLIM vs Wynntils: uses a local Gson (no WynntilsMod.GSON in the shim) and the shim's plain
 * BoundingBox overlap test (no BoundingShape / BoundingCircle). getMapsForBoundingCircle +
 * isPlayerInMappedArea are dropped (no fork caller). Faithful otherwise: same maps.json schema,
 * same Managers.Net.download(URI, cacheName, md5).handleInputStream flow.
 */
package julianh06.wynnextras.wtshim.services.map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Managers;
import julianh06.wynnextras.wtshim.core.components.Service;
import julianh06.wynnextras.wtshim.core.net.Download;
import julianh06.wynnextras.wtshim.core.net.DownloadRegistry;
import julianh06.wynnextras.wtshim.core.net.UrlId;
import julianh06.wynnextras.wtshim.utils.type.BoundingBox;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.texture.NativeImage;

public final class MapService extends Service {
    private static final Gson GSON = new Gson();

    private volatile List<MapTexture> maps = new CopyOnWriteArrayList<>();

    @Override
    public void registerDownloads(DownloadRegistry registry) {
        registry.registerDownload(UrlId.DATA_STATIC_MAPS).handleReader(this::handleMaps);
    }

    public List<MapTexture> getMapsForBoundingBox(BoundingBox box) {
        return maps.stream().filter(map -> box.intersects(map.getBox())).toList();
    }

    private void handleMaps(Reader reader) {
        Type type = new TypeToken<List<MapPartProfile>>() {}.getType();

        List<MapPartProfile> mapPartList = GSON.fromJson(reader, type);
        if (mapPartList == null) return;

        List<MapTexture> newMaps = new CopyOnWriteArrayList<>();
        for (MapPartProfile mapPart : mapPartList) {
            String fileName = mapPart.md5 + ".png";
            loadMapPart(mapPart, fileName, newMaps);
        }

        maps = newMaps;
    }

    private void loadMapPart(MapPartProfile mapPart, String fileName, List<MapTexture> newMaps) {
        Download dl = Managers.Net.download(
                URI.create(Managers.Url.getDownloadSourceUrl() + mapPart.path), "maps/" + fileName, mapPart.md5);
        dl.handleInputStream(
                inputStream -> {
                    try {
                        NativeImage nativeImage = NativeImage.read(inputStream);
                        MapTexture mapPartImage =
                                new MapTexture(fileName, nativeImage, mapPart.x1, mapPart.z1, mapPart.x2, mapPart.z2);
                        newMaps.add(mapPartImage);
                    } catch (IOException e) {
                        WynntilsMod.warn("IOException occurred while loading map image of " + mapPart.name, e);
                    }
                },
                onError -> WynntilsMod.warn("Error occurred while downloading map image of " + mapPart.name, onError));
    }

    private record MapPartProfile(String name, String url, String path, int x1, int z1, int x2, int z2, String md5) {}
}
