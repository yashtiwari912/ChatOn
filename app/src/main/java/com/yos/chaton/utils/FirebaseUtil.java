package com.yos.chaton.utils;

import com.google.firebase.Firebase;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;

public class FirebaseUtil {
    public static String currentUserId(){
        return FirebaseAuth.getInstance().getUid();
    }
    public static DocumentReference currentUserDetails(){
        return FirebaseFirestore.getInstance().collection("users").document(currentUserId());
    }
    public static boolean isLoggedIn(){
        if(currentUserId()!= null){
            return true;
        }
        return false;
    }
    public static CollectionReference allUserCollectionRefrence(){
        return FirebaseFirestore.getInstance().collection("users");
    }
    public static DocumentReference getChatroomRefrence(String chatroomId){
        return  FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomId);
    }

    public static CollectionReference getChatroomMessageRefrence(String chatroomId){
        return getChatroomRefrence(chatroomId).collection("chats");
    }
    public static CollectionReference allChatroomCollectionRefrence(){
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }
    public static String getChatroomId(String userId1,String userId2)
    {
        if(userId1.hashCode()<userId2.hashCode()){
            return userId1+ "_"+userId2;
        }
        else{
            return userId2+ "_"+userId1;
        }
    }
    public static DocumentReference getOtherUserFromChatroom(List<String> userIds){
        if(userIds.get(0).equals(FirebaseUtil.currentUserId())){
            return allUserCollectionRefrence().document(userIds.get(1));
        }else{
            return allUserCollectionRefrence().document(userIds.get(0));
        }
    }
    public static String timestampToString(Timestamp Timestamp){
        return new SimpleDateFormat("HH:MM").format(Timestamp.toDate());
    }
    public static void logout(){
        FirebaseAuth.getInstance().signOut();
    }

    public static StorageReference getCurrentProfilePicStorageRef(){
        return FirebaseStorage.getInstance().getReference().child("profile_pic")
                .child(FirebaseUtil.currentUserId());

    }

    public static StorageReference getOtherProfilePicStorageRef(String otherUserId){
        return FirebaseStorage.getInstance().getReference().child("profile_pic")
                .child(otherUserId);

    }


}
