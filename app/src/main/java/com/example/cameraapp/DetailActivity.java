package com.example.cameraapp;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import com.bumptech.glide.Glide;
import com.example.cameraapp.databinding.ActivityDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;

public class DetailActivity extends AppCompatActivity {

    private ActivityDetailBinding binding;
    private ImageModel imageModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageModel = (ImageModel) getIntent().getSerializableExtra("image_data");

        if (imageModel != null) {
            setupUI();
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void setupUI() {
        binding.tvDetailName.setText(imageModel.getName());
        binding.tvDetailPath.setText("Path: " + imageModel.getPath());
        binding.tvDetailSize.setText("Size: " + imageModel.getSize());
        binding.tvDetailDate.setText("Date: " + imageModel.getDate());

        // Glide can handle both file paths and URI strings
        Object imageSource = imageModel.getPath().startsWith("content://") 
                ? Uri.parse(imageModel.getPath()) 
                : new File(imageModel.getPath());

        Glide.with(this)
                .load(imageSource)
                .into(binding.ivDetailPreview);
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage() {
        boolean deleted = false;
        String path = imageModel.getPath();

        if (path.startsWith("content://")) {
            // Handle Scoped Storage / Document Tree URIs
            DocumentFile docFile = DocumentFile.fromSingleUri(this, Uri.parse(path));
            if (docFile != null && docFile.exists()) {
                deleted = docFile.delete();
            }
        } else {
            // Handle regular Files
            File file = new File(path);
            if (file.exists()) {
                deleted = file.delete();
            }
        }

        if (deleted) {
            Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
        }
    }
}