package cn.powernukkitx.replaynk.trail;

import cn.nukkit.Player;
import cn.nukkit.api.DoNotModify;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.item.Item;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.math.Vector3;
import cn.powernukkitx.replaynk.ReplayNK;
import cn.powernukkitx.replaynk.item.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * @author daoge_cmd
 * @date 2023/6/16
 * ReplayNK Project
 */
@Getter
@Log4j2
public final class Trail {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final Map<String, Trail> TRAILS = new HashMap<>();
    private static final Map<Player, Trail> OPERATING_TRAILS = new HashMap<>();
    public static final double DEFAULT_BEZIER_CURVE_STEP = 0.001;
    public static final double DEFAULT_MIN_DISTANCE = 0.5;
    public static final double DEFAULT_CAMERA_SPEED = 2;
    private final List<Marker> markers = new ArrayList<>();
    private final String name;
    private transient Player operator;
    @Setter
    private transient boolean playing;
    private transient List<Marker> runtimeMarkers;
    @Setter
    private transient boolean changed;
    @Setter
    private boolean useBezierCurves = false;
    @Setter
    private boolean showBezierCurves = true;
    @Setter
    private double minDistance = DEFAULT_MIN_DISTANCE;
    @Setter
    private double defaultCameraSpeed = DEFAULT_CAMERA_SPEED;

    private Trail(String name) {
        this.name = name;
    }

    @DoNotModify
    public static Map<String, Trail> getTrails() {
        return TRAILS;
    }

    public static Trail getTrail(String name) {
        return TRAILS.get(name);
    }

    public static void addTrail(Trail trail) {
        if (TRAILS.containsKey(trail.getName()))
            throw new IllegalArgumentException("Trail " + trail.getName() + " already exists.");
        TRAILS.put(trail.getName(), trail);
    }

    public static Trail removeTrail(String name) {
        return TRAILS.remove(name);
    }

    @SneakyThrows
    public static void readAllTrails() {
        var basePath = ReplayNK.getInstance().getDataFolder().toPath().resolve("trails");
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
            return;
        }
        try (var paths = Files.walk(basePath)) {
            paths.forEach(path -> {
                if (Files.isDirectory(path))
                    return;
                String json;
                try {
                    json = Files.readString(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                var trail = fromJson(json);
                log.info("Loaded trail " + trail.getName() + " from " + path);
            });
        }
    }

    @SneakyThrows
    public static void saveAllTrails() {
        for (var trail : TRAILS.values()) {
            var basePath = ReplayNK.getInstance().getDataFolder().toPath().resolve("trails");
            if (!Files.exists(basePath))
                Files.createDirectory(basePath);
            var path = basePath.resolve(trail.getName() + ".json");
            log.info("Saving trail " + trail.getName() + " to " + path);
            if (!Files.exists(path))
                Files.createFile(path);
            var json = trail.toJson();
            Files.writeString(path, json);
        }
    }

    public static void closeAndSave() {
        for (var trail : OPERATING_TRAILS.values()) {
            trail.stopOperating();
        }
        saveAllTrails();
    }

    public static Trail getOperatingTrail(Player player) {
        return OPERATING_TRAILS.get(player);
    }

    public static boolean isOperatingTrail(Player player) {
        return OPERATING_TRAILS.containsKey(player);
    }

    public static Trail create(String name) {
        if (TRAILS.containsKey(name))
            return null;
        var trail = new Trail(name);
        addTrail(trail);
        return trail;
    }

    public static Trail fromJson(String json) {
        var trail = GSON.fromJson(json, Trail.class);
        addTrail(trail);
        return trail;
    }

    public void startOperating(Player player) {
        if (operator != null)
            throw new IllegalStateException("Trail " + name + " is already operating by " + operator.getName());
        operator = player;
        OPERATING_TRAILS.put(player, this);
        prepareHotBar(player);
        markers.forEach(marker -> marker.spawnDisplayEntity(player.getLevel(), this));
    }

    public void tick() {
        if (operator != null && useBezierCurves && showBezierCurves && !playing) {
            getOrCalculateRuntimeMarkers().forEach(marker -> operator.getLevel().addParticleEffect(new Vector3(marker.getX(), marker.getY(), marker.getZ()), ParticleEffect.BALLOON_GAS));
        }
    }

    public void clearRuntimeMarkers() {
        if (runtimeMarkers == null) {
            runtimeMarkers = new ArrayList<>();
            return;
        }
        if (runtimeMarkers.isEmpty())
            return;
        runtimeMarkers.clear();
    }

    public List<Marker> getOrCalculateRuntimeMarkers() {
        if (runtimeMarkers == null || runtimeMarkers.isEmpty() || isChanged()) {
            prepareRuntimeMarkers();
            setChanged(false);
        }
        return runtimeMarkers;
    }

    private void prepareHotBar(Player player) {
        var inventory = player.getInventory();
        inventory.clearAll();

        var addMarkerItem = new AddMarkerItem();
        var clearMarkerItem = new ClearMarkerItem();
        var editMarkerItem = new EditMarkerItem();
        var playItem = new PlayItem();
        var pauseItem = new PauseItem();
        var settingItem = new SettingItem();
        var exitItem = new ExitItem();

        addMarkerItem.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        clearMarkerItem.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        editMarkerItem.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        playItem.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        pauseItem.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        settingItem.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);
        exitItem.setItemLockMode(Item.ItemLockMode.LOCK_IN_SLOT);

        inventory.setItem(0, addMarkerItem);
        inventory.setItem(1, clearMarkerItem);
        inventory.setItem(2, editMarkerItem);
        inventory.setItem(3, playItem);
        inventory.setItem(4, pauseItem);
        inventory.setItem(5, settingItem);
        inventory.setItem(8, exitItem);
    }

