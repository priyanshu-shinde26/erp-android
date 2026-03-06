package com.campussync.erp.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;

import java.util.List;

public class AiChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_USER    = 0;
    private static final int VIEW_AI      = 1;
    private static final int VIEW_LOADING = 2;

    private final List<AiMessage> messages;
    private final Context context;

    public AiChatAdapter(Context context, List<AiMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        AiMessage msg = messages.get(position);
        if (msg.isLoading()) return VIEW_LOADING;
        return msg.getType() == AiMessage.TYPE_USER ? VIEW_USER : VIEW_AI;
    }

    @NonNull
    @Override

    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_USER) {
            // ✅ FIX: inflate the correct user layout
            View v = inflater.inflate(R.layout.item_ai_message_user, parent, false);
            return new UserViewHolder(v);
        } else if (viewType == VIEW_LOADING) {
            View v = inflater.inflate(R.layout.item_ai_message_loading, parent, false);
            return new LoadingViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_ai_message_ai, parent, false);
            return new AiViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AiMessage msg = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else if (holder instanceof AiViewHolder) {
            ((AiViewHolder) holder).bind(msg);
        }
        // Loading holder needs no binding
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ── ViewHolders ────────────────────────────────────────────────────────────

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvFileName;

        UserViewHolder(View v) {
            super(v);
            tvMessage  = v.findViewById(R.id.tv_user_message);
            tvFileName = v.findViewById(R.id.tv_user_filename);
        }

        void bind(AiMessage msg) {
            if (msg.getFileName() != null && !msg.getFileName().isEmpty()) {
                tvFileName.setVisibility(View.VISIBLE);
                tvFileName.setText("📎 " + msg.getFileName());
                tvMessage.setVisibility(
                        msg.getText() != null && !msg.getText().isEmpty()
                                ? View.VISIBLE : View.GONE);
            } else {
                tvFileName.setVisibility(View.GONE);
                tvMessage.setVisibility(View.VISIBLE);
            }
            if (msg.getText() != null) tvMessage.setText(msg.getText());
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        AiViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tv_ai_message);
        }

        void bind(AiMessage msg) {
            tvMessage.setText(msg.getText());
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(View v) { super(v); }
    }
}