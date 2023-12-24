package com.yos.chaton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.yos.chaton.model.UserModel;
import com.yos.chaton.utils.FirebaseUtil;

public class LoginUserNameActivity extends AppCompatActivity {
    EditText usernameInput;
    Button welcome;
    ProgressBar progressBar;
    String phoneNumber;
    UserModel userModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_user_name);

        usernameInput = findViewById(R.id.login_username);
        welcome = findViewById(R.id.login_welcome);
        progressBar = findViewById(R.id.login_progress_bar);

        phoneNumber = getIntent().getExtras().getString("phone");
        getUsername();
        welcome.setOnClickListener((view -> {
            //Toast.makeText(this, "working", Toast.LENGTH_SHORT).show();
            setUsername();
        }));


    }
    void setUsername(){

        String username = usernameInput.getText().toString();
        if(username.isEmpty() || username.length()<3){
            usernameInput.setError("Username length should be at least 3 chars");
            return;
        }
        setInProgress(true);
        if(userModel!=null){
            userModel.setUsername(username);
        }else{
            userModel = new UserModel(phoneNumber,username, Timestamp.now(),FirebaseUtil.currentUserId());
        }

        FirebaseUtil.currentUserDetails().set(userModel).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                setInProgress(false);
                if(task.isSuccessful()){
                    Intent intent = new Intent(LoginUserNameActivity.this,MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
                    startActivity(intent);
                }
                else{
                    Toast.makeText(LoginUserNameActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                }
            }
        });

    }


    void getUsername(){
        setInProgress(true);
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    setInProgress(false);
                    if(task.isSuccessful()){
                       userModel = task.getResult().toObject(UserModel.class);
                    if(userModel!= null){
                        usernameInput.setText(userModel.getUsername());
                    }

                    }
            }
        });


    }
    void setInProgress(Boolean inProgress){
        if(inProgress){
            progressBar.setVisibility(View.VISIBLE);
            welcome.setVisibility(View.GONE);

        }else{

            progressBar.setVisibility(View.GONE);
            welcome.setVisibility(View.VISIBLE);
        }

    }
}