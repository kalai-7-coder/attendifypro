package com.example.attendifypro2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class RegisterPage extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextName;
    private Button signUp, captureFaceButton;
    private TextView signIn;
    private ImageView facePreview;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private Bitmap capturedFaceImage;

    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextName = findViewById(R.id.name);
        signUp = findViewById(R.id.register);
        signIn = findViewById(R.id.login);
        captureFaceButton = findViewById(R.id.captureFaceButton);
        facePreview = findViewById(R.id.facePreview);

        signIn.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterPage.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        captureFaceButton.setOnClickListener(view -> captureImage());
        signUp.setOnClickListener(view -> registerUser());
    }

    private void captureImage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            detectFaceBeforeSaving(bitmap);
        }
    }

    private void detectFaceBeforeSaving(Bitmap bitmap) {
        FaceRecognitionHelper.detectFace(bitmap, new FaceRecognitionHelper.FaceDetectionCallback() {
            @Override
            public void onFaceDetected(Bitmap faceBitmap) {
                capturedFaceImage = faceBitmap;
                facePreview.setImageBitmap(faceBitmap);
                Toast.makeText(RegisterPage.this, "✅ Face detected! Ready to register", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoFaceDetected() {
                Toast.makeText(RegisterPage.this, "❌ No face detected. Please try again", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RegisterPage.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String name = editTextName.getText().toString().trim();

        if (!validateInput(email, password, name)) return;

        // Face registration is optional but recommended
        if (capturedFaceImage == null) {
            Toast.makeText(this, "⚠️ No face captured. You'll only be able to use fingerprint authentication.", Toast.LENGTH_LONG).show();
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = firebaseAuth.getCurrentUser().getUid();
                        saveUserProfile(userId, email, name);

                        if (capturedFaceImage != null) {
                            storeFaceImage(userId, capturedFaceImage);
                        }

                        Toast.makeText(RegisterPage.this, "✅ Registration successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterPage.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(RegisterPage.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInput(String email, String password, String name) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Enter Name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid Email", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void storeFaceImage(String userId, Bitmap bitmap) {
        String encodedImage = FaceRecognitionHelper.bitmapToBase64(bitmap);

        databaseReference.child(userId).child("faceImage").setValue(encodedImage)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Face image registered!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save face data", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile(String userId, String email, String name) {
        databaseReference.child(userId).child("name").setValue(name);
        databaseReference.child(userId).child("email").setValue(email);
        databaseReference.child(userId).child("joinedLobbies").setValue(new ArrayList<>());
        databaseReference.child(userId).child("createdLobbies").setValue(new ArrayList<>());
    }
}