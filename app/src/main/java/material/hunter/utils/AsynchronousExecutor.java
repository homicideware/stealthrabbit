package material.hunter.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AsynchronousExecutor {

    private final ExecutorService executor;

    public AsynchronousExecutor() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    private void startBackground() {
        onPreExecute();
        executor.execute(() -> {
            doInBackground();
            new Handler(Looper.getMainLooper()).post(this::onPostExecute);
        });
    }

    public void run() {
        startBackground();
    }

    public abstract void onPreExecute();

    public abstract void doInBackground();

    public abstract void onPostExecute();
}