    public void stopOperating() {
        if (operator == null)
            throw new IllegalStateException("Trail " + name + " is not operating.");
        playing = false;
        OPERATING_TRAILS.remove(operator);
        operator.getInventory().clearAll();
        markers.forEach(Marker::deleteDisplayEntity);
        operator.sendMessage(ReplayNK.getI18n().tr(operator.getLanguageCode(), "replaynk.trail.stopoperating", name));
        operator = null;
    }

    public void addMarker(MarkerBuilder builder) {
        var marker = builder.build(this);
        addMarker(marker);
    }

    public void addMarker(Marker marker) {
        markers.add(marker);
        if (operator != null) {
            marker.spawnDisplayEntity(operator.getLevel(), this);
        }
        setChanged(true);
    }

    public void insertMarker(int index, Marker marker) {
        markers.add(index, marker);
        if (operator != null) {
            marker.spawnDisplayEntity(operator.getLevel(), this);
        }
        for (int i = index + 1; i < markers.size(); i++) {
            markers.get(i).updateDisplayEntity(this);
        }
        setChanged(true);
    }

    public void removeMarker(int index) {
        var removedMarker = markers.remove(index);
        removedMarker.deleteDisplayEntity();
        for (int i = index; i < markers.size(); i++) {
            markers.get(i).updateDisplayEntity(this);
        }
        setChanged(true);
    }

