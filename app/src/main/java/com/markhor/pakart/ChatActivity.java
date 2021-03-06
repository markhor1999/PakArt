package com.markhor.pakart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.markhor.pakart.notifications.APIService;
import com.markhor.pakart.notifications.Client;
import com.markhor.pakart.notifications.Data;
import com.markhor.pakart.notifications.Response;
import com.markhor.pakart.notifications.Sender;
import com.markhor.pakart.notifications.Token;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private ImageButton SendMessageButton, SendImageFileButton;
    private EditText UserMessageInput;
    private RecyclerView UserMessageList;
    private final List<MessageModel> messageList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;

    private String messageReceiverId, messageSenderId, saveCurrentDate, saveCurrentTime;
    private TextView receiverName;
    private CircleImageView receiverProfileImage;

    private DatabaseReference RootRef;
    private FirebaseAuth mAuth;

    private APIService apiService;
    private Boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        RootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();
        //
        messageReceiverId = getIntent().getExtras().get("VisitUserId").toString();
        //ToolBar
        mToolbar = findViewById(R.id.chat_app_bar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = layoutInflater.inflate(R.layout.chat_bar_layout,null);
        actionBar.setCustomView(action_bar_view);


        //
        receiverName = findViewById(R.id.custom_profile_name);
        receiverProfileImage = findViewById(R.id.custom_profile_image);
        //
        SendMessageButton = findViewById(R.id.send_message_button);
        //SendImageFileButton = findViewById(R.id.send_image_file_button);
        UserMessageInput = findViewById(R.id.input_message);
        //
        UserMessageList = findViewById(R.id.message_list_users);
        messageAdapter = new MessageAdapter(messageList, this);
        linearLayoutManager = new LinearLayoutManager(this);
        UserMessageList.setHasFixedSize(true);
        UserMessageList.setLayoutManager(linearLayoutManager);
        UserMessageList.setAdapter(messageAdapter);

        apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);


        DisplayReceiverInfo();


        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                SendMessageToUser();
                UserMessageInput.setText(null);
            }
        });
        FetchMessages();
    }

    private void FetchMessages()
    {
        RootRef.child("Messages").child(messageSenderId).child(messageReceiverId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        if(snapshot.exists())
                        {
                            MessageModel messageModel = snapshot.getValue(MessageModel.class);
                            messageList.add(messageModel);
                            messageAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void SendMessageToUser()
    {
        final String messageText = UserMessageInput.getText().toString();
        if(TextUtils.isEmpty(messageText))
        {
            Toast.makeText(this, "Message Cannot Be Empty", Toast.LENGTH_SHORT).show();
        }
        else
        {
            String message_sender_ref = "Messages/" + messageSenderId + "/" + messageReceiverId;
            String message_receiver_ref = "Messages/" + messageReceiverId + "/" + messageSenderId;

            DatabaseReference user_message_key = RootRef.child("Messages").child(messageSenderId).child(messageReceiverId).push();
            String message_push_id = user_message_key.getKey();

            Calendar calForDate = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-YYYY");
            saveCurrentDate = currentDate.format(calForDate.getTime());

            Calendar calForTime = Calendar.getInstance();
            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm aa");
            saveCurrentTime = currentTime.format(calForTime.getTime());

            Map messageTextBody = new HashMap();
            messageTextBody.put("message", messageText);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", saveCurrentDate);
            messageTextBody.put("from", messageSenderId);
            messageTextBody.put("type", "text");
            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(message_sender_ref + "/" + message_push_id, messageTextBody);
            messageBodyDetails.put(message_receiver_ref + "/" + message_push_id, messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(!task.isSuccessful())
                    {
                        Toast.makeText(ChatActivity.this, "Error " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        DatabaseReference databaseReference = RootRef.child(messageSenderId);
                        databaseReference.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                ModelUser user = snapshot.getValue(ModelUser.class);
                                if(notify) {
                                    sendNotification(messageReceiverId, user.getFullname(), messageText);
                                }
                                notify = false;
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }
            });

        }
    }

    private void sendNotification(final String messageReceiverId, final String fullname, final String message) {
        DatabaseReference allTokensRef = FirebaseDatabase.getInstance().getReference().child("Tokens");
        Query query = allTokensRef.orderByKey().equalTo(messageReceiverId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(messageSenderId, fullname+""+message,"New Message", messageReceiverId, R.drawable.logo);
                    Sender sender = new Sender(data, token.getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<Response>() {
                                @Override
                                public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                                    Toast.makeText(ChatActivity.this, ""+response.message(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Call<Response> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void DisplayReceiverInfo() {
        RootRef.child("Users").child(messageReceiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    final String profileImage = snapshot.child("profileimage").getValue().toString();
                    Glide.with(ChatActivity.this).load(profileImage).placeholder(R.drawable.profile).into(receiverProfileImage);
                    receiverName.setText(snapshot.child("fullname").getValue().toString());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}