package com.everyonesniche.nightkeys.bridge;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import com.everyonesniche.nightkeys.NightKeysApp;
import com.everyonesniche.nightkeys.ui.MainActivity;
import rikka.shizuku.Shizuku;

/** Emulator-only transport tests. The installable APK does not contain this class. */
public final class PermissionTests extends Instrumentation {
    private Bundle arguments;
    private int passed;
    private Context context;
    private NightKeysApp app;
    private interface Checked { void run() throws Exception; }
    private interface Condition { boolean get() throws Exception; }

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        arguments = args == null ? new Bundle() : args;
        start();
    }

    @Override public void onStart() {
        context = getTargetContext();
        app = (NightKeysApp) context.getApplicationContext();
        try {
            if ("reconnect".equals(arguments.getString("scenario"))) reconnect();
            else fresh();
            Bundle result = new Bundle();
            result.putString("stream", "\nNIGHTKEYS_SHELL_TESTS_PASSED count=" + passed
                    + " scenario=" + arguments.getString("scenario", "fresh") + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable e) {
            Bundle result = new Bundle();
            result.putString("stream", "\nNIGHTKEYS_SHELL_TESTS_FAILED\n"
                    + android.util.Log.getStackTraceString(e) + "\nBridge: " + app.bridge.status()
                    + "\nTransport: " + app.bridge.transportDiagnostics() + "\n");
            result.putString("shortMsg", String.valueOf(e));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void fresh() throws Exception {
        test("fresh installation needs no saved check to enable on a compatible device", () -> {
            check(!context.getSharedPreferences("configuration", Context.MODE_PRIVATE).contains("verified_build"),
                    "Fresh fixture unexpectedly verified");
            check(com.everyonesniche.nightkeys.core.Onboarding.canStart(true, true, true, false, false),
                    "Fresh connected device was gated");
            check(!com.everyonesniche.nightkeys.core.Onboarding.canStart(true, true, true, false, true),
                    "Pending restoration must remain protected");
        });
        test("parcel parser decodes one and two code units per word", () -> {
            String a = "Result: Parcel( 00000000 00000003 00300031 00000030 '........1.0.0...')";
            check("100".equals(BridgeClient.decodeParcelString(a)), "Did not decode 100");
            String b = "Result: Parcel(\\n  0x00000000: 00000000 00000002 0031002d 00000000  '........-.1.....')";
            check("-1".equals(BridgeClient.decodeParcelString(b)), "Did not decode -1");
        });
        test("parcel parser rejects malformed output", () ->
                check(BridgeClient.decodeParcelString("not a parcel") == null, "Malformed parcel accepted"));
        openApp();
        await(Shizuku::pingBinder, 12000, "Shizuku Binder unavailable");
        check(Shizuku.getUid() == 2000, "Tests require non-root Shizuku UID 2000");
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            runOnMainSync(() -> app.bridge.authorize());
            await(this::clickAllow, 12000, "Shizuku Allow all the time dialog not found");
            await(() -> app.bridge.authorized(), 12000, "Shizuku authorization not recognized");
        }
        test("Shizuku remote process actually runs as shell UID 2000", () -> {
            ShizukuShell.Result r = ShizukuShell.run(8000, "/system/bin/id", "-u");
            check(r.exitCode == 0, "id failed");
            check("2000".equals(r.out.trim()), "Remote process UID was " + r.out);
        });
        test("remote process can use Android service command", () -> {
            ShizukuShell.Result r = ShizukuShell.run(8000, "/system/bin/service", "list");
            check(r.out.contains("package:"), "service list did not contain package manager");
        });
        test("remote process returns command failures instead of false success", () -> {
            boolean failed = false;
            try { ShizukuShell.run(8000, "/system/bin/service", "call", "__nightkeys_missing_service__", "1"); }
            catch (IllegalStateException e) { failed = true; }
            check(failed, "Missing service command incorrectly succeeded");
        });
        test("Bridge correctly refuses emulator lacking Titan agui_common", () -> {
            app.bridge.connect();
            await(() -> app.bridge.status().contains("vendor check failed")
                    || app.bridge.status().contains("agui_common ready"), 12000, "Bridge check did not finish");
            check(!app.bridge.ready(), "Non-Titan emulator unexpectedly exposed agui_common");
            check(app.bridge.status().contains("agui_common"), "Failure did not identify agui_common");
        });
        test("production screen has exactly three setup steps and no consumer test controls", () -> {
            int[] headings = {com.everyonesniche.nightkeys.R.string.step_install,
                    com.everyonesniche.nightkeys.R.string.step_connect,
                    com.everyonesniche.nightkeys.R.string.step_enable};
            for (int i = 0; i < headings.length; i++)
                check(context.getString(headings[i]).startsWith(Integer.toString(i+1)), "Wrong step order");
            check(context.getResources().getIdentifier("test", "id", context.getPackageName()) == 0, "Timed check remains");
            check(context.getResources().getIdentifier("confirm_test", "id", context.getPackageName()) == 0, "Confirmation remains");
            check(context.getResources().getIdentifier("diag_hold", "id", context.getPackageName()) == 0,
                    "Diagnostic hold control leaked into the app");
            check(context.getResources().getIdentifier("diag_force_off", "id", context.getPackageName()) == 0,
                    "Diagnostic off control leaked into the app");
        });
    }

    private void reconnect() throws Exception {
        openApp();
        test("Shizuku authorization survives app force-stop/relaunch", () ->
                await(() -> app.bridge.authorized(), 12000, "Shizuku authorization not recognized after relaunch"));
        test("remote shell remains UID 2000 after relaunch", () ->
                check("2000".equals(ShizukuShell.run(8000, "/system/bin/id", "-u").out.trim()), "Wrong remote UID"));
    }

    private void openApp() {
        runOnMainSync(() -> context.startActivity(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
    }

    private boolean clickAllow() {
        AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
        return root != null && findAndClick(root);
    }

    private boolean findAndClick(AccessibilityNodeInfo node) {
        if ("Allow all the time".contentEquals(node.getText() == null ? "" : node.getText())
                && "moe.shizuku.privileged.api".contentEquals(
                        node.getPackageName() == null ? "" : node.getPackageName())) {
            AccessibilityNodeInfo target = node;
            for (int i = 0; i < 4 && target != null; i++, target = target.getParent())
                if (target.isClickable()) return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && findAndClick(child)) return true;
        }
        return false;
    }

    private void test(String name, Checked body) throws Exception {
        body.run();
        passed++;
        Bundle status = new Bundle();
        status.putString("stream", "\nPASS " + passed + ": " + name + "\n");
        sendStatus(0, status);
    }

    private static void await(Condition condition, long millis, String error) throws Exception {
        long end = SystemClock.elapsedRealtime() + millis;
        do {
            if (condition.get()) return;
            Thread.sleep(100);
        } while (SystemClock.elapsedRealtime() < end);
        throw new AssertionError(error);
    }

    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
}
