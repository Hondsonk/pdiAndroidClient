package com.example.pdiapp

import retrofit2.Response
import retrofit2.http.GET

interface CarDataApi {
    @GET("/lastTen")
    suspend fun getCarData() : Response<CarData>
}