package material.hunter.viewmodels;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import material.hunter.RecyclerViewData.MaterialHunterData;
import material.hunter.models.MaterialHunterModel;

public class MaterialHunterViewModel extends ViewModel {
  private MutableLiveData<List<MaterialHunterModel>> mutableLiveDataMaterialHunterModelList;

  public void init(Context context) {
    if (mutableLiveDataMaterialHunterModelList != null) {
      return;
    }
    MaterialHunterData materialhunterData = MaterialHunterData.getInstance();
    if (MaterialHunterData.isDataInitiated) {
      mutableLiveDataMaterialHunterModelList = materialhunterData.getMaterialHunterModels();
    } else {
      mutableLiveDataMaterialHunterModelList = materialhunterData.getMaterialHunterModels(context);
    }
  }

  public LiveData<List<MaterialHunterModel>> getLiveDataMaterialHunterModelList() {
    return mutableLiveDataMaterialHunterModelList;
  }
}
