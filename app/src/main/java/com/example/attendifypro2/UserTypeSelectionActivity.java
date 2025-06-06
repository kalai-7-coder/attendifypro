package com.example.attendifypro2;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class UserTypeSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(UserTypeSelectionActivity.this , "here" , Toast.LENGTH_SHORT).show();
        setContentView(R.layout.activity_user_type_selection);

        // Initialize buttons
        Button studentButton = findViewById(R.id.studentButton);
        Button adminButton = findViewById(R.id.adminButton);

        // Set onClick listener for Student Button
        studentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to Student Dashboard
                Intent intent = new Intent(UserTypeSelectionActivity.this, StudentDashboard.class);
                startActivity(intent);
            }
        });

        // Set onClick listener for Admin Button
        adminButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to Admin Dashboard
                Intent intent = new Intent(UserTypeSelectionActivity.this, AdminDashboard.class);
                startActivity(intent);
            }
        });
    }
}
