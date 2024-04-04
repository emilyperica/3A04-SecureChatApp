package com.example.macmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class Login_AccountMgmt extends AppCompatActivity {
    EditText loginUsername, loginPassword;
    Button loginButton;
    TextView signup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_management_login);

        loginUsername = findViewById(R.id.editTextText2);
        loginPassword = findViewById(R.id.editTextTextPassword);
        loginButton=findViewById(R.id.button2);
        signup = findViewById(R.id.textView4);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateUsername() | !validatePassword()){

                }else{
                    checkUser();
                }
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(Login_AccountMgmt.this, Signup_AccountMgmt.class);
                startActivity(intent);
            }
        });


    }
    public Boolean validateUsername(){
        String val = loginUsername.getText().toString();
        if(val.isEmpty()){
            loginUsername.setError("Username cannot be empty");
            return false;
        }else{
            loginUsername.setError(null);
            return true;
        }
    }
    public Boolean validatePassword(){
        String val = loginPassword.getText().toString();
        if(val.isEmpty()){
            loginPassword.setError("Password cannot be empty");
            return false;
        }else{
            loginPassword.setError(null);
            return true;
        }
    }

    public void checkUser(){
        String userUsername = loginUsername.getText().toString().trim();
        String userPassword = loginPassword.getText().toString().trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("email").equalTo(userUsername);
        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(userUsername)){
                    loginUsername.setError(null);
                    String passwordFromDB = snapshot.child(userUsername).child("password").getValue(String.class);

                    if(passwordFromDB.equals(userPassword)){
                        loginUsername.setError(null);
                        Intent intent =new Intent(Login_AccountMgmt.this,Messaging.class);
                        startActivity(intent);
                    }else{
                        loginPassword.setError("Invalid credentials");
                        loginPassword.requestFocus();
                    }
                }else{
                    loginPassword.setError("User doesn't exist");
                    loginPassword.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }



}
