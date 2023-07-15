package material.hunter.viewdata;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import material.hunter.R;
import material.hunter.SQL.CustomCommandsSQL;
import material.hunter.models.CustomCommandsModel;
import material.hunter.ui.activities.MainActivity;
import material.hunter.utils.AsynchronousExecutor;
import material.hunter.utils.NotificationUtil;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;
import material.hunter.utils.TerminalUtil;

public class CustomCommandsData {

    private static final int RETURN_SUCCESS = 100;
    private static final int RETURN_FAIL = 101;
    public static boolean isDataInitiated = false;
    private static CustomCommandsData instance;
    private final ArrayList<CustomCommandsModel> customCommandsModelArrayList = new ArrayList<>();
    private final MutableLiveData<List<CustomCommandsModel>> data = new MutableLiveData<>();
    private final List<CustomCommandsModel> copyOfCustomCommandsModelListFull = new ArrayList<>();
    public List<CustomCommandsModel> customCommandsModelListFull;

    public static synchronized CustomCommandsData getInstance() {
        if (instance == null) {
            instance = new CustomCommandsData();
        }
        return instance;
    }

    public MutableLiveData<List<CustomCommandsModel>> getCustomCommandsModels(Context context) {
        if (!isDataInitiated) {
            data.setValue(
                    CustomCommandsSQL.getInstance(context).bindData(customCommandsModelArrayList));
            customCommandsModelListFull = new ArrayList<>(data.getValue());
            isDataInitiated = true;
        }
        return data;
    }

    public MutableLiveData<List<CustomCommandsModel>> getCustomCommandsModels() {
        return data;
    }

