package org.homicideware.stealthrabbit.viewmodels;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import org.homicideware.stealthrabbit.models.ServicesModel;
import org.homicideware.stealthrabbit.viewdata.ServicesData;

public class ServicesViewModel extends ViewModel {

    private MutableLiveData<List<ServicesModel>> mutableLiveDataServicesModelList;

    public void init(Activity activity, Context context) {
        if (mutableLiveDataServicesModelList != null) {
            return;
        }
        ServicesData servicesData = ServicesData.getInstance();
        if (ServicesData.isDataInitiated) {
            mutableLiveDataServicesModelList = servicesData.getServicesModels();
        } else {
            mutableLiveDataServicesModelList = servicesData.getServicesModels(activity, context);
        }
    }

    public LiveData<List<ServicesModel>> getLiveDataServicesModelList() {
        return mutableLiveDataServicesModelList;
    }
}
