package com.playgilround.schedule.client.friend.fragment;

import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.playgilround.calendar.widget.calendar.retrofit.APIClient;
import com.playgilround.calendar.widget.calendar.retrofit.APIInterface;
import com.playgilround.calendar.widget.calendar.retrofit.Result;
import com.playgilround.schedule.client.R;
import com.playgilround.schedule.client.friend.UserJsonData;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 유저 검색 결과 다이얼로그
 */
public class UserSearchFragment extends DialogFragment {

    static final String TAG = UserSearchFragment.class.getSimpleName();

    static String resName, resBirth;

    static int resId;

    String nickName;
    SharedPreferences pref;

    TextView tvName, tvBirth;
    Button btnOK, btnCancel;

    static boolean resIsFriend;

    public static UserSearchFragment getInstance(int id, String name, String birth, boolean isFriend) {

        resId = id;
        resName = name;
        resBirth = birth;
        resIsFriend = isFriend;

        UserSearchFragment fragment = new UserSearchFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.user_search, container);

        tvName = rootView.findViewById(R.id.searchName);
        tvBirth = rootView.findViewById(R.id.searchBirth);

        tvName.setText(resName);
        tvBirth.setText(resBirth);
        pref = getActivity().getSharedPreferences("loginData", Context.MODE_PRIVATE);
        nickName = pref.getString("loginName", "");

        btnOK = rootView.findViewById(R.id.btnUserReq);
        btnOK.setOnClickListener(l -> {
            if (nickName.equals(resName)) {
                Log.d(TAG, "same nickname");
                Toast.makeText(getActivity(), "자기 자신은 친구 추가할 수 없습니다.", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "check is Friends? ->" + resIsFriend);
                if (!resIsFriend) {
                    //친구가 안되있는 유저
                    Log.d(TAG, "try new friend...-->" + resId);

//                    JsonObject userIds = new JsonObject();
//                    JsonObject userIds = new JsonObject();
                    JsonArray jsonArray = new JsonArray();
                    jsonArray.add(resId);
//                    userIds.add("user_ids", jsonArray);



                    String authToken = pref.getString("loginToken", "default");
//                    String authToken = "dfqelfwelflasdfl";

                    /**
                     * {
                     * "user_ids": [1]
                     * }
                     */
                    Log.d(TAG, "jsonObject - >" + jsonArray);

                    Retrofit retrofit = APIClient.getClient();
                    APIInterface newFriendAPI = retrofit.create(APIInterface.class);
//                    Call<JsonObject> result = newFriendAPI.postNewFriend(jsonObject, authToken);
                    Call<JsonArray> result = newFriendAPI.postNewFriend(jsonArray, authToken);

                    result.enqueue(new Callback<JsonArray>() {
                        String success;
                        String error;
                        @Override
                        public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                            if (response.isSuccessful()) {
                                success = response.body().toString();
                                Log.d(TAG, "response new friend -->" + success);

                                /**
                                 * 친구 추가 완료
                                 * [{"id":4,"name":"eee","email":"c004112@daum.net","birth":-59087664000}]
                                 *
                                 * 이미 친구
                                 * []
                                 *
                                 * auth token error
                                 * {"code":401,"message":["Unauthorized auth_token."]}
                                 */

                                if (success.equals("[]")) {
                                    //이미 친구
                                    Log.d(TAG, "already friend");
                                    Toast.makeText(getActivity(), resName + "님과는 이미 친구입니다.", Toast.LENGTH_LONG).show();
                                } else {
                                    Log.d(TAG,  "try request friend");
                                    //지금은 무조건 친구추가,
                                    // 추후에는 상대쪽에서 수락하면 추가로.
                                    Toast.makeText(getActivity(), resName + "님에게 친구 요청을 합니다!", Toast.LENGTH_LONG).show();

//                                    Type list = new TypeToken<UserJsonData>() {
//                                    }.getType();
//
//                                    UserJsonData friendList = new Gson().fromJson(success, list)

                                    getDialog().dismiss();
                                }
                            } else {
                                try {
                                    error = response.errorBody().string();
                                    Log.d(TAG, "response new friend error ->" + error);

                                    Result result = new Gson().fromJson(error, Result.class);

                                    List<String> message = result.message;

                                    if (message.contains("Unauthorized auth_token.")) {
                                        Log.d(TAG, "message -->" + message);
                                        Toast.makeText(getActivity(), "auth token error " , Toast.LENGTH_LONG).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonArray> call, Throwable t) {
                            Log.d(TAG, "response failure -->" + t.toString());
                        }
                    });

                }
//                JsonObject
            }

        });
        btnCancel = rootView.findViewById(R.id.btnUserCancel);
        btnCancel.setOnClickListener(c -> {
            Log.d(TAG, "cancel btn");
            getDialog().dismiss();
        });



        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart User Search");

        getDialog().getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}