    public void moveMarker(int oldIndex, int newIndex) {
        var marker = markers.remove(oldIndex);
        markers.add(newIndex, marker);
        for (int i = Math.min(oldIndex, newIndex); i < markers.size(); i++) {
            markers.get(i).updateDisplayEntity(this);
        }
        setChanged(true);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public void play(Player player) {
        if (playing) {
            player.sendMessage(ReplayNK.getI18n().tr(operator.getLanguageCode(), "replaynk.trail.alreadyplaying", name));
            return;
        }
        if (markers.size() <= 1) {
            player.sendMessage(ReplayNK.getI18n().tr(operator.getLanguageCode(), "replaynk.trail.toolittlemarks", name));
            return;
        }
        player.sendMessage(ReplayNK.getI18n().tr(operator.getLanguageCode(), "replaynk.trail.startplaying", name));
        playing = true;
        markers.forEach(Marker::invisible);
        prepareRuntimeMarkers();
        new Thread(() -> runtimeMarkers.get(0).play(player, this)).start();
    }

    public void showEditorForm(Player player) {
        var langCode = player.getLanguageCode();
        var useBezierCurvesElement = new ElementToggle(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.usebeziercurves"), useBezierCurves);
        var useBezierCurvesDetailsElement = new ElementLabel(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.usebeziercurves.details"));
        var showBazierCurvesElement = new ElementToggle(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.showbeziercurves"), showBezierCurves);
        var showBazierCurvesDetailsElement = new ElementLabel(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.showbeziercurves.details"));
        var minDistanceElement = new ElementInput(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.mindistance"), String.valueOf(DEFAULT_MIN_DISTANCE), String.valueOf(minDistance));
        var minDistanceDetailsElement = new ElementLabel(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.mindistance.details"));
        var defaultCameraSpeedElement = new ElementInput(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.defaultcameraspeed"), String.valueOf(DEFAULT_CAMERA_SPEED), String.valueOf(defaultCameraSpeed));
        var defaultCameraSpeedDetailsElement = new ElementLabel(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.defaultcameraspeed.details"));
        var doRecalculateEaseTimeElement = new ElementToggle(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.dorecalculateeasetime"), false);
        var doRecalculateEaseTimeDetailsElement = new ElementLabel(ReplayNK.getI18n().tr(langCode, "replaynk.trail.editorform.dorecalculateeasetime.details"));
        var form = new FormWindowCustom(name, List.of(useBezierCurvesElement, useBezierCurvesDetailsElement, showBazierCurvesElement, showBazierCurvesDetailsElement, minDistanceElement, minDistanceDetailsElement, defaultCameraSpeedElement, defaultCameraSpeedDetailsElement, doRecalculateEaseTimeElement, doRecalculateEaseTimeDetailsElement));
        form.addHandler((p, id) -> {
            var response = form.getResponse();
            if (response == null) return;
            try {
                useBezierCurves = response.getToggleResponse(0);
                showBezierCurves = response.getToggleResponse(2);
                minDistance = Double.parseDouble(response.getInputResponse(4));
                setChanged(true);
                defaultCameraSpeed = Double.parseDouble(response.getInputResponse(6));
                if (response.getToggleResponse(8)) {
                    resetAllMarkerSpeed();
                    computeAllLinearDistance(markers, false);
                }
            } catch (Exception e) {
                player.sendMessage(ReplayNK.getI18n().tr(langCode, "replaynk.generic.invalidinput"));
            }
        });
        player.showFormWindow(form);
    }

    public void prepareRuntimeMarkers() {
        clearRuntimeMarkers();
        if (useBezierCurves) {
            int n = markers.size() - 1;

            for (double u = 0; u <= 1; u += DEFAULT_BEZIER_CURVE_STEP) {
                Marker[] p = new Marker[n + 1];
                for (int i = 0; i <= n; i++) {
                    p[i] = new Marker(markers.get(i));
                }

                for (int r = 1; r <= n; r++) {
                    for (int i = 0; i <= n - r; i++) {
                        p[i].setX((1 - u) * p[i].getX() + u * p[i + 1].getX());
                        p[i].setY((1 - u) * p[i].getY() + u * p[i + 1].getY());
                        p[i].setZ((1 - u) * p[i].getZ() + u * p[i + 1].getZ());
                        p[i].setRotX((1 - u) * p[i].getRotX() + u * p[i + 1].getRotX());
                        p[i].setRotY((1 - u) * p[i].getRotY() + u * p[i + 1].getRotY());
                        p[i].setCameraSpeed((1 - u) * p[i].getCameraSpeed() + u * p[i + 1].getCameraSpeed());
                    }
                }
                runtimeMarkers.add(p[0]);
            }

            computeAllLinearDistance(runtimeMarkers, true);
        } else {
            runtimeMarkers.addAll(markers);
        }
        cacheIndexForRuntimeMarkers();
    }

    public void resetAllMarkerSpeed() {
        for (var marker : markers) {
            marker.setCameraSpeed(defaultCameraSpeed);
        }
    }

    private void computeAllLinearDistance(List<Marker> markers, boolean doRemoveTooCloseMarker) {
        boolean first = true;
        for (Iterator<Marker> iterator = markers.iterator(); iterator.hasNext(); ) {
            var marker = iterator.next();
            if (first) {
                first = false;
                marker.setDistance(1);
                marker.setCameraSpeed(1);
                continue;
            }
            var lastMarker = markers.get(markers.indexOf(marker) - 1);
            var distance = Math.sqrt(Math.pow(lastMarker.getX() - marker.getX(), 2) + Math.pow(lastMarker.getY() - marker.getY(), 2) + Math.pow(lastMarker.getZ() - marker.getZ(), 2));
            if (distance < minDistance && doRemoveTooCloseMarker) {
                iterator.remove();
            } else {
                marker.setDistance(distance);
            }
        }
    }

    private void cacheIndexForRuntimeMarkers() {
        //为runtime marker缓存index，提高运镜时流畅度
        for (int i = 0; i < runtimeMarkers.size(); i++) {
            runtimeMarkers.get(i).cacheIndex(i);
        }
    }

    public boolean pause() {
        if (!playing)
            return false;
        playing = false;
        return true;
    }

}
