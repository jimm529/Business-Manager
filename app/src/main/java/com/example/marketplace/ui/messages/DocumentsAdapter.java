package com.example.marketplace.ui.messages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.bid.NegotiationDocument;
import com.example.marketplace.databinding.ItemSharedDocumentBinding;
import com.example.marketplace.utils.Formatters;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/** Draws the shared documents: a red file icon for PDFs, a green shield for everything else, and a download button. */
public class DocumentsAdapter extends ListAdapter<NegotiationDocument, DocumentsAdapter.ViewHolder> {

    /** What happens when the download button of a document is tapped. */
    public interface Listener {
        void onDownloadClick(@NonNull NegotiationDocument document);
    }

    private static final DiffUtil.ItemCallback<NegotiationDocument> DIFF = new DiffUtil.ItemCallback<NegotiationDocument>() {
        @Override
        public boolean areItemsTheSame(@NonNull NegotiationDocument oldItem, @NonNull NegotiationDocument newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull NegotiationDocument oldItem, @NonNull NegotiationDocument newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final Listener listener;

    public DocumentsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSharedDocumentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSharedDocumentBinding binding;

        ViewHolder(ItemSharedDocumentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NegotiationDocument document, Listener listener) {
            Context context = itemView.getContext();
            String title = document.getTitle();
            binding.tvTitle.setText(title == null || title.trim().isEmpty()
                    ? context.getString(R.string.document_untitled) : title);
            binding.tvMeta.setText(context.getString(R.string.document_meta,
                    Formatters.fileSize(context, document.getFileSize()),
                    Formatters.shortDate(context, document.getDate())));

            boolean pdf = document.isPdf();
            binding.ivIcon.setImageResource(pdf ? R.drawable.ic_document : R.drawable.ic_shield);
            binding.ivIcon.setColorFilter(MaterialColors.getColor(binding.getRoot(), pdf
                    ? com.google.android.material.R.attr.colorError
                    : com.google.android.material.R.attr.colorSecondary));

            binding.btnDownload.setEnabled(document.getDownloadUrl() != null);
            binding.btnDownload.setOnClickListener(v -> listener.onDownloadClick(document));
        }
    }
}