    public void runCommandForItem(Activity activity, Context context, int position) {
        List<CustomCommandsModel> model = getInitCopyOfCustomCommandsModelListFull();

        new AsynchronousExecutor() {

            int returnValue = 0;
            boolean isChroot = false;

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    if (model.get(position).getMode().equals("interactive")) {
                        String command = model.get(position).getCommand();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            TerminalUtil terminal = new TerminalUtil(activity, context);
                            try {
                                terminal.runCommand(
                                        (model.get(position)
                                                .getEnv()
                                                .equals("android")
                                                ? command
                                                : PathsUtil.APP_SCRIPTS_PATH
                                                + "/bootroot_exec '"
                                                + command + "'"),
                                        false);
                            } catch (ActivityNotFoundException | PackageManager.NameNotFoundException e) {
                                terminal.showTerminalNotInstalledDialog();
                            } catch (SecurityException e) {
                                terminal.showPermissionDeniedDialog();
                            }
                        });
                    } else {
                        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                        Intent appIntent = new Intent(context, MainActivity.class);
                        PendingIntent pendingIntent =
                                PendingIntent.getActivity(
                                        context, 0, appIntent, NotificationUtil.setPendingIntentFlag());

                        Notification notification =
                                new NotificationCompat.Builder(context, "base")
                                        .setAutoCancel(false)
                                        .setSmallIcon(R.drawable.ic_stat_mh_notification)
                                        .setOnlyAlertOnce(true)
                                        .setStyle(
                                                new NotificationCompat.BigTextStyle().bigText(
                                                        "Command "
                                                                + model.get(position).getCommand()
                                                                + " "
                                                                + "is being run in background and in "
                                                                + model.get(position).getEnv()
                                                                + " environment."
                                                ))
                                        .setContentTitle("Custom Commands")
                                        .setContentText(
                                                "Command "
                                                        + "is being run in background and in "
                                                        + model.get(position).getEnv()
                                                        + " environment.")
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                                        .setContentIntent(pendingIntent)
                                        .build();
                        notificationManagerCompat.notify(1001, notification);
                        if (model.get(position).getEnv().equals("android")) {
                            returnValue =
                                    new ShellUtils()
                                            .executeCommandAsRootWithReturnCode(model.get(position).getCommand());
                        } else {
                            isChroot = true;
                            returnValue =
                                    new ShellUtils()
                                            .executeCommandAsChrootWithReturnCode(
                                                    model.get(position).getCommand());
                        }
                        if (returnValue == 0) {
                            returnValue = RETURN_SUCCESS;
                        } else {
                            returnValue = RETURN_FAIL;
                        }
                    }
                }
            }

            @Override
            public void onPostExecute() {
                updateCustomCommandsModelListFull(model);
                getCustomCommandsModels().getValue().clear();
                getCustomCommandsModels().getValue().addAll(model);
                getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
                if (returnValue != 0) {
                    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                    Intent appIntent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent =
                            PendingIntent.getActivity(
                                    context, 0, appIntent, NotificationUtil.setPendingIntentFlag());

                    Notification notification =
                            new NotificationCompat.Builder(context, "base")
                                    .setAutoCancel(false)
                                    .setSmallIcon(R.drawable.ic_stat_mh_notification)
                                    .setOnlyAlertOnce(true)
                                    .setStyle(
                                            new NotificationCompat.BigTextStyle().bigText(
                                                    "Return "
                                                            + (returnValue == RETURN_SUCCESS ? "success" : "error")
                                                            + ". "
                                                            + "Command "
                                                            + model.get(position).getCommand()
                                                            + " "
                                                            + "has been executed in "
                                                            + (isChroot ? "chroot" : "android")
                                                            + " environment."
                                            ))
                                    .setContentTitle("Custom Commands")
                                    .setContentText(
                                            "Return " + (returnValue == RETURN_SUCCESS ? "success" : "error") + ".")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setContentIntent(pendingIntent)
                                    .build();
                    notificationManagerCompat.notify(1001, notification);
                }
            }
        }.run();
    }

    public void editData(
            int position, ArrayList<String> dataArrayList, CustomCommandsSQL customCommandsSQL) {
        List<CustomCommandsModel> model = getInitCopyOfCustomCommandsModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    model.get(position).setLabel(dataArrayList.get(0));
                    model.get(position).setCommand(dataArrayList.get(1));
                    model.get(position).setEnv(dataArrayList.get(2));
                    model.get(position).setMode(dataArrayList.get(3));
                    model.get(position).setRunOnBoot(dataArrayList.get(4));
                    updateRunOnBootScripts(model);
                    customCommandsSQL.editData(position, dataArrayList);
                }
            }

            @Override
            public void onPostExecute() {
                updateCustomCommandsModelListFull(model);
                getCustomCommandsModels().getValue().clear();
                getCustomCommandsModels().getValue().addAll(model);
                getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
            }
        }.run();
    }

    public void addData(
            int position, ArrayList<String> dataArrayList, CustomCommandsSQL customCommandsSQL) {
        List<CustomCommandsModel> model = getInitCopyOfCustomCommandsModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    model.add(
                            position - 1,
                            new CustomCommandsModel(
                                    dataArrayList.get(0),
                                    dataArrayList.get(1),
                                    dataArrayList.get(2),
                                    dataArrayList.get(3),
                                    dataArrayList.get(4)));
                    if (dataArrayList.get(4).equals("1")) {
                        updateRunOnBootScripts(model);
                    }
                    customCommandsSQL.addData(position, dataArrayList);
                }
            }

            @Override
            public void onPostExecute() {
                updateCustomCommandsModelListFull(model);
                getCustomCommandsModels().getValue().clear();
                getCustomCommandsModels().getValue().addAll(model);
                getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
            }
        }.run();
    }

    public void deleteData(
            ArrayList<Integer> selectedPositionList,
            ArrayList<Integer> selectedTargetIds,
            CustomCommandsSQL customCommandsSQL) {
        List<CustomCommandsModel> model = getInitCopyOfCustomCommandsModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    selectedPositionList.sort(Collections.reverseOrder());
                    for (Integer s : selectedPositionList) {
                        model.remove(s.intValue());
                    }
                    customCommandsSQL.deleteData(selectedTargetIds);
                }
            }

            @Override
            public void onPostExecute() {
                updateCustomCommandsModelListFull(model);
                getCustomCommandsModels().getValue().clear();
                getCustomCommandsModels().getValue().addAll(model);
                getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
            }
        }.run();
    }

    public void moveData(
            int originalPositionIndex,
            int targetPositionIndex,
            CustomCommandsSQL customCommandsSQL) {
        List<CustomCommandsModel> model = getInitCopyOfCustomCommandsModelListFull();

        new AsynchronousExecutor() {

            int _targetPositionIndex = targetPositionIndex;

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    CustomCommandsModel tempCustomCommandsModel =
                            new CustomCommandsModel(
                                    model.get(originalPositionIndex).getLabel(),
                                    model.get(originalPositionIndex).getCommand(),
                                    model.get(originalPositionIndex).getEnv(),
                                    model.get(originalPositionIndex).getMode(),
                                    model.get(originalPositionIndex).getRunOnBoot());
                    model.remove(originalPositionIndex);
                    if (originalPositionIndex < targetPositionIndex) {
                        _targetPositionIndex = targetPositionIndex - 1;
                    }
                    model.add(targetPositionIndex, tempCustomCommandsModel);
                    customCommandsSQL.moveData(originalPositionIndex, targetPositionIndex);
                }
            }

            @Override
            public void onPostExecute() {
                updateCustomCommandsModelListFull(model);
                getCustomCommandsModels().getValue().clear();
                getCustomCommandsModels().getValue().addAll(model);
                getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
            }
        }.run();
    }

    public String backupData(@NonNull CustomCommandsSQL customCommandsSQL, String storedDBpath) {
        return customCommandsSQL.backupData(storedDBpath);
    }

    public String restoreData(@NonNull CustomCommandsSQL customCommandsSQL, String storedDBpath) {
        String returnedResult = customCommandsSQL.restoreData(storedDBpath);

        if (returnedResult == null) {
            new AsynchronousExecutor() {

                List<CustomCommandsModel> model = getInitCopyOfCustomCommandsModelListFull();

                @Override
                public void onPreExecute() {
                }

                @Override
                public void doInBackground() {
                    if (model != null) {
                        model.clear();
                        model = customCommandsSQL.bindData((ArrayList<CustomCommandsModel>) model);
                    }
                }

                @Override
                public void onPostExecute() {
                    updateCustomCommandsModelListFull(model);
                    getCustomCommandsModels().getValue().clear();
                    getCustomCommandsModels().getValue().addAll(model);
                    getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
                }
            }.run();
            return null;
        } else {
            return returnedResult;
        }
    }

    public void resetData(CustomCommandsSQL customCommandsSQL) {
        new AsynchronousExecutor() {

            List<CustomCommandsModel> model = getInitCopyOfCustomCommandsModelListFull();

            @Override
            public void onPreExecute() {
                customCommandsSQL.resetData();
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    model.clear();
                    model = customCommandsSQL.bindData((ArrayList<CustomCommandsModel>) model);
                }
            }

            @Override
            public void onPostExecute() {
                updateCustomCommandsModelListFull(model);
                getCustomCommandsModels().getValue().clear();
                getCustomCommandsModels().getValue().addAll(model);
                getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
            }
        }.run();
    }

    public void updateCustomCommandsModelListFull(
            List<CustomCommandsModel> copyOfCustomCommandsModelList) {
        customCommandsModelListFull.clear();
        customCommandsModelListFull.addAll(copyOfCustomCommandsModelList);
    }

    private List<CustomCommandsModel> getInitCopyOfCustomCommandsModelListFull() {
        copyOfCustomCommandsModelListFull.clear();
        copyOfCustomCommandsModelListFull.addAll(customCommandsModelListFull);
        return copyOfCustomCommandsModelListFull;
    }

    private void updateRunOnBootScripts(@NonNull List<CustomCommandsModel> model) {
        StringBuilder tmpStringBuilder = new StringBuilder();
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).getRunOnBoot().equals("1")) {
                tmpStringBuilder
                        .append(model.get(i).getEnv())
                        .append(" ")
                        .append(model.get(i).getCommand())
                        .append("\n");
            }
        }
        new ShellUtils()
                .executeCommandAsRootWithOutput(
                        "cat << 'EOF' > "
                                + PathsUtil.APP_SCRIPTS_PATH
                                + "/init-custom-commands"
                                + "\n"
                                + tmpStringBuilder
                                + "\nEOF");
    }
}