package org.homicideware.stealthrabbit.ui.activities.menu;

import static org.homicideware.stealthrabbit.utils.Utils.setErrorListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.homicideware.stealthrabbit.BuildConfig;
import org.homicideware.stealthrabbit.R;
import org.homicideware.stealthrabbit.SQL.USBArmorySQL;
import org.homicideware.stealthrabbit.databinding.ActionbarLayoutBinding;
import org.homicideware.stealthrabbit.databinding.UsbarmoryActivityBinding;
import org.homicideware.stealthrabbit.models.USBArmorySwitchModel;
import org.homicideware.stealthrabbit.ui.activities.ThemedActivity;
import org.homicideware.stealthrabbit.utils.PathsUtil;
import org.homicideware.stealthrabbit.utils.ShellUtils;

public class USBArmoryActivity extends ThemedActivity {

    private UsbarmoryActivityBinding binding;
    private ExecutorService executor;
    private SharedPreferences prefs;

    @SuppressLint({"SetTextI18n", "CommitPrefEdits"})
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = UsbarmoryActivityBinding.inflate(getLayoutInflater());
        executor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        setContentView(binding.getRoot());

        ActionbarLayoutBinding included = binding.included;
        setSupportActionBar(included.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        File sql_folder = new File(PathsUtil.APP_SD_SQLBACKUP_PATH);
        if (!sql_folder.exists()) {
            try {
                if (sql_folder.mkdir()) ;
                PathsUtil.showSnackBar(
                        this, binding.getRoot(), "Created directory for backing up config.", false);
            } catch (Exception e) {
                e.printStackTrace();
                PathsUtil.showSnackBar(
                        this, binding.getRoot(), "Failed to create directory " + PathsUtil.APP_SD_SQLBACKUP_PATH, false);
            }
        }

        binding.targetOsTitle.setEnabled(true);
        binding.functionsTitle.setEnabled(true);
        binding.targetOs.setEnabled(true);
        binding.functions.setEnabled(true);

        binding.targetOsLayout.setOnClickListener(v -> {
            String[] os = new String[]{
                    "Windows",
                    "Linux",
                    "Mac OS"
            };
            String[] selectedOs = {prefs.getString("usbarmory_target_os", "Windows")};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Target OS")
                    .setSingleChoiceItems(os, Arrays.asList(os).indexOf(selectedOs[0]), (di, position) -> selectedOs[0] = os[position])
                    .setPositiveButton(android.R.string.ok, (di, i) -> {
                        prefs.edit().putString("usbarmory_target_os", selectedOs[0]).apply();
                        binding.targetOs.setText(selectedOs[0]);
                        setDeviceInformation();
                    })
                    .setNegativeButton(android.R.string.cancel, (di, i) -> {
                    })
                    .show();
        });

        binding.functionsOsLayout.setOnClickListener(v -> {
            boolean isMacOs = binding.targetOs.getText().toString().equals("Mac OS");
            String readFunctionsList = getUSBFunctions();
            String[] functions = new String[]{
                    "hid",
                    "mass_storage",
                    isMacOs ? "acm,ecm" : "rndis"
            };
            final boolean[] functionsBooleans = new boolean[]{
                    readFunctionsList.contains("hid"),
                    readFunctionsList.contains("mass_storage"),
                    readFunctionsList.matches(".*(rndis|acm|ecm).*")
            };
            final StringBuilder result = new StringBuilder();
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Functions")
                    .setMultiChoiceItems(functions, functionsBooleans, (di, position, bool) -> functionsBooleans[position] = bool)
                    .setPositiveButton(android.R.string.ok, (di, i) -> {
                        for (int a = 0; a < functions.length; a++) {
                            result.append(functionsBooleans[a] ? "," + functions[a] : "");
                        }
                        try {
                            String functionsResult = result.substring(1);
                            binding.functions.setText(functionsResult);
                            setDeviceInformation();
                            binding.adbSwitchLayout.setEnabled(true);
                            binding.adbSwitch.setEnabled(true);
                        } catch (StringIndexOutOfBoundsException e) {
                            binding.functions.setText("reset");
                            setDeviceInformation();
                            binding.adbSwitchLayout.setEnabled(false);
                            binding.adbSwitch.setEnabled(false);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (di, i) -> {
                    })
                    .show();
        });

        binding.adbSwitchLayout.setOnClickListener(v -> {
            binding.adbSwitch.toggle();
            setDeviceInformation();
        });

        binding.targetOs.setText(prefs.getString("usbarmory_target_os", "Windows"));

        binding.update.setOnClickListener(v -> executor.execute(() -> {
            String enabledUsbFunctions = getEnabledUSBFunctions();
            new Handler(Looper.getMainLooper()).post(() -> {
                setDeviceInformation();
                if (enabledUsbFunctions == null) {
                    binding.functions.setText("Nothing.");
                } else {
                    binding.functions.setText(enabledUsbFunctions);
                    if (enabledUsbFunctions.equals("adb")) {
                        binding.adbSwitchLayout.setEnabled(false);
                        binding.adbSwitch.setEnabled(false);
                    } else {
                        binding.adbSwitch.setChecked(enabledUsbFunctions.contains("adb"));
                    }
                    if (enabledUsbFunctions.contains("mass_storage")) {
                        binding.imageMounterCard.setVisibility(View.VISIBLE);
                        executor.execute(() -> {
                            boolean isRoEnabled = new ShellUtils().executeCommandAsRootWithOutput(
                                    "cat /config/usb_gadget/g1/functions/mass_storage.0/lun.0/ro").equals("1");
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (isRoEnabled) {
                                    binding.imageReadOnlySwitch.setChecked(true);
                                }
                            });
                        });
                    } else {
                        binding.imageMounterCard.setVisibility(View.GONE);
                    }
                }
            });
        }));

        binding.setUsbFunctions.setOnClickListener(v -> {
            if (isUSBSystemInfoValid()) {
                binding.setUsbFunctions.setEnabled(false);
                String target =
                        getTargetOS().equals("Windows")
                                ? "win"
                                : getTargetOS().equals("Linux")
                                ? "lnx"
                                : getTargetOS().equals("Mac OS")
                                ? "mac"
                                : "";
                String functions = getUSBFunctions();
                String manufacturer =
                        binding.manufacturer.getText().toString().isEmpty()
                                ? ""
                                : " -m '"
                                + binding.manufacturer.getText().toString()
                                + "'";
                String product =
                        binding.product.getText().toString().isEmpty()
                                ? ""
                                : " -P '"
                                + binding.product.getText().toString()
                                + "'";
                String serialnumber =
                        binding.serialnumber.getText().toString().isEmpty()
                                ? ""
                                : " -s '"
                                + binding.serialnumber.getText().toString()
                                + "'";

                executor.execute(() -> {
                    int result = new ShellUtils().executeCommandAsRootWithReturnCode(
                            PathsUtil.APP_SCRIPTS_PATH
                                    + "/usbarmory -t '"
                                    + target
                                    + "' -f '"
                                    + functions
                                    + "' -v '"
                                    + binding.idVendorLayout.getPrefixText().toString()
                                    + binding.idVendor.getText().toString()
                                    + "' -p '"
                                    + binding.idProductLayout.getPrefixText().toString()
                                    + binding.idProduct.getText().toString()
                                    + "' "
                                    + manufacturer
                                    + product
                                    + serialnumber);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (result != 0) {
                            PathsUtil.showSnackBar(
                                    this,
                                    "Failed to set USB function.",
                                    false);
                        } else {
                            PathsUtil.showSnackBar(
                                    this,
                                    "USB function set successfully!",
                                    false);
                        }
                        binding.update.performClick();
                        binding.setUsbFunctions.setEnabled(true);
                    });
                });
            }
        });

        binding.saveConfig.setOnClickListener(v -> {
            if (isUSBSystemInfoValid()) {
                ArrayList<TextInputEditText> arrayList = new ArrayList<>();
                arrayList.add(binding.idVendor);
                arrayList.add(binding.idProduct);
                arrayList.add(binding.manufacturer);
                arrayList.add(binding.product);
                arrayList.add(binding.serialnumber);
                for (int i = 0; i < arrayList.size(); i++) {
                    if (!USBArmorySQL.getInstance(this)
                            .setUSBSwitchColumnData(
                                    getUSBFunctions(),
                                    i + 2,
                                    getTargetOS(),
                                    arrayList.get(i).getText().toString())) {
                        PathsUtil.showSnackBar(this, "Something wrong when processing key " + i, false);
                    }
                    PathsUtil.showSnackBar(this, "Done!", false);
                }
            }
        });

        binding.imageMount.setOnClickListener(v -> {
            if (binding.images.getText().toString().isEmpty()) {
                PathsUtil.showSnackBar(this, "No image file is selected.", false);
            } else {
                binding.imageMount.setEnabled(false);
                binding.imageUmount.setEnabled(false);
                executor.execute(() -> {
                    int result;
                    if (binding.imageReadOnlySwitch.isChecked())
                        result = new ShellUtils().executeCommandAsRootWithReturnCode(
                                String.format(
                                        "%s%s && echo '%s/%s' >"
                                                + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/file",
                                        "echo '1' >"
                                                + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/ro",
                                        binding.images.getText().toString().contains(".iso")
                                                ? " && echo '1' >"
                                                + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/cdrom"
                                                : " && echo '0' >"
                                                + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/cdrom",
                                        PathsUtil.APP_SD_FILES_IMG_PATH,
                                        binding.images.getText().toString()));
                    else
                        result = new ShellUtils().executeCommandAsRootWithReturnCode(
                                String.format(
                                        "%s%s && echo '%s/%s' >"
                                                + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/file",
                                        "echo '0' >"
                                                + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/ro",
                                        binding.images.getText().toString().contains(".iso")
                                                ? " && echo '1' >"
                                                + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/cdrom"
                                                : " && echo '0' >"
                                                + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/cdrom",
                                        PathsUtil.APP_SD_FILES_IMG_PATH,
                                        binding.images.getText().toString()));
                    new Handler(Looper.getMainLooper()).post(() -> {
                        boolean selectedImageAlreadyMounted = binding.images.getText().toString().equals(binding.mountedImage.getText().toString());
                        if (result == 0) {
                            PathsUtil.showSnackBar(
                                    this, binding.images.getText().toString() + " has been mounted!", false);
                        } else {
                            /*if (selectedImageAlreadyMounted) {
                                PathsUtil.showSnackBar(this, "Umount the current image for this action.", false);
                            } else {
                                executor.execute(() -> {
                                    int umountImage = umountImage();
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (umountImage == 0) {
                                            binding.imageMount.setEnabled(true);
                                            binding.imageMount.performClick();
                                        } else {*/
                            PathsUtil.showSnackBar(this, "Failed to mount " + binding.images.getText().toString(), false);
                                        /*}
                                    });
                                });
                            }*/
                        }
                        binding.updateImagesLayout.performClick();
                    });
                });
            }
        });

        binding.imageUmount.setOnClickListener(v -> {
            binding.imageMount.setEnabled(false);
            binding.imageUmount.setEnabled(false);
            executor.execute(() -> {
                int result = umountImage();
                if (result == 0) {
                    PathsUtil.showSnackBar(
                            this, binding.mountedImage.getText().toString() + " has been umounted.", false);
                } else {
                    PathsUtil.showToast(
                            this,
                            "Failed to umount image "
                                    + binding.mountedImage.getText().toString()
                                    + ". Your drive may be still be in use by the"
                                    + " host, please eject your drive on the host"
                                    + " first, and then try to umount the image"
                                    + " again.",
                            true);
                }
                new Handler(Looper.getMainLooper()).post(() -> binding.updateImagesLayout.performClick());
            });
        });

        binding.imageReadOnlyLayout.setOnClickListener(v -> {
            if (binding.mountedImage.getText().toString().equals("No image mounted.")) {
                binding.imageReadOnlySwitch.toggle();
            } else {
                PathsUtil.showSnackBar(this, "Umount the current image for this action.", false);
            }
        });

        binding.updateImagesLayout.setOnClickListener(v -> executor.execute(() -> {
            String result =
                    new ShellUtils().executeCommandAsRootWithOutput(
                            "basename $(cat /config/usb_gadget/g1/functions/mass_storage.0/lun.0/file)");
            new Handler(Looper.getMainLooper()).post(() -> {
                if (result.equals("")) {
                    binding.mountedImage.setText("No image mounted.");
                } else {
                    binding.mountedImage.setText(result);
                }
                if (result.equals(binding.images.getText().toString())) {
                    binding.imageMount.setEnabled(false);
                    binding.imageUmount.setEnabled(true);
                } else {
                    binding.imageMount.setEnabled(true);
                    binding.imageUmount.setEnabled(false);
                }
            });
        }));

        binding.images.setOnItemClickListener((parent, view, position, id) -> {
            boolean selectedImageAlreadyMounted = binding.images.getText().toString().equals(binding.mountedImage.getText().toString());
            binding.imageMount.setEnabled(!selectedImageAlreadyMounted);
            binding.imageUmount.setEnabled(selectedImageAlreadyMounted);
        });

        setErrorListener(binding.idVendor, binding.idVendorLayout, "^[A-z0-9]{4}$", "The id vendor must be matches ^0x[A-z0-9]{4}$");
        setErrorListener(binding.idProduct, binding.idProductLayout, "^[A-z0-9]{4}$", "The id product must be matches ^0x[A-z0-9]{4}$");
        setErrorListener(binding.manufacturer, binding.manufacturerLayout, "^[A-z0-9\\- ]+$|^$", "The manufacturer must be matches ^[A-z0-9\\- ]+$");
        setErrorListener(binding.serialnumber, binding.serialnumberLayout, "^[A-z0-9]+$|^$", "The serial number must be matches ^[A-z0-9]+$");
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.update.performClick();
        setDeviceInformation();
        binding.updateImagesLayout.performClick();
        getImagesList();
    }

    private int umountImage() {
        return new ShellUtils().executeCommandAsRootWithReturnCode(
                "echo '' >"
                        + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/file"
                        + " && echo '0' >"
                        + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/ro"
                        + " && echo '0' >"
                        + " /config/usb_gadget/g1/functions/mass_storage.0/lun.0/cdrom");
    }

    private void getImagesList() {
        binding.imageMount.setEnabled(false);
        binding.imageUmount.setEnabled(false);
        ArrayList<String> result = new ArrayList<>();
        File image_folder = new File(PathsUtil.APP_SD_FILES_IMG_PATH);
        if (!image_folder.exists()) {
            PathsUtil.showSnackBar(this, "Creating directory for storing image files...", false);
            try {
                if (image_folder.mkdir()) ;
            } catch (Exception e) {
                e.printStackTrace();
                PathsUtil.showSnackBar(this, "Failed to get images files.", false);
                return;
            }
        }
        try {
            File[] filesInFolder = image_folder.listFiles();
            assert filesInFolder != null;
            for (File file : filesInFolder) {
                if (!file.isDirectory()) {
                    if (file.getName().matches(".*\\.(img|iso)$")) {
                        result.add(file.getName());
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> imageAdapter = new ArrayAdapter<>(this, R.layout.sr_spinner_item, result);
        binding.images.setAdapter(imageAdapter);

        if (result.size() > 0) binding.images.setText(result.get(0), false);
        binding.imageMount.setEnabled(true);
        binding.imageUmount.setEnabled(true);
    }

    @NonNull
    private String getTargetOS() {
        return binding.targetOs.getText().toString();
    }

    @Nullable
    private String getEnabledUSBFunctions() {
        String result =
                new ShellUtils()
                        .executeCommandAsRootWithOutput(
                                "for i in $(find /config/usb_gadget/g1/configs/b.1 -type l -exec readlink -e {} \\;); do basename $i; done | xargs");
        if (result.isEmpty()) {
            return null;
        } else {
            return result.replace("ffs.adb", "adb");
        }
    }

    @NonNull
    private String getUSBFunctions() {
        String functionsList = binding.functions.getText().toString();
        if (functionsList.isEmpty() || functionsList.equals("Nothing.")) {
            return "reset";
        } else {
            StringBuilder functions = new StringBuilder();
            if (functionsList.equals("adb") || functionsList.equals("reset")) {
                return "reset";
            } else {
                if (functionsList.contains("hid")) {
                    functions.append(",hid");
                }
                if (functionsList.contains("mass_storage")) {
                    functions.append(",mass_storage");
                }
                if (functionsList.contains("rndis")) {
                    functions.append(",rndis");
                }
                if (functionsList.contains("acm") || functionsList.contains("ecm")) {
                    functions.append(",acm,ecm");
                }
                if (binding.adbSwitch.isChecked()) {
                    functions.append(",adb");
                }
            }
            return functions.substring(1);
        }
    }

    private void setDeviceInformation() {
        executor.execute(() -> {
            USBArmorySwitchModel result =
                    USBArmorySQL.getInstance(this)
                            .getUSBSwitchColumnData(getTargetOS(), getUSBFunctions());
            String manufacturer =
                    TextUtils.isEmpty((result).getManufacturer())
                            ? new ShellUtils().executeCommandAsRootWithOutput(
                            "cat /config/usb_gadget/g1/strings/0x409/manufacturer")
                            : (result).getManufacturer();
            String product =
                    TextUtils.isEmpty((result).getProduct())
                            ? new ShellUtils().executeCommandAsRootWithOutput(
                            "cat /config/usb_gadget/g1/strings/0x409/product")
                            : (result).getProduct();
            String serialnumber =
                    TextUtils.isEmpty((result).getSerialnumber())
                            ? new ShellUtils().executeCommandAsRootWithOutput(
                            "cat /config/usb_gadget/g1/strings/0x409/serialnumber")
                            : (result).getSerialnumber();
            new Handler(Looper.getMainLooper()).post(() -> {
                binding.idVendor.setText((result).getIdVendor());
                binding.idProduct.setText((result).getIdProduct());
                binding.manufacturer.setText(manufacturer);
                binding.product.setText(product);
                binding.serialnumber.setText(serialnumber);
            });
        });
    }

    private boolean isUSBSystemInfoValid() {
        boolean valid = true;
        if (!binding.idVendor.getText().toString().matches("^[A-z0-9]{4}$")) {
            valid = false;
        }
        if (!binding.idProduct.getText().toString().matches("^[A-z0-9]{4}$")) {
            valid = false;
        }
        if (!binding.manufacturer.getText().toString().matches("^[A-z0-9\\- ]+$|^$")) {
            valid = false;
        }
        if (!binding.serialnumber.getText().toString().matches("^[A-z0-9]+$|^$")) {
            valid = false;
        }
        return valid;
    }
}
