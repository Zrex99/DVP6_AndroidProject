package com.example.projectremnant.Sessions.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projectremnant.Character.CharacterActivity;
import com.example.projectremnant.Checklist.ChecklistActivity;
import com.example.projectremnant.DataModels.Character;
import com.example.projectremnant.DataModels.Session;
import com.example.projectremnant.DataModels.User;
import com.example.projectremnant.R;
import com.example.projectremnant.Sessions.Fragments.SessionFormFragment;
import com.example.projectremnant.Sessions.Fragments.SessionListFragment;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

public class SessionActivity extends AppCompatActivity implements SessionFormFragment.OnCreateTapped, SessionListFragment.SessionListFragmentListener {

    //TODO: Session package is good to go and approved for final review.

    private static final String TAG = "SessionActivity.TAG";

    public static final int REQUEST_SESSIONDETAILS = 2;

    public static final String EXTRA_CHARACTER = "EXTRA_CHARACTER";

    DatabaseReference mDatabaseSessionCountRef = FirebaseDatabase.getInstance().getReference().child("sessionCount");
    DatabaseReference mDatabaseUserSessions = FirebaseDatabase.getInstance().getReference().child("users");

    private Character mCharacter;
    private User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_activity);

        Button btn_available = findViewById(R.id.btn_available);
        btn_available.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAvailableGroupsFragment();
            }
        });

        Button btn_joined = findViewById(R.id.btn_joined);
        btn_joined.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchJoinedGroupsFragment();
            }
        });


        Intent starter = getIntent();
        if(starter != null) {
            mUser = (User) starter.getSerializableExtra(CharacterActivity.EXTRA_USER);
            mCharacter = (Character) starter.getSerializableExtra(EXTRA_CHARACTER);
            String searchOption = starter.getStringExtra(ChecklistActivity.EXTRA_OPTION);
            if(searchOption.equals("search")){
                //Launch the available sessions lists fragments.
                launchAvailableGroupsFragment();
            }else if(searchOption.equals("create")){
                //Launch the session form fragment.
                launchFormFragment();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUEST_SESSIONDETAILS) {
            if(data != null) {
                mUser = (User) data.getSerializableExtra(CharacterActivity.EXTRA_USER);
                mCharacter = (Character) data.getSerializableExtra(CharacterActivity.EXTRA_CHARACTER);
                launchJoinedGroupsFragment();
            }
        }
    }

    /**
     * Methods to manipulate the view layout for showing fragments.
     */

    private void hideListFrameLayouts() {
        FrameLayout fl_list = findViewById(R.id.fragment_container_list);
        fl_list.setVisibility(View.GONE);

        Button btn_available = findViewById(R.id.btn_available);
        btn_available.setVisibility(View.GONE);

        Button btn_joined = findViewById(R.id.btn_joined);
        btn_joined.setVisibility(View.GONE);

        FrameLayout fl_container = findViewById(R.id.fragment_container);
        fl_container.setVisibility(View.VISIBLE);
    }
    private void hideFormFrameLayouts() {
        FrameLayout fl_list = findViewById(R.id.fragment_container_list);
        fl_list.setVisibility(View.VISIBLE);

        Button btn_available = findViewById(R.id.btn_available);
        btn_available.setVisibility(View.VISIBLE);

        Button btn_joined = findViewById(R.id.btn_joined);
        btn_joined.setVisibility(View.VISIBLE);

        FrameLayout fl_container = findViewById(R.id.fragment_container);
        fl_container.setVisibility(View.GONE);
    }

    /**
     * Methods to load the fragments.
     */

    private void launchFormFragment() {
        hideListFrameLayouts();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, SessionFormFragment.newInstance())
                .commit();
    }

    private void launchJoinedGroupsFragment() {
        swapButtonColors(true);
        hideFormFrameLayouts();
        setTitle("Joined Sessions");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_list, SessionListFragment.newInstance(true, mUser, mCharacter))
                .commit();
    }

    private void launchAvailableGroupsFragment() {
        swapButtonColors(false);
        hideFormFrameLayouts();
        setTitle("Available Sessions");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_list, SessionListFragment.newInstance(false, mUser, mCharacter))
                .commit();
    }

    private void swapButtonColors(boolean _joined) {
        if(_joined) {
            Button btn_joined = findViewById(R.id.btn_joined);
            btn_joined.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            btn_joined.setTextColor(Color.WHITE);

            Button btn_available = findViewById(R.id.btn_available);
            btn_available.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            btn_available.setTextColor(Color.WHITE);
        }else {
            Button btn_joined = findViewById(R.id.btn_joined);
            btn_joined.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            btn_joined.setTextColor(Color.WHITE);

            Button btn_available = findViewById(R.id.btn_available);
            btn_available.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            btn_available.setTextColor(Color.WHITE);
        }
    }

    private void updateSessionsCount() {

        ValueEventListener countListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long count = dataSnapshot.getValue(Long.class);
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("sessionCount");
                ref.setValue(count + 1);
                mDatabaseSessionCountRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        mDatabaseSessionCountRef.addValueEventListener(countListener);
    }

    /**
     * Interface method for the session form fragment.
     */

    @Override
    public void createTapped(String _sessionName, String _sessionDescription, String _playerLimit) {
        //Update the database with the session and close the fragment/launch the joined or available session list fragment.

        //TODO: Shoddy fix but it will hold for now.
        JSONArray characterArray = new JSONArray();
        characterArray.put(mCharacter.getCharacterForSession());

        final Session session = new Session(_sessionName, Integer.parseInt(_playerLimit), characterArray.toString(), _sessionDescription);
        String sessionDetails = session.getSessionDetails();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

        ref.child("sessions").child(session.getSessionId()).setValue(sessionDetails).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateSessionsCount();
                mUser.updateJoinedSessions(session.getSessionId());
                //If i need to i can add a success listener to this.
                mDatabaseUserSessions.child(mUser.getUserName()).child("joinedSessionsIds").setValue(mUser.getJoinedSessionsIds());

                //Now that the session was created, go to the joined sessions fragment list.
                launchJoinedGroupsFragment();
            }
        });


    }

    /**
     * Interface method for the session list fragment.
     */

    @Override
    public void sessionClicked(Session _session) {
        Intent starter = new Intent(getApplicationContext(), SessionDetailsActivity.class);
        starter.putExtra(SessionDetailsActivity.EXTRA_SESSION, _session);
        starter.putExtra(CharacterActivity.EXTRA_USER, mUser);
        starter.putExtra(CharacterActivity.EXTRA_CHARACTER, mCharacter);
        startActivityForResult(starter, REQUEST_SESSIONDETAILS);
    }
}
