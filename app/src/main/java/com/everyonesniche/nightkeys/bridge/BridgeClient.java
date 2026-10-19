package com.everyonesniche.nightkeys.bridge;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Display;
import com.everyonesniche.nightkeys.core.BacklightSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rikka.shizuku.Shizuku;

/**
 * Shizuku transport for the Titan 2 Elite keyboard-light vendor store.
 *
 * This intentionally copies the community-proven shell interface used by
 * titan2-keylight / PhysiBoard rather than trying to discover a vendor Binder
 * from the app UID. The privileged process executes fixed argv arrays as shell:
 *
 *   service call agui_common 3 s16 <key> s16 <default>   (read)
 *   service call agui_common 4 s16 <key> s16 <value>     (write)
 *   am broadcast -a agui.action.CLOSE_KEYBOARD_LIGHT     (immediate off)
 *
 * Only an allow-list of known keys can be addressed and every write is read back.
 */
public final class BridgeClient {
    public static final int REQUEST = 734;
    public static final String BIG_TIMEOUT = "2147483647";
    private static final String SERVICE = "agui_common";
    private static final String MISSING = "__NIGHTKEYS_MISSING__";
    private static final Set<String> READABLE_KEYS = Set.of(
            BacklightSession.TIMEOUT,
            BacklightSession.TIMEOUT_BACKUP,
            BacklightSession.BRIGHTNESS,
            BacklightSession.AUTO);
    private static final Pattern HEX_WORD = Pattern.compile("(?i)(?<![0-9a-f])[0-9a-f]{8}(?![0-9a-f])");

