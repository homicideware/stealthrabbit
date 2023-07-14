package material.hunter.viewdata;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import material.hunter.SQL.CustomCommandsSQL;
import material.hunter.SQL.ServicesSQL;
import material.hunter.models.CustomCommandsModel;
import material.hunter.models.ServicesModel;
import material.hunter.utils.AsynchronousExecutor;
import material.hunter.utils.PathsUtil;
import material.hunter.utils.ShellUtils;

public class ServicesData {

    public static boolean isDataInitiated = false;
    private static ServicesData instance;
    private final ArrayList<ServicesModel> servicesModelArrayList = new ArrayList<>();
    private final MutableLiveData<List<ServicesModel>> data = new MutableLiveData<>();
    private final List<ServicesModel> copyOfServicesModelListFull = new ArrayList<>();
    public List<ServicesModel> servicesModelListFull;
    private Activity activity;

    public static synchronized ServicesData getInstance() {
        if (instance == null) {
            instance = new ServicesData();
        }
        return instance;
    }

    public MutableLiveData<List<ServicesModel>> getServicesModels(Activity activity, Context context) {
        this.activity = activity;
        if (!isDataInitiated) {
            data.setValue(ServicesSQL.getInstance(context).bindData(servicesModelArrayList));
            servicesModelListFull = new ArrayList<>(data.getValue());
            isDataInitiated = true;
        }
        return data;
    }

    public MutableLiveData<List<ServicesModel>> getServicesModels() {
        return data;
    }

