package org.homicideware.stealthrabbit.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import org.homicideware.stealthrabbit.models.CustomCommandsModel;
import org.homicideware.stealthrabbit.viewdata.CustomCommandsData;

public class CustomCommandsViewModel extends ViewModel {

    private MutableLiveData<List<CustomCommandsModel>> mutableLiveDataCustomCommandsModelList;

    public void init(Context context) {
        if (mutableLiveDataCustomCommandsModelList != null) {
            return;
        }
        CustomCommandsData customCommandsData = CustomCommandsData.getInstance();
        if (CustomCommandsData.isDataInitiated) {
            mutableLiveDataCustomCommandsModelList = customCommandsData.getCustomCommandsModels();
        } else {
            mutableLiveDataCustomCommandsModelList =
                    customCommandsData.getCustomCommandsModels(context);
        }
    }

    public LiveData<List<CustomCommandsModel>> getLiveDataCustomCommandsModelList() {
        return mutableLiveDataCustomCommandsModelList;
    }
}
