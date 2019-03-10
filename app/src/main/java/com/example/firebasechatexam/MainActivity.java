package com.example.firebasechatexam;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;



import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    private FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder> mFirebaseAdapter;
    private RecyclerView mMessageRecyclerView;

    // Firebase 인스턴스 변수
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    // 사용자 이름과 사진
    private String mUsername;
    private String mPhotoUrl;

    // Google
    private GoogleApiClient mGoogleApiClient;

    // 데이터 베이스
    private DatabaseReference mFirebaseDatabaseReference;
    private EditText mMessageEditText;
    public static final String MESSAGE_CHILD = "messages";

    public static class MessageViewHolder extends RecyclerView.ViewHolder{
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;
        public MessageViewHolder(View v){
            super(v);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            messengerTextView = itemView.findViewById(R.id.messengerTextView);
            messengerImageView = itemView.findViewById(R.id.messengerImageView);
        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase 실시간 데이터베이스 초기화
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mMessageEditText = findViewById(R.id.message_edit);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        FirebaseApp.initializeApp(this);


        // Firebase 인증 초기화
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if(mFirebaseUser == null){
            // 인증이 안 되었다면 인증 화면으로 이동
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }else{
            mUsername = mFirebaseUser.getDisplayName();
            if(mFirebaseUser.getPhotoUrl() != null){
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }

        }

        mMessageRecyclerView = findViewById(R.id.message_recycler_view);

        // 보내기 버튼
        findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatMessage chatMessage = new ChatMessage(mMessageEditText.getText().toString(), mUsername, mPhotoUrl, null);
                mFirebaseDatabaseReference.child(MESSAGE_CHILD).push().setValue(chatMessage);
                mMessageEditText.setText("");
            }
        });

        // 쿼리 수행 위치
        Query query = mFirebaseDatabaseReference.child(MESSAGE_CHILD);
        //옵션
        FirebaseRecyclerOptions<ChatMessage> options = new FirebaseRecyclerOptions.Builder<ChatMessage>()
                .setQuery(query, ChatMessage.class)
                .build();

        // 어댑터
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>(options) {
            @Override
            protected void onBindViewHolder(MessageViewHolder holder, int position, ChatMessage model) {
                holder.messageTextView.setText(model.getText());
                holder.messengerTextView.setText(model.getName());
                if (model.getPhotoUrl() == null) {
                    holder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_account_circle_black_24dp));
                } else {
                    Glide.with(MainActivity.this).load(model.getPhotoUrl()).into(holder.messengerImageView);
                }
            }

            @Override
            public MessageViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
                return  new MessageViewHolder(view);
            }

        };

        // 리사이클러뷰에 레이아웃 매니저와 어댑터 설정
        mMessageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);


        // 소프트 키에 가려 보이지 않는 최신  메시지에 대한 처리
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                LinearLayoutManager layoutManager = (LinearLayoutManager) mMessageRecyclerView.getLayoutManager();
                int lastVisiblePostion = layoutManager.findLastCompletelyVisibleItemPosition();

                if(lastVisiblePostion == -1 || (positionStart >= (friendlyMessageCount -1) &&
                        lastVisiblePostion == (positionStart -1))){
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom){
                if(bottom < oldBottom){
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mMessageRecyclerView.smoothScrollToPosition(mFirebaseAdapter.getItemCount());
                        }


                    }, 100);
                }
            }


        });




    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult){
        Toast.makeText(this, "Google Play Services error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = "";
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();;
        // FirebaseRecyclerAdapter 실시간 쿼리 시작
        mFirebaseAdapter.startListening();
    }

    @Override
    protected void onStop(){
        super.onStop();
        // FirebaseRecyclerAdapter 실시간 쿼리 중지
        mFirebaseAdapter.stopListening();
    }


}
