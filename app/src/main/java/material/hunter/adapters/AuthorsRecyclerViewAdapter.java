package material.hunter.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import material.hunter.R;
import material.hunter.models.AuthorsModel;

public class AuthorsRecyclerViewAdapter extends RecyclerView.Adapter<AuthorsRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<AuthorsModel> list;

    public AuthorsRecyclerViewAdapter(
            Context context, List<AuthorsModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(context)
                        .inflate(
                                R.layout.author_item,
                                parent,
                                false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        AuthorsModel model = list.get(position);
        holder.nickname.setText(model.getNickname());
        holder.nickname.append(" (" + model.getNicknameDesc() + ")");
        holder.description.setText(
                Html.fromHtml(
                        model.getDescription(),
                        Html.FROM_HTML_MODE_LEGACY));
        holder.description.setMovementMethod(LinkMovementMethod.getInstance());
        String contact = !model.getGithub().isEmpty() ? model.getGithub() : !model.getTelegram().isEmpty() ? model.getTelegram() : "";
        if (!contact.isEmpty()) {
            holder.card.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(contact));
                context.startActivity(intent);
            });
        }
        if (!model.getGithub().isEmpty()) {
            holder.github.setVisibility(View.VISIBLE);
            holder.github.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getGithub()));
                context.startActivity(intent);
            });
        }
        if (!model.getTelegram().isEmpty()) {
            holder.telegram.setVisibility(View.VISIBLE);
            holder.telegram.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getTelegram()));
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public MaterialCardView card;
        public TextView nickname;
        public TextView description;
        public ImageView github;
        public ImageView telegram;

        public ViewHolder(View v) {
            super(v);
            card = v.findViewById(R.id.card);
            nickname = v.findViewById(R.id.nickname);
            description = v.findViewById(R.id.description);
            github = v.findViewById(R.id.github);
            telegram = v.findViewById(R.id.telegram);
        }
    }
}