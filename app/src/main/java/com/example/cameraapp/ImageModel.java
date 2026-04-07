package com.example.cameraapp;

import java.io.Serializable;

public class ImageModel implements Serializable {
    private String name;
    private String path;
    private String size;
    private String date;

    public ImageModel(String name, String path, String size, String date) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.date = date;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getSize() { return size; }
    public String getDate() { return date; }
}