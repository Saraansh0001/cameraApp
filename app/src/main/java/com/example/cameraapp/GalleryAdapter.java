package com.example.cameraapp;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cameraapp.databinding.ItemGalleryCardBinding;
import java.io.File;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private final List<ImageModel> imageList;
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(ImageModel image);
    }

    public GalleryAdapter(List<ImageModel> imageList, OnImageClickListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGalleryCardBinding binding = ItemGalleryCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageModel image = imageList.get(position);
        
        Object imageSource = image.getPath().startsWith("content://") 
                ? Uri.parse(image.getPath()) 
                : new File(image.getPath());

        Glide.with(holder.itemView.getContext())
                .load(imageSource)
                .centerCrop()
                .into(holder.binding.ivGalleryThumbnail);

        holder.itemView.setOnClickListener(v -> listener.onImageClick(image));
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemGalleryCardBinding binding;

        public ViewHolder(ItemGalleryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}