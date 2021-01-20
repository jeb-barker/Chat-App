package com.example.chatapp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Chat extends AppCompatActivity {
    LinearLayout layout;
    ImageView sendButton;
    ImageView imageButton;
    EditText messageArea;
    ScrollView scrollView;
    Firebase reference1, reference2;
    private ProgressDialog dialog;
    StorageReference mStorageRef;
    FirebaseAuth mAuth;
    Bitmap bm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        layout = (LinearLayout)findViewById(R.id.layout1);
        sendButton = (ImageView)findViewById(R.id.sendButton);
        imageButton = (ImageView)findViewById(R.id.imageButton);
        messageArea = (EditText)findViewById(R.id.messageArea);
        scrollView = (ScrollView)findViewById(R.id.scrollView);

        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        scrollView.fullScroll(View.FOCUS_DOWN);
        dialog = new ProgressDialog(Chat.this);
        Firebase.setAndroidContext(this);
        reference1 = new Firebase("https://chatroom-app09-default-rtdb.firebaseio.com//messages/" + UserDetails.username + "_" + UserDetails.chatWith);
        reference2 = new Firebase("https://chatroom-app09-default-rtdb.firebaseio.com//messages/" + UserDetails.chatWith + "_" + UserDetails.username);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // do your stuff
        } else {
            mAuth.signInAnonymously();
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageArea.getText().toString();

                if(!messageText.equals("")){
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("message", messageText);
                    map.put("user", UserDetails.username);
                    map.put("image", "");
                    reference1.push().setValue(map);
                    reference2.push().setValue(map);
                    messageArea.setText("");
                }
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });



        reference1.addChildEventListener(new ChildEventListener() {
            @Override
            public synchronized void onChildAdded(DataSnapshot dataSnapshot, String s) {

                synchronized (this) {
                    Map map = dataSnapshot.getValue(Map.class);
                    String message = map.get("message").toString();
                    String userName = map.get("user").toString();
                    String imageURL = map.get("image").toString();


                    final StorageReference[] picRef = new StorageReference[1];

                    CountDownLatch done = new CountDownLatch(1);

                    if (!imageURL.equals("")) {
                        ImageView im;
                        if (userName.equals(UserDetails.username)) {
                            im = addImageBox(1);
                        } else {
                            im = addImageBox(2);
                        }
//                        CountDownLatch done = new CountDownLatch(1);
//                        dialog.setMessage("Loading...");
//                        dialog.show();

                        picRef[0] = mStorageRef.child(imageURL.substring(1));

                        picRef[0].getBytes(1024 * 1024 * 50).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                // Data for "some super long file path" is returned
                                getBM(bytes);
                                im.setImageBitmap(Bitmap.createScaledBitmap(bm, 500, 500, false));
                                scrollView.fullScroll(View.FOCUS_DOWN);
                                //done.countDown();
                                //dialog.dismiss();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {

                            }
                        });
                    }
                    else {
                        if (userName.equals(UserDetails.username)) {
                            addMessageBox("You:\n" + message, 1);
                        } else {
                            addMessageBox(UserDetails.chatWith + ":\n" + message, 2);
                        }
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }
    public ImageView addImageBox(int type){
        ImageView imageView = new ImageView(Chat.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        if(type == 1) {
            lp.setMargins(200, 5, 5, 30);
            imageView.setLayoutParams(lp);
            //imageView.setBackgroundResource(R.drawable.rounded_corner1);
        }
        else{
            lp.setMargins(5, 5, 200, 30);
            imageView.setLayoutParams(lp);
            //imageView.setBackgroundResource(R.drawable.rounded_corner2);
        }
        //textView.setGravity(Gravity.BOTTOM);
        layout.addView(imageView);

        scrollView.fullScroll(View.FOCUS_DOWN);
        return imageView;
    }
    public void getBM(byte[] bytes){

        bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    public void addMessageBox(String message, int type){
        TextView textView = new TextView(Chat.this);
        textView.setText(message);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        if(type == 1) {
            lp.setMargins(300, 5, 5, 30);
            textView.setLayoutParams(lp);
            textView.setBackgroundResource(R.drawable.rounded_corner1);
        }
        else{
            lp.setMargins(5, 5, 300, 30);
            textView.setLayoutParams(lp);
            textView.setBackgroundResource(R.drawable.rounded_corner2);
        }
        //textView.setGravity(Gravity.BOTTOM);
        layout.addView(textView);

        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    File photoFile = null;
    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // File wasn't created successfully
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {

            StorageReference picRef = mStorageRef.child(photoFile.getPath());
            picRef.putFile(Uri.fromFile(photoFile));

            Map<String, String> map = new HashMap<String, String>();
            map.put("message", "");
            map.put("user", UserDetails.username);
            map.put("image", photoFile.getPath());
            reference1.push().setValue(map);
            reference2.push().setValue(map);
            messageArea.setText("");
        }
    }
    }