    private final Context context;
    private final Runnable changed;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService setup = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nightkeys-vendor-setup");
        t.setDaemon(true);
        return t;
    });
    private final Object commandLock = new Object();
    private volatile Bundle info;
    private volatile String status = "Start Shizuku, then authorize NightKeys.";
    private volatile String transportDiagnostics = "Not checked";
    private volatile boolean ready;
    private boolean connecting;
    private long generation;

    private final IPrivilegedBridge direct = new IPrivilegedBridge.Stub() {
        @Override public Bundle inspect() { return inspectDirect(); }
        @Override public void enableContinuous() {
            if (!screenInteractive()) throw new IllegalStateException("Screen is not interactive");
            writeVendorKey(BacklightSession.TIMEOUT, BIG_TIMEOUT);
            if (!screenInteractive()) {
                try { disableLightingDirect(); } catch (Exception ignored) { }
                throw new IllegalStateException("Screen turned off while enabling keyboard lighting");
            }
        }
        @Override public void disableLighting() { disableLightingDirect(); }
        @Override public boolean verifyContinuous() {
            if (!screenInteractive()) return false;
            String value = readVendorKey(BacklightSession.TIMEOUT, MISSING);
            if (MISSING.equals(value)) throw new IllegalStateException("Vendor keyboard timeout disappeared");
            return BIG_TIMEOUT.equals(value);
        }
        @Override public void restore(String originalTimeout) {
            BacklightSession.requireTimeout(originalTimeout);
            writeVendorKey(BacklightSession.TIMEOUT, originalTimeout);
            if ("0".equals(originalTimeout)) closeKeyboardLight();
        }
        @Override public void destroy() { /* no separate privileged process is owned */ }
    };

    public BridgeClient(Context context, Runnable changed) {
        this.context = context.getApplicationContext();
        this.changed = changed;
        Shizuku.addBinderReceivedListenerSticky(() -> main.post(() -> {
            refreshAuthorizationStatus();
            connect();
        }));
        Shizuku.addBinderDeadListener(() -> main.post(() -> disconnect(
                "Shizuku stopped. Start it again; the phone still turns the keyboard off with the screen.")));
        Shizuku.addRequestPermissionResultListener((request, grant) -> {
            if (request != REQUEST) return;
            main.post(() -> {
                if (grant == PackageManager.PERMISSION_GRANTED) {
                    status = "Shizuku authorization granted ✓ · checking Titan vendor service…";
                    changed.run();
                    connect(true);
                } else {
                    status = "Authorization denied. Allow NightKeys in Shizuku to continue.";
                    changed.run();
                }
            });
        });
    }

    public void authorize() {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post(this::authorize); return; }
        try {
            if (!Shizuku.pingBinder()) {
                status = "Shizuku is not running. Start it first.";
                changed.run();
                return;
            }
            if (Shizuku.isPreV11()) throw new IllegalStateException("Update Shizuku to a current release");
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                status = "Shizuku authorization recognized ✓ · checking Titan vendor service…";
                changed.run();
                connect(true);
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                status = "Open Shizuku → Authorized applications and allow NightKeys.";
                changed.run();
            } else {
                Shizuku.requestPermission(REQUEST);
            }
        } catch (Exception e) {
            status = clean(e);
            changed.run();
        }
    }

    public void connect() { connect(false); }

    private void connect(boolean force) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post(() -> connect(force)); return; }
        try {
            if (!force && ready()) return;
            if (connecting) return;
            if (!Shizuku.pingBinder()) {
                disconnect("Shizuku is not running. Start it, then return to NightKeys.");
                return;
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                disconnect("Shizuku is running · authorization required.");
                return;
            }
            ready = false;
            info = null;
            connecting = true;
            long attempt = ++generation;
            status = "Shizuku authorized ✓ · checking shell UID and agui_common…";
            changed.run();
            setup.execute(() -> {
                try {
                    Bundle inspected = inspectDirect();
                    main.post(() -> {
                        if (generation != attempt) return;
                        info = inspected;
                        ready = true;
                        connecting = false;
                        status = "Shizuku authorized ✓ · agui_common ready ✓";
                        transportDiagnostics = "Remote shell UID " + inspected.getInt("shellUid")
                                + " · agui_common readback OK";
                        changed.run();
                    });
                } catch (Exception e) {
                    main.post(() -> {
                        if (generation != attempt) return;
                        ready = false;
                        connecting = false;
                        transportDiagnostics = clean(e);
                        status = "Shizuku authorized ✓. Titan vendor check failed: " + clean(e);
                        changed.run();
                    });
                }
            });
        } catch (Exception e) {
            disconnect("Shizuku setup failed: " + clean(e));
        }
    }

    private void refreshAuthorizationStatus() {
        try {
            if (!Shizuku.pingBinder()) status = "Shizuku is not running.";
            else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                status = ready ? "Shizuku authorized ✓ · agui_common ready ✓"
                        : "Shizuku authorization recognized ✓ · vendor check required";
            else status = "Shizuku is running · authorization required.";
        } catch (Exception e) {
            status = clean(e);
        }
        changed.run();
    }

    private Bundle inspectDirect() {
        requireAuthorized();
        int shellUid = shellUid();
        if (shellUid != 2000 && shellUid != 0)
            throw new IllegalStateException("Shizuku command process has unexpected UID " + shellUid);
        String services = run("/system/bin/service", "list").out;
        if (!containsService(services, SERVICE))
            throw new IllegalStateException("agui_common is not visible from the Shizuku shell");
        String timeout = readVendorKey(BacklightSession.TIMEOUT, MISSING);
        if (MISSING.equals(timeout))
            throw new IllegalStateException("agui_common did not expose keyboard_brightness_timeout");
        BacklightSession.requireTimeout(timeout);

        Bundle result = new Bundle();
        result.putString("timeout", timeout);
        result.putString("timeoutBackup", readVendorKey(BacklightSession.TIMEOUT_BACKUP, MISSING));
        result.putString("brightness", readVendorKey(BacklightSession.BRIGHTNESS, MISSING));
        result.putString("auto", readVendorKey(BacklightSession.AUTO, MISSING));
        result.putBoolean("interactive", screenInteractive());
        result.putInt("shellUid", shellUid);
        result.putInt("shizukuApi", Shizuku.getVersion());
        result.putString("transport", "Shizuku remote process → /system/bin/service → agui_common");
        return result;
    }

    private int shellUid() {
        String value = run("/system/bin/id", "-u").out.trim();
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new IllegalStateException("Cannot identify Shizuku command UID: " + value); }
    }

    private static boolean containsService(String listing, String wanted) {
        for (String line : listing.split("\\R")) {
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String left = line.substring(0, colon).trim();
            int tab = left.lastIndexOf('\t');
            int space = left.lastIndexOf(' ');
            int cut = Math.max(tab, space);
            String name = cut >= 0 ? left.substring(cut + 1) : left;
            if (wanted.equals(name)) return true;
        }
        // Android's service-list formatting differs slightly by release. A bounded
        // token fallback is safe because the wanted service name is a constant.
        return Pattern.compile("(?m)(?:^|\\s)" + Pattern.quote(wanted) + ":").matcher(listing).find();
    }

    private String readVendorKey(String key, String defaultValue) {
        requireKnownKey(key);
        ShizukuShell.Result result = run("/system/bin/service", "call", SERVICE, "3",
                "s16", key, "s16", defaultValue);
        String decoded = decodeParcelString(result.out);
        if (decoded == null) throw new IllegalStateException("Could not decode agui_common reply for " + key);
        return decoded;
    }

    private void writeVendorKey(String key, String value) {
        requireKnownKey(key);
        if (!BacklightSession.TIMEOUT.equals(key))
            throw new IllegalArgumentException("NightKeys only writes keyboard_brightness_timeout");
        BacklightSession.requireTimeout(value);
        run("/system/bin/service", "call", SERVICE, "4", "s16", key, "s16", value);
        String observed = readVendorKey(key, MISSING);
        if (!value.equals(observed))
            throw new IllegalStateException("Vendor timeout readback mismatch: requested " + value + ", got " + observed);
    }

    private void disableLightingDirect() {
        RuntimeException failure = null;
        try { writeVendorKey(BacklightSession.TIMEOUT, "0"); }
        catch (RuntimeException e) { failure = e; }
        try { closeKeyboardLight(); }
        catch (RuntimeException e) {
            if (failure == null) failure = e; else failure.addSuppressed(e);
        }
        if (failure != null) throw failure;
    }

    private void closeKeyboardLight() {
        run("/system/bin/am", "broadcast", "-a", "agui.action.CLOSE_KEYBOARD_LIGHT");
    }

    public void forceOff() {
        try { require().disableLighting(); }
        catch (android.os.RemoteException e) { throw new IllegalStateException("Local bridge failure", e); }
    }

    private ShizukuShell.Result run(String... argv) {
        requireAuthorized();
        synchronized (commandLock) {
            return ShizukuShell.run(8_000, argv);
        }
    }

    static String decodeParcelString(String raw) {
        if (raw == null) return null;
        String scrubbed = raw.replaceAll("(?i)0x[0-9a-f]+:", " ");
        Matcher matcher = HEX_WORD.matcher(scrubbed);
        List<Long> words = new ArrayList<>();
        while (matcher.find()) {
            try { words.add(Long.parseUnsignedLong(matcher.group(), 16)); }
            catch (NumberFormatException ignored) { }
        }
        if (words.size() < 2) return null;
        long lengthWord = words.get(1);
        if (lengthWord == 0xffffffffL) return null;
        if (lengthWord < 0 || lengthWord > 4096) return null;
        int length = (int) lengthWord;
        StringBuilder out = new StringBuilder(length);
        int remaining = length;
        for (int i = 2; i < words.size() && remaining > 0; i++) {
            long word = words.get(i);
            char low = (char) (word & 0xffff);
            out.append(low);
            remaining--;
            if (remaining > 0) {
                char high = (char) ((word >>> 16) & 0xffff);
                out.append(high);
                remaining--;
            }
        }
        if (remaining != 0) return null;
        return out.toString();
    }

    private static void requireKnownKey(String key) {
        if (!READABLE_KEYS.contains(key)) throw new IllegalArgumentException("Vendor key is not allow-listed");
    }

    private void requireAuthorized() {
        try {
            if (!Shizuku.pingBinder()) throw new IllegalStateException("Shizuku is not running");
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                throw new SecurityException("NightKeys is not authorized in Shizuku");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot verify Shizuku authorization: " + clean(e), e);
        }
    }

    private boolean screenInteractive() {
        PowerManager power = context.getSystemService(PowerManager.class);
        DisplayManager displays = context.getSystemService(DisplayManager.class);
        Display screen = displays == null ? null : displays.getDisplay(Display.DEFAULT_DISPLAY);
        return power != null && power.isInteractive() && screen != null && screen.getState() == Display.STATE_ON;
    }

    private void disconnect(String message) {
        ++generation;
        ready = false;
        connecting = false;
        info = null;
        status = message == null ? "Shizuku unavailable" : message;
        changed.run();
    }

    public boolean ready() {
        if (!ready) return false;
        try {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean authorized() {
        try {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    public String status() { return status; }
    public String transportDiagnostics() { return transportDiagnostics; }
    public Bundle info() { return info == null ? null : new Bundle(info); }

    public IPrivilegedBridge require() {
        if (!ready()) throw new IllegalStateException("Shizuku agui_common channel is not ready yet");
        return direct;
    }

    private static String clean(Throwable e) {
        if (e == null) return "";
        String value = e.getMessage();
        return value == null || value.isBlank() ? e.getClass().getSimpleName() : value;
    }
}
