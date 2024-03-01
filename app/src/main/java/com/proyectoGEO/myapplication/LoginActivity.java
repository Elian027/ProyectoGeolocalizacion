package com.proyectoGEO.myapplication;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText etCorreo, etContrasena;
    Button btn_ingresar;
    TextView btn_registrar;
    int REQUEST_LOCATION_PERMISSION = 1;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        db = FirebaseFirestore.getInstance();

        etCorreo = findViewById(R.id.correo);
        etContrasena = findViewById(R.id.pass);
        btn_ingresar = findViewById(R.id.ingresar);
        btn_registrar = findViewById(R.id.registrar);

        verificarSesion();

        btn_ingresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarSesion();
            }
        });

        btn_registrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent irRegistrar = new Intent(LoginActivity.this, RegistrarActivity.class);
                startActivity(irRegistrar);
            }
        });
    }

    private void iniciarSesion() {
        final String correo = etCorreo.getText().toString().trim();
        final String contrasena = etContrasena.getText().toString().trim();

        if (correo.isEmpty()) {
            etCorreo.setError("Por favor ingrese su correo electrónico");
            etCorreo.requestFocus();
            return;
        }

        if (contrasena.isEmpty()) {
            etContrasena.setError("Por favor ingrese su contraseña");
            etContrasena.requestFocus();
            return;
        }

        // Obtener hash
        String contrasenaHash = hash(contrasena);

        // Consulta a Firestore para obtener los datos del usuario
        db.collection("Usuarios")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String usuarioID = document.getId();
                                String correoFirestore = document.getString("correo");
                                String contrasenaFirestore = document.getString("contrasena");

                                if (correo.equals(correoFirestore) && contrasenaHash.equals(contrasenaFirestore)) {
                                    guardarID(usuarioID);
                                    Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                                    Intent irMain = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(irMain);
                                    return;
                                }
                            }
                            Toast.makeText(LoginActivity.this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void guardarID(String usuarioId) {
        // Almacenar las credenciales
        SharedPreferences preferences = getSharedPreferences("user_info", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userId", usuarioId);
        editor.apply();
    }

    private void verificarSesion() {
        SharedPreferences preferences = getSharedPreferences("user_info", Context.MODE_PRIVATE);
        String userId = preferences.getString("userId", "");

        if (!userId.isEmpty()) {
            // Iniciar directamente con el usuario almacenado
            Intent irMain = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(irMain);
            finish();
        }
    }

}
