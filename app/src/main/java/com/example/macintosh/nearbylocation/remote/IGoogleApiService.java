package com.example.macintosh.nearbylocation.remote;

import com.example.macintosh.nearbylocation.model.MyPlace;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface IGoogleApiService {
    @GET
    Call<MyPlace> getNearbyPlaces(@Url String url);
}
