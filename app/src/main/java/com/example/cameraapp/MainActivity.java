package com.example.cameraapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.cameraapp.databinding.ActivityMainBinding;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private GalleryAdapter adapter;
    private final List<ImageModel> imageList = new ArrayList<>();
    private String currentPhotoPath;
    private Uri selectedFolderUri;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Toast.makeText(this, getString(R.string.permissions_granted), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.permissions_required), Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Uri> selectFolderLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    selectedFolderUri = uri;
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    binding.tvFolderPath.setText(uri.getPath());
                    loadImagesFromFolder(uri);
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    Toast.makeText(this, getString(R.string.photo_saved), Toast.LENGTH_SHORT).show();
                    if (selectedFolderUri != null) {
                        loadImagesFromFolder(selectedFolderUri);
                    } else {
                        File picDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        if (picDir != null) {
                            refreshGalleryFromPath(picDir.getAbsolutePath());
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> detailActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (selectedFolderUri != null) {
                        loadImagesFromFolder(selectedFolderUri);
                    } else if (currentPhotoPath != null) {
                        refreshGalleryFromPath(new File(currentPhotoPath).getParent());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkPermissions();
        setupRecyclerView();

        binding.btnSelectFolder.setOnClickListener(v -> selectFolderLauncher.launch(null));
        binding.fabCamera.setOnClickListener(v -> capturePhoto());

        // Load default pictures on start if any
        File picDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (picDir != null) {
            refreshGalleryFromPath(picDir.getAbsolutePath());
        }
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissionLauncher.launch(listPermissionsNeeded.toArray(new String[0]));
        }
    }

    private void setupRecyclerView() {
        adapter = new GalleryAdapter(imageList, image -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("image_data", image);
            detailActivityLauncher.launch(intent);
        });
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerView.setAdapter(adapter);
    }

    private void capturePhoto() {
        try {
            File photoFile = createImageFile();
            Uri currentPhotoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(currentPhotoUri);
        } catch (IOException ex) {
            Toast.makeText(this, getString(R.string.error_creating_file), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void loadImagesFromFolder(Uri folderUri) {
        imageList.clear();
        DocumentFile root = DocumentFile.fromTreeUri(this, folderUri);
        if (root != null) {
            for (DocumentFile file : root.listFiles()) {
                if (file.getType() != null && file.getType().startsWith("image/")) {
                    String size = String.format(Locale.getDefault(), "%.2f MB", (float) file.length() / (1024 * 1024));
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(file.lastModified()));
                    imageList.add(new ImageModel(file.getName(), file.getUri().toString(), size, date));
                }
            }
        }
        updateUI();
    }

    private void refreshGalleryFromPath(String path) {
        if (path == null) return;
        imageList.clear();
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isImageFile(file.getName())) {
                    String size = String.format(Locale.getDefault(), "%.2f MB", (float) file.length() / (1024 * 1024));
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(file.lastModified()));
                    imageList.add(new ImageModel(file.getName(), file.getAbsolutePath(), size, date));
                }
            }
        }
        updateUI();
    }

    private boolean isImageFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        binding.tvEmpty.setVisibility(imageList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}