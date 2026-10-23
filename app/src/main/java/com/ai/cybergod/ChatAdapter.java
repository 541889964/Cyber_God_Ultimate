package com.ai.cybergod;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.noties.markwon.Markwon;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private final List<String[]> messages;
    private final Markwon markwon;

    public ChatAdapter(Context context, List<String[]> messages) {
        this.messages = messages;
        this.markwon = Markwon.create(context);
    }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] msg = messages.get(position);
        holder.tvRole.setText(msg[0]);
        markwon.setMarkdown(holder.tvContent, msg[1]);
        
        if (msg[0].startsWith("Me")) holder.tvRole.setTextColor(0xFF00FFCC);
        else if (msg[0].startsWith("System")) holder.tvRole.setTextColor(0xFFFFD54F);
        else holder.tvRole.setTextColor(0xFF10B981);

        holder.itemView.setOnLongClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("AI", msg[1]));
            Toast.makeText(v.getContext(), "已复制", Toast.LENGTH_SHORT).show();
            return true;
        });
    }
    @Override public int getItemCount() { return messages.size(); }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRole, tvContent;
        ViewHolder(View v) { super(v); tvRole = v.findViewById(R.id.tvRole); tvContent = v.findViewById(R.id.tvContent); }
    }
}
