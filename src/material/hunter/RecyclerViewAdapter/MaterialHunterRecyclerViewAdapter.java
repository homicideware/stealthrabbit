package material.hunter.RecyclerViewAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import material.hunter.R;
import material.hunter.RecyclerViewData.MaterialHunterData;
import material.hunter.SQL.MaterialHunterSQL;
import material.hunter.models.MaterialHunterModel;
import material.hunter.utils.NhPaths;

import java.util.ArrayList;
import java.util.List;

public class MaterialHunterRecyclerViewAdapter extends RecyclerView.Adapter<MaterialHunterRecyclerViewAdapter.ItemViewHolder> implements Filterable {

    private static final String TAG = "MaterialHunterRecyclerView";
    private Context context;
    private List<MaterialHunterModel> nethunterModelList;
    private Filter MaterialHunterModelListFilter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                results.values = new ArrayList<>(MaterialHunterData.getInstance().nethunterModelListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                List<MaterialHunterModel> tempMaterialHunterModelList = new ArrayList<>();
                for (MaterialHunterModel nethunterModel : MaterialHunterData.getInstance().nethunterModelListFull) {
                    if (nethunterModel.getTitle().toLowerCase().contains(filterPattern)) {
                        tempMaterialHunterModelList.add(nethunterModel);
                    }
                }
                results.values = tempMaterialHunterModelList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            MaterialHunterData.getInstance().getMaterialHunterModels().getValue().clear();
            MaterialHunterData.getInstance().getMaterialHunterModels().getValue().addAll((List<MaterialHunterModel>) results.values);
            MaterialHunterData.getInstance().getMaterialHunterModels().postValue(MaterialHunterData.getInstance().getMaterialHunterModels().getValue());
        }
    };

    public MaterialHunterRecyclerViewAdapter(Context context, List<MaterialHunterModel> nethunterModelList) {
        this.context = context;
        this.nethunterModelList = nethunterModelList;
    }

    @NonNull
    @Override
    public MaterialHunterRecyclerViewAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.nethunter_recyclerview_main, parent, false);
        return new MaterialHunterRecyclerViewAdapter.ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialHunterRecyclerViewAdapter.ItemViewHolder holder, int position) {
        holder.titleTextView.setText(nethunterModelList.get(position).getTitle());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        holder.resultRecyclerView.setLayoutManager(linearLayoutManager);
        holder.resultRecyclerView.setAdapter(new MaterialHunterRecyclerViewAdapterResult(context, nethunterModelList.get(position).getResult()));
        holder.runButton.setOnClickListener(v -> MaterialHunterData.getInstance().runCommandforItem(position));
        holder.titleTextView.setOnLongClickListener(v -> {
            final LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewEdit = mInflater.inflate(R.layout.nethunter_edit_dialog_view, null);
            final EditText titleEditText = promptViewEdit.findViewById(R.id.f_nethunter_edit_adb_et_title);
            final EditText cmdEditText = promptViewEdit.findViewById(R.id.f_nethunter_edit_adb_et_command);
            final EditText delimiterEditText = promptViewEdit.findViewById(R.id.f_nethunter_edit_adb_et_delimiter);
            final CheckBox runOnCreateCheckbox = promptViewEdit.findViewById(R.id.f_nethunters_edit_adb_checkbox_runoncreate);
            final FloatingActionButton readmeButton1 = promptViewEdit.findViewById(R.id.f_nethunter_edit_btn_info_fab1);
            final FloatingActionButton readmeButton2 = promptViewEdit.findViewById(R.id.f_nethunter_edit_btn_info_fab2);
            final FloatingActionButton readmeButton3 = promptViewEdit.findViewById(R.id.f_nethunter_edit_btn_info_fab3);
            readmeButton1.setOnClickListener(view -> {
                androidx.appcompat.app.AlertDialog.Builder adb = new androidx.appcompat.app.AlertDialog.Builder(context);
                adb.setTitle("HOW TO USE:")
                        .setMessage(context.getString(R.string.nethunter_howtouse_cmd))
                        .setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss());
                final androidx.appcompat.app.AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton2.setOnClickListener(view -> {
                androidx.appcompat.app.AlertDialog.Builder adb = new androidx.appcompat.app.AlertDialog.Builder(context);
                adb.setTitle("HOW TO USE:")
                        .setMessage(context.getString(R.string.nethunter_howtouse_delimiter))
                        .setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss());
                final androidx.appcompat.app.AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton3.setOnClickListener(view -> {
                androidx.appcompat.app.AlertDialog.Builder adb = new androidx.appcompat.app.AlertDialog.Builder(context);
                adb.setTitle("HOW TO USE:")
                        .setMessage(context.getString(R.string.nethunter_howtouse_runoncreate))
                        .setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss());
                final androidx.appcompat.app.AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });
            titleEditText.setText(MaterialHunterData.getInstance().nethunterModelListFull.get(
                    MaterialHunterData.getInstance().nethunterModelListFull.indexOf(
                            nethunterModelList.get(position))).getTitle());
            cmdEditText.setText(MaterialHunterData.getInstance().nethunterModelListFull.get(
                    MaterialHunterData.getInstance().nethunterModelListFull.indexOf(
                            nethunterModelList.get(position))).getCommand());
            delimiterEditText.setText(MaterialHunterData.getInstance().nethunterModelListFull.get(
                    MaterialHunterData.getInstance().nethunterModelListFull.indexOf(
                            nethunterModelList.get(position))).getDelimiter());
            runOnCreateCheckbox.setChecked(MaterialHunterData.getInstance().nethunterModelListFull.get(
                    MaterialHunterData.getInstance().nethunterModelListFull.indexOf(
                            nethunterModelList.get(position))).getRunOnCreate().equals("1"));

            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            adb.setPositiveButton("Apply", (dialog, which) -> {
            });
            final AlertDialog ad = adb.create();
            ad.setView(promptViewEdit);
            ad.setCancelable(true);
            ad.setOnShowListener(dialog -> {
                final Button buttonEdit = ad.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonEdit.setOnClickListener(v1 -> {
                    if (titleEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage(context, "Title cannot be empty");
                    } else if (cmdEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage(context, "Command cannot be empty");
                    } else if (delimiterEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage(context, "Delimiter cannot be empty");
                    } else {
                        ArrayList<String> dataArrayList = new ArrayList<>();
                        dataArrayList.add(titleEditText.getText().toString());
                        dataArrayList.add(cmdEditText.getText().toString());
                        dataArrayList.add(delimiterEditText.getText().toString());
                        dataArrayList.add(runOnCreateCheckbox.isChecked() ? "1" : "0");
                        MaterialHunterData.getInstance().editData(MaterialHunterData.getInstance().nethunterModelListFull.indexOf(
                                nethunterModelList.get(position)), dataArrayList, MaterialHunterSQL.getInstance(context));
                        ad.dismiss();
                    }
                });
            });
            ad.show();
            return false;
        });
        if (MaterialHunterData.getInstance().nethunterModelListFull.get(MaterialHunterData.getInstance().nethunterModelListFull.indexOf(nethunterModelList.get(position))).getRunOnCreate().equals("1")) {
            holder.runButton.setVisibility(View.INVISIBLE);
        } else {
            holder.runButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return nethunterModelList.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public Filter getFilter() {
        return MaterialHunterModelListFilter;
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private RecyclerView resultRecyclerView;
        private Button runButton;

        //private Button editButton;
        private ItemViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.f_nethunter_item_title_tv);
            resultRecyclerView = view.findViewById(R.id.f_nethunter_item_result_recyclerview);
            runButton = view.findViewById(R.id.f_nethunter_item_run_btn);
            //editButton = view.findViewById(R.id.f_nethunter_item_edit_btn);
        }
    }
}