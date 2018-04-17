package com.example.macintosh.nearbylocation;

import com.example.macintosh.nearbylocation.remote.IGoogleApiService;
import com.example.macintosh.nearbylocation.remote.RetrofitClient;


public class Common {
    public static final String GOOGLE_API_URL = "https://maps.googleapis.com/maps/api/";

    public static IGoogleApiService getGoogleApiService(){
        return RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleApiService.class);
    }
}
