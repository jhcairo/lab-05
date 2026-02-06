package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);


        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
                return;
            }
            if (value != null){
                cityArrayList.clear();
                if (!value.isEmpty()){
                    for (QueryDocumentSnapshot snapshot : value){
                        String name = snapshot.getString("name");
                        String province = snapshot.getString("province");
                        if (name != null && province != null){
                            cityArrayList.add(new City(name, province));
                        }
                    }
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = city.getName();
        String newName = title.trim();
        String newProvince = year.trim();
        
        // If name changed, delete old document and create new one
        if (!oldName.equals(newName)) {
            // Delete old document
            DocumentReference oldDocRef = citiesRef.document(oldName);
            oldDocRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Old document deleted"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting old document", e));
            
            // Create new document with new name
            if (!newName.isEmpty() && !newProvince.isEmpty()) {
                Map<String, Object> cityData = new HashMap<>();
                cityData.put("name", newName);
                cityData.put("province", newProvince);
                
                DocumentReference newDocRef = citiesRef.document(newName);
                newDocRef.set(cityData)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "City updated successfully"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Error updating city", e));
            }
        } else {
            // Name didn't change, just update the document
            if (!newName.isEmpty() && !newProvince.isEmpty()) {
                Map<String, Object> cityData = new HashMap<>();
                cityData.put("name", newName);
                cityData.put("province", newProvince);
                
                DocumentReference docRef = citiesRef.document(newName);
                docRef.set(cityData)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "City updated successfully"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Error updating city", e));
            }
        }
        
        // Update local list (will be synced by snapshot listener)
        city.setName(newName);
        city.setProvince(newProvince);
        cityArrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void addCity(City city){
        String name = city.getName().trim();
        String province = city.getProvince().trim();
        
        if (name.isEmpty() || province.isEmpty()) {
            return; // Don't add empty cities
        }
        
        // Convert City to Map for Firestore
        Map<String, Object> cityData = new HashMap<>();
        cityData.put("name", name);
        cityData.put("province", province);
        
        DocumentReference docRef = citiesRef.document(name);
        docRef.set(cityData)
            .addOnSuccessListener(aVoid -> Log.d("Firestore", "City added successfully"))
            .addOnFailureListener(e -> Log.e("Firestore", "Error adding city", e));
        
        // Note: The snapshot listener will update the local list automatically
    }
    
    @Override
    public void deleteCity(City city) {
        String cityName = city.getName();
        if (cityName != null && !cityName.isEmpty()) {
            DocumentReference docRef = citiesRef.document(cityName);
            docRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City deleted successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting city", e));
        }
    }
}