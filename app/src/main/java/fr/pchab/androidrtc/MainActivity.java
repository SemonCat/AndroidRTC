package fr.pchab.androidrtc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import fr.pchab.webrtcclient.WebRtc;
import fr.pchab.webrtcclient.WebRtcClient;

public class MainActivity extends AppCompatActivity implements WebRtcClient.RtcListener {

    private String mSocketAddress;

    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @Bind(R.id.name_view)
    TextView mNameView;

    private NormalRecyclerViewAdapter mAdapter;

    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mSocketAddress = "http://" + getResources().getString(R.string.host);
        mSocketAddress += (":" + getResources().getString(R.string.port) + "/");

        WebRtc.init(getApplicationContext(), mSocketAddress);


        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new NormalRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        getStreamJSON();

        WebRtc.getInstance().addRtcListener(this);

        name = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("name", "");

        if (TextUtils.isEmpty(name)) {
            mNameView.setVisibility(View.GONE);
        } else {
            mNameView.setVisibility(View.VISIBLE);
            mNameView.setText(name);
        }

    }

    @Override
    protected void onDestroy() {
        WebRtc.getInstance().removeRtcListener(this);
        super.onDestroy();
    }

    @Override
    public void onUserCalling(String targetUserId) {
        Intent answerIntent = new Intent(this, RtcActivity.class);
        answerIntent.setAction(RtcActivity.INTENT_ACTION_ANSWER);
        answerIntent.putExtra("callerId", targetUserId);
        startActivity(answerIntent);
    }

    @Override
    public void onCallReady(String callId) {
        if (TextUtils.isEmpty(name)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title("Please type your name")
                            .input("Name", "", new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    name = input.toString();
                                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("name", name).commit();
                                    WebRtc.getInstance().readyToStream(name);
                                }
                            }).show();
                }
            });
        } else {
            WebRtc.getInstance().readyToStream(name);
        }

    }

    @Override
    public void onStatusChanged(String newStatus) {

    }

    @Override
    public void onLocalStream(MediaStream localStream) {

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {

    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {

    }

    private void getStreamJSON() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(mSocketAddress + "streams.json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String json = response.body().string();
                            JSONArray jsonArray = new JSONArray(json);
                            List<RTCUser> rtcUserList = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String name = jsonObject.getString("name");
                                String id = jsonObject.getString("id");
                                RTCUser rtcUser = new RTCUser(name, id);

                                rtcUserList.add(rtcUser);
                            }

                            mAdapter.refresh(rtcUserList);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });


            }
        });
    }

    public static class RTCUser {
        private String name;
        private String id;

        public RTCUser(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }


    public static class NormalRecyclerViewAdapter extends RecyclerView.Adapter<NormalRecyclerViewAdapter.NormalTextViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private List<RTCUser> mUserList;
        private Context mContext;

        public NormalRecyclerViewAdapter(Context context) {
            mContext = context;
            mUserList = new ArrayList<>();
            mLayoutInflater = LayoutInflater.from(context);
        }

        public void refresh(List<RTCUser> rtcUsers) {
            this.mUserList = rtcUsers;
            notifyDataSetChanged();
        }

        @Override
        public NormalTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new NormalTextViewHolder(mLayoutInflater.inflate(R.layout.item_text, parent, false));
        }

        @Override
        public void onBindViewHolder(NormalTextViewHolder holder, final int position) {
            holder.mTextView.setText(mUserList.get(position).getName());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent callIntent = new Intent(mContext, RtcActivity.class);
                    callIntent.setAction(RtcActivity.INTENT_ACTION_CALL);
                    callIntent.putExtra("callId", mUserList.get(position).getId());
                    mContext.startActivity(callIntent);
                }
            });
            //holder.mTextView.setOm
        }

        @Override
        public int getItemCount() {
            return mUserList.size();
        }

        public class NormalTextViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.text_view)
            TextView mTextView;

            NormalTextViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
        }
    }
}