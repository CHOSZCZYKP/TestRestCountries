package pl.edu.pb.testrestcountries;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RestCountriesApi {
    @GET("v3.1/region/{region}")
    Call<List<Country>> getCountriesByRegion(@Path("region") String region);

    @GET("v3.1/all")
    Call<List<Country>> getAllCountries();
}
