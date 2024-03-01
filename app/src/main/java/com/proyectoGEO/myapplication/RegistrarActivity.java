package com.proyectoGEO.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class RegistrarActivity extends AppCompatActivity {

    TextInputEditText etNombre, etCorreo, etContrasena;
    Button btn_registrar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar);

        db = FirebaseFirestore.getInstance();

        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btn_registrar = findViewById(R.id.registrar);

        btn_registrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });
    }

    private void registrarUsuario() {
        final String nombre = etNombre.getText().toString().trim();
        final String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombre.setError("Por favor ingrese su nombre");
            etNombre.requestFocus();
            return;
        }

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

        String contrasenaHash = hash(contrasena);

        // Verificar que se haya generado el hash correctamente
        if (TextUtils.isEmpty(contrasenaHash)) {
            Toast.makeText(this, "Error al generar el hash de la contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear un mapa con los datos del usuario
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", nombre);
        usuario.put("correo", correo);
        usuario.put("contrasena", contrasenaHash);

        // Agregar el usuario a Firestore
        db.collection("Usuarios")
                .add(usuario)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(RegistrarActivity.this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                    Intent irLogin = new Intent(RegistrarActivity.this, LoginActivity.class);
                    startActivity(irLogin);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegistrarActivity.this, "Error al registrar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("RegistrarActivity", "Error al registrar usuario", e);
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
}