    public void refreshData() {
        List<ServicesModel> model = getInitCopyOfServicesModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    for (int i = 0; i < model.size(); i++) {
                        model.get(i)
                                .setStatus(
                                        new ShellUtils()
                                                .executeCommandAsRootWithReturnCode(
                                                        PathsUtil.BUSYBOX
                                                                + " ps | grep -v"
                                                                + " grep | grep '"
                                                                + model.get(i)
                                                                .getCommandForCheckingService()
                                                                + "'")
                                                == 0
                                                ? "[+] Service is running!"
                                                : "[-] Service isn't running.");
                    }
                }
            }

            @Override
            public void onPostExecute() {
                getServicesModels().getValue().clear();
                getServicesModels().getValue().addAll(model);
                getServicesModels().postValue(getServicesModels().getValue());
            }
        }.run();
    }

    public void startServiceForItem(int position, MaterialSwitch mSwitch) {
        List<ServicesModel> model = getInitCopyOfServicesModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
                mSwitch.setEnabled(false);
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    model.get(position)
                            .setStatus(
                                    new ShellUtils()
                                            .executeCommandAsChrootWithReturnCode(
                                                    model.get(position)
                                                            .getCommandForStartingService())
                                            == 0
                                            ? "[+] Service is running!"
                                            : "[-] Service isn't running.");
                }
            }

            @Override
            public void onPostExecute() {
                mSwitch.setEnabled(true);
                mSwitch.setChecked(model.get(position).getStatus().startsWith("[+]"));
                getServicesModels().getValue().clear();
                getServicesModels().getValue().addAll(model);
                getServicesModels().postValue(getServicesModels().getValue());
                if (!mSwitch.isChecked())
                    PathsUtil.showSnackBar(
                            activity,
                            "Failed starting "
                                    + getServicesModels().getValue().get(position).getLabel()
                                    + " service.",
                            false);
            }
        }.run();
    }

    public void stopServiceForItem(int position, MaterialSwitch mSwitch, Context context) {
        List<ServicesModel> model = getInitCopyOfServicesModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
                mSwitch.setEnabled(false);
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    model.get(position)
                            .setStatus(
                                    new ShellUtils()
                                            .executeCommandAsChrootWithReturnCode(
                                                    model.get(position)
                                                            .getCommandForStoppingService())
                                            == 0
                                            ? "[-] Service isn't running."
                                            : "[+] Service is running!");
                }
            }

            @Override
            public void onPostExecute() {
                mSwitch.setEnabled(true);
                mSwitch.setChecked(model.get(position).getStatus().startsWith("[+]"));
                getServicesModels().getValue().clear();
                getServicesModels().getValue().addAll(model);
                getServicesModels().postValue(getServicesModels().getValue());
                if (mSwitch.isChecked())
                    PathsUtil.showSnackBar(
                            activity,
                            "Failed to stop "
                                    + getServicesModels().getValue().get(position).getLabel()
                                    + " service.",
                            false);
            }
        }.run();
    }

    public void editData(int position, ArrayList<String> dataArrayList, ServicesSQL servicesSQL) {
        List<ServicesModel> model = getInitCopyOfServicesModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    model.get(position).setServiceName(dataArrayList.get(0));
                    model.get(position).setCommandForStartingService(dataArrayList.get(1));
                    model.get(position).setCommandForStoppingService(dataArrayList.get(2));
                    model.get(position).setCommandForCheckingService(dataArrayList.get(3));
                    model.get(position).setRunOnChrootStart(dataArrayList.get(4));
                    updateRunOnChrootStartScripts(model);
                    servicesSQL.editData(position, dataArrayList);
                }
            }

            @Override
            public void onPostExecute() {
                updateServicesModelListFull(model);
                getServicesModels().getValue().clear();
                getServicesModels().getValue().addAll(model);
                getServicesModels().postValue(getServicesModels().getValue());
            }
        }.run();
    }

    public void addData(int position, ArrayList<String> dataArrayList, ServicesSQL servicesSQL) {
        List<ServicesModel> model = getInitCopyOfServicesModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    model.add(
                            position - 1,
                            new ServicesModel(
                                    dataArrayList.get(0),
                                    dataArrayList.get(1),
                                    dataArrayList.get(2),
                                    dataArrayList.get(3),
                                    dataArrayList.get(4),
                                    ""));
                    if (dataArrayList.get(4).equals("1")) {
                        updateRunOnChrootStartScripts(model);
                    }
                    servicesSQL.addData(position, dataArrayList);
                }
            }

            @Override
            public void onPostExecute() {
                updateServicesModelListFull(model);
                getServicesModels().getValue().clear();
                getServicesModels().getValue().addAll(model);
                getServicesModels().postValue(getServicesModels().getValue());
            }
        }.run();
    }

    public void deleteData(
            ArrayList<Integer> selectedPositionList,
            ArrayList<Integer> selectedTargetIds,
            ServicesSQL servicesSQL) {
        List<ServicesModel> model = getInitCopyOfServicesModelListFull();

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
                    servicesSQL.deleteData(selectedTargetIds);
                }
            }

            @Override
            public void onPostExecute() {
                updateServicesModelListFull(model);
                getServicesModels().getValue().clear();
                getServicesModels().getValue().addAll(model);
                getServicesModels().postValue(getServicesModels().getValue());
            }
        }.run();
    }

    public void moveData(
            final int originalPositionIndex, final int targetPositionIndex, ServicesSQL servicesSQL) {
        List<ServicesModel> model = getInitCopyOfServicesModelListFull();

        new AsynchronousExecutor() {

            @Override
            public void onPreExecute() {
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    int mTargetPositionIndex = targetPositionIndex;
                    ServicesModel tempServicesModel =
                            new ServicesModel(
                                    model.get(originalPositionIndex).getLabel(),
                                    model.get(originalPositionIndex).getCommandForStartingService(),
                                    model.get(originalPositionIndex).getCommandForStoppingService(),
                                    model.get(originalPositionIndex)
                                            .getCommandForCheckingService(),
                                    model.get(originalPositionIndex).getRunOnChrootStart(),
                                    model.get(originalPositionIndex).getStatus());
                    model.remove(originalPositionIndex);
                    if (originalPositionIndex < targetPositionIndex) {
                        mTargetPositionIndex = targetPositionIndex - 1;
                    }
                    model.add(mTargetPositionIndex, tempServicesModel);
                    servicesSQL.moveData(originalPositionIndex, targetPositionIndex);
                }
            }

            @Override
            public void onPostExecute() {
                updateServicesModelListFull(model);
                getServicesModels().getValue().clear();
                getServicesModels().getValue().addAll(model);
                getServicesModels().postValue(getServicesModels().getValue());
            }
        }.run();
    }

    public String backupData(@NonNull ServicesSQL servicesSQL, String storedDBpath) {
        return servicesSQL.backupData(storedDBpath);
    }

    public String restoreData(@NonNull ServicesSQL servicesSQL, String storedDBpath) {
        String returnedResult = servicesSQL.restoreData(storedDBpath);

        if (returnedResult == null) {
            new AsynchronousExecutor() {

                final List<ServicesModel> model = getInitCopyOfServicesModelListFull();

                @Override
                public void onPreExecute() {
                }

                @Override
                public void doInBackground() {
                    if (model != null) {
                        model.clear();
                        servicesSQL.bindData((ArrayList<ServicesModel>) model);
                    }
                }

                @Override
                public void onPostExecute() {
                    updateServicesModelListFull(model);
                    getServicesModels().getValue().clear();
                    getServicesModels().getValue().addAll(model);
                    getServicesModels().postValue(getServicesModels().getValue());
                    refreshData();
                }
            }.run();
            return null;
        } else {
            return returnedResult;
        }
    }

    public void resetData(ServicesSQL servicesSQL) {
        new AsynchronousExecutor() {

            List<ServicesModel> model = getInitCopyOfServicesModelListFull();

            @Override
            public void onPreExecute() {
                servicesSQL.resetData();
            }

            @Override
            public void doInBackground() {
                if (model != null) {
                    model.clear();
                    model = servicesSQL.bindData((ArrayList<ServicesModel>) model);
                }
            }

            @Override
            public void onPostExecute() {
                updateServicesModelListFull(model);
                getServicesModels().getValue().clear();
                getServicesModels().getValue().addAll(model);
                getServicesModels().postValue(getServicesModels().getValue());
                refreshData();
            }
        }.run();
    }

    public void updateServicesModelListFull(List<ServicesModel> copyOfServicesModelList) {
        servicesModelListFull.clear();
        servicesModelListFull.addAll(copyOfServicesModelList);
    }

    private List<ServicesModel> getInitCopyOfServicesModelListFull() {
        copyOfServicesModelListFull.clear();
        copyOfServicesModelListFull.addAll(servicesModelListFull);
        return copyOfServicesModelListFull;
    }

    private void updateRunOnChrootStartScripts(@NonNull List<ServicesModel> servicesModelList) {
        StringBuilder tmpStringBuilder = new StringBuilder();
        for (int i = 0; i < servicesModelList.size(); i++) {
            if (servicesModelList.get(i).getRunOnChrootStart().equals("1")) {
                tmpStringBuilder
                        .append(servicesModelList.get(i).getCommandForStartingService())
                        .append("\n");
            }
        }
        new ShellUtils()
                .executeCommandAsRootWithOutput(
                        "cat << 'EOF' > "
                                + PathsUtil.APP_SCRIPTS_PATH
                                + "/init-services"
                                + "\n"
                                + tmpStringBuilder
                                + "\nEOF");
    }
}