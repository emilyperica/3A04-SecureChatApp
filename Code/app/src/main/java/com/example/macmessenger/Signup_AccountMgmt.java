package com.example.macmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Signup_AccountMgmt extends AppCompatActivity {
    EditText SignUpEmail, SignUpPassword;
    Button SignupButton;
    TextView loginRedirect;
    FirebaseDatabase database;
    DatabaseReference reference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_management_signup);

        SignUpEmail = findViewById(R.id.editTextText2);
        SignUpPassword = findViewById(R.id.editTextTextPassword);
        SignupButton = findViewById(R.id.button2);
        loginRedirect = findViewById(R.id.textView4);

        SignupButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                database = FirebaseDatabase.getInstance();
                reference = database.getReference("users");

                String email = SignUpEmail.getText().toString();
                String password = SignUpPassword.getText().toString();

                HelperClass_AccountMgmt helperClass = new HelperClass_AccountMgmt(email,password);
                reference.child(email).setValue(helperClass);

                Toast.makeText(Signup_AccountMgmt.this, "You have signup successfully",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Signup_AccountMgmt.this, Login_AccountMgmt.class);
                startActivity(intent);

            }
        });

        loginRedirect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(Signup_AccountMgmt.this, Login_AccountMgmt.class);
                startActivity(intent);
            }
        });


    }


}