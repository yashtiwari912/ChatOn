package com.yos.chaton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.yos.chaton.adapter.ChatRecyclerAdapter;
import com.yos.chaton.adapter.SearchUserRecyclerAdapter;
import com.yos.chaton.model.ChatMessageModel;
import com.yos.chaton.model.ChatroomModel;
import com.yos.chaton.model.UserModel;
import com.yos.chaton.utils.AndroidUtil;
import com.yos.chaton.utils.FirebaseUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    UserModel otherUser;
    ChatroomModel chatroomModel;
    String chatroomId;
    EditText messageInput;
    ImageButton backbtn;
    ImageButton sendMessageBtn;
    TextView otherUsername;
    ImageView imageView;
    RecyclerView recyclerView;
    ChatRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUser = AndroidUtil.getuserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(),otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        backbtn = findViewById(R.id.back_btn);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView =findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if(t.isSuccessful()){
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(this,uri,imageView);
                    }
                });


        backbtn.setOnClickListener(view -> {
            //startActivity(new Intent(ChatActivity.this, SearchUserActivity.class));
            onBackPressed();
        });
        otherUsername.setText(otherUser.getUsername());
        sendMessageBtn.setOnClickListener(view -> {
                String message = messageInput.getText().toString().trim();
                if(message.isEmpty())
                    return;
                sendMessageToUser(message);

        });

        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }
    void setupChatRecyclerView(){
        Query query = FirebaseUtil.getChatroomMessageRefrence(chatroomId)
                .orderBy("timestamp",Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options,getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    void sendMessageToUser(String message){

        chatroomModel.setLastMessagetimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomRefrence(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(message,FirebaseUtil.currentUserId(),Timestamp.now());
        FirebaseUtil.getChatroomMessageRefrence(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if(task.isSuccessful()){
                            messageInput.setText("");
                            sendNotification(message);

                        }
                    }
                });
    }
    void getOrCreateChatroomModel(){
            FirebaseUtil.getChatroomRefrence(chatroomId).get().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    chatroomModel = task.getResult().toObject(ChatroomModel.class);
                }
                if(chatroomModel == null){
                    //first time chat
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(),otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseUtil.getChatroomRefrence(chatroomId).set(chatroomModel);
                }
            });
    }
    void sendNotification(String message){
            //currentusername,message,currentuserid,otherusertoken
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    UserModel currentUser = task.getResult().toObject(UserModel.class);
                    try{
                        JSONObject jsonObject = new JSONObject();


                        JSONObject notificationObj = new JSONObject();
                        notificationObj.put("title",currentUser.getUsername());
                        notificationObj.put("body",message);

                        JSONObject dataObj = new JSONObject();
                        dataObj.put("userId",currentUser.getUserId());

                        jsonObject.put("notification",notificationObj);
                        jsonObject.put("data",dataObj);
                        jsonObject.put("to",otherUser.getFcmToken());


                        callApi(jsonObject);
                    }catch(Exception e){

                    }
                }
        });
    }
    void callApi(JSONObject jsonObject){
        MediaType JSON = MediaType.get("application/json");

        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(),JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization","Bearer AAAA4egaL-E:APA91bGLGKhwon2nILQwCkyfFhgeIdlMzA5HzsS8XXm7ZPdeH8VZpHm8Z9MWyvidEl_wT-mM-e4_BAiydx30N_5n01i2NDh7lnFC5bMnSyG7VXQDYDEGr5rgKYiGYcF2P2cTcvxxrThS")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });

    }







//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        Log.d("CDA", "onBackPressed Called");
//        startActivity(new Intent(ChatActivity.this, SearchUserActivity.class));
//    }
}




