package pl.edu.pb.testrestcountries;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //private TextView countryNameTextView;
    private ImageView flagImageView;
    private Spinner regionSpinner;
    private EditText countryInputEditText;
    private TextView scoreTextView;
    private String[] regions = {"All", "Europe", "Asia", "Americas", "Africa", "Oceania", "Antarctic"};
    private List<Country> europeanCountries = null;
    private int currentCountryIndex = 0;
    private int correctAnswers = 0;
    private boolean wasShaked = false;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private long lastUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Sprawdzenie połączenia z internetem
        if (!isNetworkAvailable()) {
            showNoInternetDialog();
            return;
        }

        //countryNameTextView = findViewById(R.id.countryNameTextView);
        flagImageView = findViewById(R.id.flagImageView);
        regionSpinner = findViewById(R.id.regionSpinner);
        countryInputEditText = findViewById(R.id.countryInputEditText);
        scoreTextView = findViewById(R.id.scoreTextView);

        regionSpinner.setSelection(0); // Wybór "All" jako domyślny
// Ustaw adapter dla spinnera
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, regions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        regionSpinner.setAdapter(adapter);

// Listener dla zmiany regionu
        regionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRegion = regions[position];
                //correctAnswers = 0;
                fetchCountriesByRegion(selectedRegion);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nie wykonuj nic, jeśli nic nie zostało wybrane
            }
        });


        //fetchEuropeanCountries();

        Button switchCountryButton = findViewById(R.id.switchCountryButton);
        switchCountryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (europeanCountries != null && !europeanCountries.isEmpty()) {
                    checkAnswer();
                    currentCountryIndex = (currentCountryIndex + 1) % europeanCountries.size();

                    // Resetowanie wyniku po przejściu przez wszystkie kraje
                    if (currentCountryIndex == 0) {
                        resetScore();
                    }

                    displayCountry(europeanCountries.get(currentCountryIndex));
                    wasShaked = false;
                }
            }
        });


        //Button submitAnswerButton = findViewById(R.id.submitAnswerButton);
        //submitAnswerButton.setOnClickListener(v -> checkAnswer());

        // Inicjalizacja menedżera sensorów
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Rejestracja czujnika
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_SHORT).show();
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();

            // Odczekaj 200 ms przed kolejnym odczytem
            if ((currentTime - lastUpdateTime) > 200) {
                lastUpdateTime = currentTime;

                // Odczytywanie wartości przyspieszenia
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Obliczenie siły wstrząsu
                float shakeMagnitude = (float) Math.sqrt(x * x + y * y + z * z);

                if (shakeMagnitude > SHAKE_THRESHOLD) {
                    onShake();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nie wymagane w tym przypadku
    }

    private void onShake() {
        // Co zrobić po wykryciu potrząsania?
        //Toast.makeText(this, "Shake detected!", Toast.LENGTH_SHORT).show();
        // Tutaj dodaj akcję, np. zmień kraj lub wywołaj inną funkcję

        if (europeanCountries != null && !europeanCountries.isEmpty()) {
            Country currentCountry = europeanCountries.get(currentCountryIndex);
            String countryName = currentCountry.name.common != null ? currentCountry.name.common : "Unknown";
            if (!wasShaked)
            {
                correctAnswers--;
            }
            wasShaked = true;

            Toast.makeText(this, "Currently displayed country: " + countryName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No country is currently loaded.", Toast.LENGTH_SHORT).show();
        }
    }



    private void fetchCountriesByRegion(String region) {
        if (!isNetworkAvailable()) {
            Toast.makeText(MainActivity.this, "No internet connection available, region hasn't changed.", Toast.LENGTH_SHORT).show();
            return;
        }

        RestCountriesApi api = RetrofitClient.getInstance().create(RestCountriesApi.class);
        Call<List<Country>> call;

        if (region.equals("All")) {
            call = api.getAllCountries(); // Pobieranie wszystkich krajów
        } else {
            call = api.getCountriesByRegion(region); // Pobieranie krajów z wybranego regionu
        }

        call.enqueue(new Callback<List<Country>>() {
            @Override
            public void onResponse(Call<List<Country>> call, Response<List<Country>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    europeanCountries = response.body();
                    currentCountryIndex = 0; // Resetuj indeks kraju
                    correctAnswers = 0;

                    //int countryCount = europeanCountries.size();
                    Log.d("CountryFetch", "Number of countries fetched: "+ europeanCountries.size());

                    if (!europeanCountries.isEmpty()) {
                        displayCountry(europeanCountries.get(0)); // Wyświetl pierwszy kraj
                        scoreTextView.setText(correctAnswers + " / " + europeanCountries.size());
                    }
                    else {
                        scoreTextView.setText("0 / 0");
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load countries", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Country>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayCountry(Country country) {
        if (country != null && country.name != null && country.flags != null) {
            //countryNameTextView.setText(country.name.common != null ? country.name.common : "Unknown");
            Picasso.get().load(country.flags.png).into(flagImageView);
        }
    }

    private void checkAnswer() {
        if (europeanCountries != null && !europeanCountries.isEmpty()){
            String userInput = countryInputEditText.getText().toString().trim();
            String correctAnswer = europeanCountries.get(currentCountryIndex).name.common;

            if (userInput.equalsIgnoreCase(correctAnswer)){
                correctAnswers++;
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show();
            }

            scoreTextView.setText("Score: " + correctAnswers + " / " + europeanCountries.size());
        }
        countryInputEditText.setText("");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void resetScore() {
        int finalScore = correctAnswers;
        correctAnswers = 0;
        scoreTextView.setText("Score: " + correctAnswers + " / " + europeanCountries.size());

        new AlertDialog.Builder(this)
                .setTitle("Congratulations, you have completed the quiz.")
                .setMessage("You scored " + finalScore + " out of " + europeanCountries.size() + "!")
                .setPositiveButton("OK", (dialog, which) ->dialog.dismiss())
                .show();
    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("This app requires an internet connection to work. Please connect to the internet and try again.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }
}