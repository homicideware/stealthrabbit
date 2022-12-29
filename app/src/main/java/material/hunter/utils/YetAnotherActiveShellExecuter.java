package material.hunter.utils;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class YetAnotherActiveShellExecuter {

    private final ExecutorService executor;
    private int endCode = 0;
    private OutputStream stdin;

    public YetAnotherActiveShellExecuter() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    private void init(String command) {
        onPrepare();
        executor.execute(() -> {
            String line;
            try {
                Process process = Runtime.getRuntime().exec("su -mm");
                stdin = process.getOutputStream();
                InputStream stdout = process.getInputStream();
                stdin.write((command + "\n").getBytes());
                stdin.flush();
                stdin.close();
                BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                while ((line = br.readLine()) != null) {
                    final String newLine = line;
                    new Handler(Looper.getMainLooper()).post(() -> onNewLine(newLine));
                }
                br.close();
                process.waitFor();
                process.destroy();
                endCode = process.exitValue();
            } catch (InterruptedException | IOException ignored) {
            }
            new Handler(Looper.getMainLooper()).post(() -> onFinished(endCode));
        });
    }

    public void exec(String command) {
        init(command);
    }

    public abstract void onPrepare();

    public abstract void onNewLine(String line);

    public abstract void onFinished(int code);
}
