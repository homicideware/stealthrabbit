package material.hunter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import material.hunter.services.RunAtBootService;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent service = new Intent(context, RunAtBootService.class);
                context.startService(service);
            }
        }
    }
}