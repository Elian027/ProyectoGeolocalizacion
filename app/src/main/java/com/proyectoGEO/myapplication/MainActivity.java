package com.proyectoGEO.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    GoogleMap gMap;
    Button btn_guardar, btn_limpiar, btn_cerrar;
    int REQUEST_LOCATION_PERMISSION = 1;
    FusedLocationProviderClient fusedLocationClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    // Almacena los puntos seleccionados
    ArrayList<LatLng> puntos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_guardar = findViewById(R.id.guardar);
        btn_limpiar = findViewById(R.id.limpiar);
        btn_cerrar = findViewById(R.id.cerrar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permisos de ubicacion
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        btn_limpiar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                puntos.clear();
                gMap.clear();
            }
        });

        btn_guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dibujarFigura();
                calcularYMostrarArea();
            }
        });

        btn_cerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                limpiarDatos();
                Intent irLogin = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(irLogin);
            }
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Obtener la ubicación en tiempo real
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    guardarUbicacion(latitude, longitude);
                }
            }
        };

        startLocationUpdates();

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        gMap.setMyLocationEnabled(true);
        gMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        puntos.add(latLng); // Agregar el punto a la lista
        gMap.addMarker(new MarkerOptions().position(latLng)); // Agregar marcador en el mapa
    }

    private void dibujarFigura() {
        if (puntos.size() < 3) {
            Toast.makeText(this, "Debe seleccionar al menos 3 puntos para formar una figura", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dibujar la figura en el mapa
        PolygonOptions polygonOptions = new PolygonOptions();
        for (LatLng point : puntos) {
            polygonOptions.add(point);
        }
        polygonOptions.strokeColor(Color.RED);
        gMap.addPolygon(polygonOptions);
    }

    private void calcularYMostrarArea() {
        double area = calcularArea(puntos);

        String areaN = String.format("%.2f", area);

        // Mostrar el área calculada en una ventana
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Área de la figura: " + areaN + " m2")
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private double calcularArea(ArrayList<LatLng> points) {
        if (points.size() < 3) {
            return 0;
        } else if (points.size() == 3) {
            //Calcula área de tríangulo con los 3 puntos dados
            LatLng p1 = points.get(0);
            LatLng p2 = points.get(1);
            LatLng p3 = points.get(2);

            double lado1 = calcularDistancia(p1, p2);
            double lado2 = calcularDistancia(p2, p3);
            double lado3 = calcularDistancia(p3, p1);

            double s = (lado1 + lado2 + lado3) / 2;
            return Math.sqrt(s * (s - lado1) * (s - lado2) * (s - lado3));
        } else if (points.size() == 4) {
            // Calcular área de rectángulo con 4 puntos dados
            LatLng p1 = points.get(0);
            LatLng p2 = points.get(1);
            LatLng p3 = points.get(2);
            LatLng p4 = points.get(3);

            double lado1 = Math.min(calcularDistancia(p1, p2), calcularDistancia(p2, p3));
            double lado2 = Math.min(calcularDistancia(p2, p3), calcularDistancia(p3, p4));

            return lado1 * lado2;
        } else {
            return 0;
        }
    }

    private double calcularDistancia(LatLng p1, LatLng p2) {
        double lat1 = p1.latitude;
        double lon1 = p1.longitude;
        double lat2 = p2.latitude;
        double lon2 = p2.longitude;

        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private void limpiarDatos() {
        // Limpia el ID almacenado
        SharedPreferences preferences = getSharedPreferences("user_info", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    private void guardarUbicacion(double latitude, double longitude) {
        // Guardar la ubicación en Realtime Database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("ubicaciones_usuarios");
        String userId = obtenerId();
        Map<String, Object> ubicacionMap = new HashMap<>();
        ubicacionMap.put("latitud", latitude);
        ubicacionMap.put("longitud", longitude);
        databaseRef.child(userId).setValue(ubicacionMap);
    }

    private String obtenerId() {
        SharedPreferences preferences = getSharedPreferences("user_info", Context.MODE_PRIVATE);
        return preferences.getString("userId","");
    }

}
