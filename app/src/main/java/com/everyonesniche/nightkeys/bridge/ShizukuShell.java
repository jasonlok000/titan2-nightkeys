package com.everyonesniche.nightkeys.bridge;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import rikka.shizuku.Shizuku;

/**
 * Small compatibility wrapper around Shizuku's legacy remote-process primitive.
 *
 * Shizuku API 13 still ships a legacy remote-process implementation. Its Java
 * entry point is private; upstream recommends UserService for new apps. NightKeys' earlier
 * UserService transport did not reconnect on the target Titan firmware. For this
 * device-specific utility we therefore invoke that API implementation reflectively
 * and execute only fixed, allow-listed argv arrays. There is no `sh -c`, no caller
 * supplied command text and no interpolation of sensor/user data into a shell line.
 */
public final class ShizukuShell {
    public static final class Result {
        public final int exitCode;
        public final String out;
        public final String err;
        Result(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }
        public String combined() {
            if (err.isBlank()) return out;
            if (out.isBlank()) return err;
            return out + "\n" + err;
        }
    }

    private static final int LIMIT = 64 * 1024;
    private static volatile Method newProcess;

    private ShizukuShell() { }

    public static synchronized Result run(long timeoutMs, String... argv) {
        if (argv == null || argv.length == 0) throw new IllegalArgumentException("Missing command argv");
        if (timeoutMs < 100 || timeoutMs > 30_000) throw new IllegalArgumentException("Invalid command timeout");
        for (String arg : argv) {
            if (arg == null || arg.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid command argument");
        }
        Process process = null;
        StreamCollector stdout = null;
        StreamCollector stderr = null;
        try {
            process = start(argv);
            stdout = new StreamCollector(process.getInputStream(), "nightkeys-shell-stdout");
            stderr = new StreamCollector(process.getErrorStream(), "nightkeys-shell-stderr");
            stdout.start();
            stderr.start();
            Process awaited = process;
            FutureTask<Integer> waiter = new FutureTask<>(awaited::waitFor);
            Thread waitThread = new Thread(waiter, "nightkeys-shell-wait");
            waitThread.setDaemon(true);
            waitThread.start();
            int exit;
            try {
                exit = waiter.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                process.destroy();
                waiter.cancel(true);
                throw new IllegalStateException("Shizuku command timed out: " + argv[0], e);
            } catch (InterruptedException e) {
                process.destroy();
                waiter.cancel(true);
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Shizuku command", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw new IllegalStateException("Shizuku command wait failed: " + clean(cause), cause);
            }
            stdout.await(1000);
            stderr.await(1000);
            Result result = new Result(exit, stdout.text(), stderr.text());
            if (result.exitCode != 0) {
                String detail = result.combined().trim();
                throw new IllegalStateException("Shizuku command exited " + result.exitCode
                        + (detail.isEmpty() ? "" : ": " + detail));
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot execute privileged Shizuku command: " + clean(e), e);
        } finally {
            if (process != null) {
                try { process.getOutputStream().close(); } catch (Exception ignored) { }
            }
        }
    }

    private static Process start(String[] argv) throws Exception {
        Method method = newProcess;
        if (method == null) {
            synchronized (ShizukuShell.class) {
                method = newProcess;
                if (method == null) {
                    method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                    method.setAccessible(true);
                    newProcess = method;
                }
            }
        }
        try {
            Object value = method.invoke(null, new Object[]{argv, null, null});
            if (!(value instanceof Process)) throw new IllegalStateException("Unexpected Shizuku remote-process type");
            return (Process) value;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
    }

    private static final class StreamCollector {
        private final InputStream input;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final AtomicReference<Exception> error = new AtomicReference<>();
        private final CountDownLatch done = new CountDownLatch(1);
        private final Thread thread;
        private boolean truncated;

        StreamCollector(InputStream input, String name) {
            this.input = input;
            this.thread = new Thread(this::collect, name);
            this.thread.setDaemon(true);
        }
        void start() { thread.start(); }
        private void collect() {
            try (InputStream in = input) {
                byte[] buffer = new byte[2048];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    int keep = Math.min(count, Math.max(0, LIMIT - bytes.size()));
                    if (keep > 0) bytes.write(buffer, 0, keep);
                    if (keep < count) truncated = true;
                }
            } catch (Exception e) {
                error.set(e);
            } finally {
                done.countDown();
            }
        }
        void await(long millis) throws Exception {
            if (!done.await(millis, TimeUnit.MILLISECONDS))
                throw new IllegalStateException("Privileged command output did not finish");
            if (error.get() != null) throw error.get();
        }
        String text() {
            String value = bytes.toString(StandardCharsets.UTF_8).trim();
            return truncated ? value + " [output truncated]" : value;
        }
    }

    private static String clean(Throwable e) {
        String message = e == null ? null : e.getMessage();
        return message == null || message.isBlank() ? (e == null ? "unknown error" : e.getClass().getSimpleName()) : message;
    }
}
