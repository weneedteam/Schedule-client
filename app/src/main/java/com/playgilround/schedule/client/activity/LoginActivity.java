package com.playgilround.schedule.client.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import com.playgilround.schedule.client.R;
import com.playgilround.schedule.client.dialog.SelectFindDialog;
import com.playgilround.schedule.client.gson.HolidayJsonData;
import com.playgilround.schedule.client.gson.LoginJsonData;
import com.playgilround.schedule.client.gson.ShareScheduleJsonData;
import com.playgilround.schedule.client.gson.ShareUserScheJsonData;
import com.playgilround.schedule.client.gson.UserJsonData;
import com.playgilround.schedule.client.realm.ScheduleR;
import com.playgilround.schedule.client.retrofit.APIClient;
import com.playgilround.schedule.client.retrofit.APIInterface;
import com.playgilround.schedule.client.gson.Result;
import com.playgilround.schedule.client.gson.TokenSerialized;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class LoginActivity extends Activity implements SelectFindDialog.OnFindSetListener {

    EditText idInput, pwInput;
    CheckBox autoCheck;

    String loginId, loginPw;

    Button registBtn, loginBtn, findBtn;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    Boolean loginChecked;

    SelectFindDialog mSelectFindDialog;

    String resPushTitle;
    static final String TAG = LoginActivity.class.getSimpleName();
    String authToken;
    Realm realm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        pref = getSharedPreferences("loginData", MODE_PRIVATE);
        editor = pref.edit();

        idInput = (EditText) findViewById(R.id.idInput);
        pwInput = (EditText) findViewById(R.id.pwInput);

        autoCheck = (CheckBox) findViewById(R.id.checkbox);

        registBtn = (Button) findViewById(R.id.registBtn);

        loginBtn = (Button) findViewById(R.id.loginBtn);

        findBtn = findViewById(R.id.findBtn);

        realm = Realm.getDefaultInstance();


        /**
         * Retrofit Holiday 로그인하기전에 실행
         * 인터넷 연결 확인필
         */
        checkHoliday(new ApiCallback() {
            @Override
            public void onSuccess(String success) {
                //Holiday가 끝나면 스케줄 수락된 스케줄,
                //아직 스케줄 요청중인 스케줄 검사
                int nYear;

                Calendar calendar = new GregorianCalendar(Locale.KOREA);
                nYear = calendar.get(Calendar.YEAR);

                Log.d(TAG, "check this year ->" + nYear);

                authToken = pref.getString("loginToken", "");
                Log.d(TAG, "goSchedule authToken ->" + authToken);
                //Search Schedule API
                Retrofit retrofit = APIClient.getClient();
                APIInterface searchScheAPI = retrofit.create(APIInterface.class);
                Call<ArrayList<JsonObject>> result = searchScheAPI.getSearchSchedule(authToken, nYear);


                result.enqueue(new Callback<ArrayList<JsonObject>>() {

                    String error;
                    @Override
                    public void onResponse(Call<ArrayList<JsonObject>> call, Response<ArrayList<JsonObject>> response) {

                        /**
                         *[{
                         * 	"id": 74,
                         * 	"state": 0,
                         * 	"title": "ggggg",
                         * 	"start_time": "2018-10-22 00:00:00",
                         * 	"latitude": 0.0,
                         * 	"longitude": 0.0,
                         * 	"user": [{
                         * 		"id": 1,
                         * 		"name": "c004245",
                         * 		"email": "c004245@naver.com",
                         * 		"arrive": true
                         *        }, {
                         * 		"id": 5,
                         * 		"name": "hyun123",
                         * 		"email": "c00@naver.com",
                         * 		"arrive": false
                         *    }]
                         * }]
                         *
                         * 자기가 arrive : false 한 스케줄은 보이지 않는거 같음.
                         * realm 에 eventSetId -2 형태로 저장
                         * 해당 연도 삭제 후 다시 저장.
                         *
                         */
                        if (response.isSuccessful()) {

                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    RealmResults<ScheduleR> shareSchedule = realm.where(ScheduleR.class).equalTo("eventSetId", -2).equalTo("year", nYear).findAll();

                                    Log.d(TAG, "shareSchedule size ->" + shareSchedule.size());

                                    if (shareSchedule.size() == 0) {
                                        //ScheduleR 에 공유된 스케줄이 저장이 안 되어있음

                                        /**
                                         * Use gson json Parsing
                                         */
                                        String strSearch = response.body().toString();
                                        Log.d(TAG, "search schedule success-->" + strSearch);

                                        Type list = new TypeToken<List<ShareScheduleJsonData>>() {
                                        }.getType();

                                        //user jsonArray 이
                                        Type list2 = new TypeToken<List<ShareUserScheJsonData>>(){
                                        }.getType();

                                        List<ShareScheduleJsonData> shareData = new Gson().fromJson(strSearch, list);

//                                        Gson userGson = new Gson();

                                        List<ShareUserScheJsonData> shareUserData = new Gson().fromJson(strSearch, list2);


//                                        Log.d(TAG, "ShareData size -->" + shareData.size());
//                                        JsonArray userJson = shareData.get(0).user;

//                                        List<ShareScheduleJsonData> shareUserData = new Gson().fromJson(shareData.get(0).us)
                                        //shareData만큼 반복
//                                        for (ShareScheduleJsonData resShare : shareData) {

                                        for (int i = 0; i < shareData.size(); i++) {
                                            Log.d(TAG, "result S- >" + shareData.get(i).id + "--" + shareData.get(i).state + "--" + shareData.get(i).title + "--"
                                                + shareData.get(i).startTime + "--" + shareData.get(i).latitude + "--" + shareData.get(i).longitude);

                                            Log.d(TAG, "user Result --> " + shareUserData.get(i).user +"--" +shareUserData.get(i).user_id + "--" + shareUserData.get(i).name + "--" + shareUserData.get(i).email +
                                                "--" + shareUserData.get(i).arrive);
//                                            Log.d(TAG, "result -->" + resShare.id + "--" + resShare.state + "--" + resShare.title + "--" + resShare.startTime
//                                                    + "--" + resShare.latitude + "--" + resShare.longitude + "--");

//                                                    JsonArray resJsonArray = resShare.user;

//                                                        Log.d(TAG, "shareUser -->" + )
                                                  /*  for (ShareUserScheJsonData resUserShare : shareUserData) {
                                                        Log.d(TAG, "result To -->" + resUserShare.user + "--" + resUserShare.user_id + "--" + resUserShare.name + "--" + resUserShare.email + "--" + resUserShare.arrive);

                                                    }*/
                                        }
                                    }
                                }
                            });

                        } else {
                            try {
                                error = response.errorBody().string();
                                Log.d(TAG, "search schedule error -->" + error);

                                Result result = new Gson().fromJson(error, Result.class);

                                List<String> message = result.message;

                                if (message.contains("Unauthorized auth_token.")) {
                                    Toast.makeText(getApplicationContext(), "Auth Token Error..", Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ArrayList<JsonObject>> call, Throwable t) {
                        Log.d(TAG, "search schedule Failure -->" + t);
                    }
                });
            }

            @Override
            public void onFail(String result) {

            }
        });


        //로그인 버튼
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                String resPush = intent.getStringExtra("push");
                String resPushName = intent.getStringExtra("pushName");
                int resPushId = intent.getIntExtra("pushId",1);

                if (resPush == null) {

                }
                else if (resPush.equals("SchedulePush")) {
                    //스케줄 공유 푸쉬일 경우
                    resPushTitle = intent.getStringExtra("pushTitle");
                }

                loginId = idInput.getText().toString();
                loginPw = pwInput.getText().toString();
//                loginPw = getBase64encode(pwInput.getText().toString());

                Log.d(TAG, "loginPw base64 ->" + loginPw);


                //입력한 아이디, 비밀번호가 등록된 자료인지..
                if (loginId.equals("") || loginPw.equals("")) {
                    Toast.makeText(getApplicationContext(), "아이디나 비밀번호가 비어있습니다.", Toast.LENGTH_LONG).show();
                } else {

                    if (checkEmail(loginId)) {
                        Log.d(TAG, "이메일 형식입니다.");
                        if (checkPassWord(loginPw)) {
                            loginValidation(loginId, loginPw, new ApiCallback() {
                                @Override
                                public void onSuccess(String result) {
                                    Log.d(TAG, "로그인 성공 ----");

                                    sendFirebaseToken();

                                    Log.d(TAG, "push Title -->" + resPushTitle);
                                    Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                                    mainIntent.putExtra("pushData", resPush);
                                    mainIntent.putExtra("pushName", resPushName);
                                    mainIntent.putExtra("pushId", resPushId);

                                    if (resPushTitle != null) {
                                        mainIntent.putExtra("pushTitle", resPushTitle);
                                    }
                                    Log.d(TAG, "resPush -->" + resPush);

                                    startActivity(mainIntent);
                                    finish();
                                }

                                @Override
                                public void onFail(String result) {
                                    Toast.makeText(getApplicationContext(), "로그인 실패..", Toast.LENGTH_LONG).show();
                                    //회원가입 하시겠습니까? 다이얼로그 생성 예정

                                }
                            });
                        } else {
                            Toast.makeText(getApplicationContext(), "패스워드에 소문자, 특수문자, 숫자가 포함되어야합니다..", Toast.LENGTH_LONG).show();
                        }
                    } else {
                       Toast.makeText(getApplicationContext(), "이메일 형식이 아닙니다.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        //회원가입 버튼
        registBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        findBtn.setOnClickListener(l -> {
            if (mSelectFindDialog == null)
                mSelectFindDialog = new SelectFindDialog(this, this);

            mSelectFindDialog.show();
        });
      /*  if (pref.getString("prefId", "").equals("") || pref.getString("prefPw", "").equals("")) {
            Log.d(TAG, "서버에 저장된 정보가 없어 자동로그인을 진행할 수 없습니다..");
            autoCheck.setEnabled(false);
        } else {*/
        //자동 로그인 체크 이벤트
        autoCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                if (isChecked) {
                    editor.putString("prefAutoLogin", "check");

                    loginChecked = true;
                } else {
                    editor.putString("prefAutoLogin", "uncheck");
                    //uncheck
                    loginChecked = false;
                }
                editor.commit();
            }

        });

    }

    public void checkHoliday(final ApiCallback callback) {
        int nYear;

        Calendar calendar = new GregorianCalendar(Locale.KOREA);
        nYear = calendar.get(Calendar.YEAR);

        Log.d(TAG, "check this year start ->" + nYear);

        Retrofit retrofit = APIClient.getClient();
        APIInterface holidayAPI = retrofit.create(APIInterface.class);
        Call<ArrayList<JsonObject>> result = holidayAPI.getListHoliday(nYear);

        result.enqueue(new Callback<ArrayList<JsonObject>>() {
            @Override
            public void onResponse(Call<ArrayList<JsonObject>> call, Response<ArrayList<JsonObject>> response) {
                // Log.v(TAG, "Response - " + response.code());
                if (response.isSuccessful() && response.body() != null) {

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            // Log.d(TAG, "Holiday Check ----");
                            RealmResults<ScheduleR> holidayRS = realm.where(ScheduleR.class).equalTo("eventSetId", -1).findAll();
                            // Log.d(TAG, "holidayRS size ->" + holidayRS.size());
                            if (holidayRS.size() == 0) {
                                //ScheduleR Table 에 공휴일정보가 저장이 되어있지않음.
                                /**
                                 * Use gson json parsing.
                                 */
                                Type list = new TypeToken<List<HolidayJsonData>>(){}.getType();
                                List<HolidayJsonData> holidays = new Gson().fromJson(response.body().toString(), list);

                                Log.d(TAG, "holiday response ->" + response.body().toString());
                                Log.d(TAG, "holidayList ->" + response.body().size());
                                Log.d(TAG, "holiday gson ->" + holidays.get(0).toString());

                                Log.d(TAG, "Holiday Insert Realm ....");
                                //print
                                for (HolidayJsonData resHoliday : holidays) {

                                    Number currentIdNum = realm.where(ScheduleR.class).max("seq");

                                    int nextId;

                                    if (currentIdNum == null) {
                                        nextId = 0;
                                    } else {
                                        nextId = currentIdNum.intValue() + 1;
                                    }
                                    ScheduleR holidayR = realm.createObject(ScheduleR.class, nextId);

                                    Log.d(TAG, "holiday data id -> " + resHoliday.id + "//" + resHoliday.year + ":" + resHoliday.month + ":" + resHoliday.day + ":" + resHoliday.name);

                                    holidayR.setId(resHoliday.id);
                                    holidayR.setYear(resHoliday.year);
                                    holidayR.setMonth(resHoliday.month);
                                    holidayR.setDay(resHoliday.day);
                                    holidayR.setTitle(resHoliday.name);
                                    holidayR.setEventSetId(-1); //공휴일 EventSetId -1 고정
                                    holidayR.setColor(-1); //공휴일 하늘색.
                                    holidayR.setState(-1); //상태 -1 (좌측 노란색)
                                }

                            } else {
                                Log.d(TAG, "exist HolidayR");
                            }
                            Log.d(TAG, "success holidayR");
                            callback.onSuccess("success");

                        }
                    });
                }
            }
            @Override
            public void onFailure(Call<ArrayList<JsonObject>> call, Throwable t) {
                Log.e(TAG, "error - " + t.toString());
            }
        });
    }

    /**
     * 이 부분에 자동로그인 검사를 넣는 이유는,
     * 회원가입 후에 로그인화면으로 올 때에 서버에 저장된 정보가 있으면
     * 다시 자동로그인 체크박스를 활성화 시켜주기위해.
     */
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        //이전에 자동로그인을 체크했나 ?
        String strAuto = pref.getString("prefAutoLogin", "");
        Log.d(TAG, "autoCheck -->" + strAuto);

        String autoId = pref.getString("prefEmail", "");
        String autoPw = pref.getString("prefPw", "");

        Log.d(TAG, "id info --> " + autoId + "////" + autoPw);
        //기존에 자동로그인을 체크했을 경우 아이디 비밀번호 표시
        if (strAuto.equals("check")) {
            idInput.setText(autoId);
            pwInput.setText(autoPw);
            autoCheck.setChecked(true);
        } else if (autoId.equals("") || autoPw.equals("")) {
            Log.d(TAG, "서버에 저장된 정보가 없어 자동로그인을 체크가 불가능합니다..");
            autoCheck.setEnabled(true);
        } else {
            Log.d(TAG, "체크 안한경우.");
            autoCheck.setChecked(false);
            autoCheck.setEnabled(true);
        }
    }

    //check login info validate

    /**
     * SharedPreference 에 데이터가 없으면, 회원가입이 필요.
     * SharedPreference 에 데이터가 있지만, 입력받은 정보랑 다르면 저장된 정보가 아니다.
     * 같을 경우에만 true
     *
     * Base64로 인코딩된 암호를 한번 디코딩 작업을 한 후, 입력받은 패스워드와 비교.
     * @param id
     * @param pw
     * @return
     */
    private void loginValidation(String id, String pw, final ApiCallback callback) {
        /**
         * SignIn http://localhost:3000/users/sign_in
         * {
         * “user”: {
         * “email”: “tjstlr2010@gmail.com”,
         * “password”: “js30211717”
         * }
         * }
         */
        Log.d(TAG, "pw validate ->" + pw);
        JsonObject jsonObject = new JsonObject();
        JsonObject userJsonObject = new JsonObject();
        userJsonObject.addProperty("email", id);
        userJsonObject.addProperty("password", pw);
        jsonObject.add("user", userJsonObject);

        Log.d(TAG, "jsonresult ->" + jsonObject);
        /**
         *   Parameters: {"_json"=>"{\"user\":{\"email\":\"c004245@naver.com\",\"password\":\"whgusdnr1!\"}}",
         *   "session"=>{"_json"=>"{\"user\":{\"email\":\"c004245@naver.com\",\"password\":\"whgusdnr1!\"}}"}}
         */
//        Call<JsonObject> res = RequestLogin.getInstance().getService().postSignIn(jsonObject);
//        res.enqueue(new Callback<JsonObject>() {
        Retrofit retrofit = APIClient.getClient();
        APIInterface loginAPI = retrofit.create(APIInterface.class);
        Call<JsonObject> result = loginAPI.postSignIn(jsonObject);

        result.enqueue(new Callback<JsonObject>() {

            String error;
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                /**
                 * 성공 {"id":1,"name":"c004245","email":"c004245@naver.com","phone":"01027327899","birth":"1997-08-02T00:00:00.000Z","auth_token":"3Ay2uTVEVtwCe5YscQrq","created_at":"2018-09-25T05:17:34.000Z","updated_at":"2018-09-26T04:07:15.949Z"}
                 * 실패
                 */

                if (response.isSuccessful()) {
                    Log.d(TAG, "response success -->" + response.body().toString());
                    String jsonArray = response.body().toString();
                    Type list = new TypeToken<LoginJsonData>() {
                    }.getType();
                    LoginJsonData loginList = new Gson().fromJson(jsonArray, list);

                    int loginId = loginList.id;
                    String loginName = loginList.name;
                    String loginToken = loginList.token;

                    Log.d(TAG, "result name and token ->" + loginName + "--" + loginToken);

                    editor.putInt("loginId", loginId);
                    editor.putString("loginName", loginName);
                    editor.putString("loginToken", loginToken);

                    //자동 로그인 테스트
                    editor.putString("prefEmail", id);
                    editor.putString("prefPw", pw);

                    editor.apply();
                    callback.onSuccess("success");
                } else {
                    try {
                        error = response.errorBody().string();
                        Log.d(TAG, "response error ->" + error);

                        Result result = new Gson().fromJson(error, Result.class);

                        int code = result.code;
                        List<String> message = result.message;

                        Log.d(TAG, "Login fail code and message ----> " + code +"--"+ message);

                        /**
                         * 이메일, 패스워드 틀릴 경우 동일하게 처리 예정
                         * [The password is incorrect.]
                         * ["The email is incorrect."]
                         */
                        if (message.contains("The email is incorrect.") || message.contains("The password is incorrect.")) {
                            Log.d(TAG, "email and password error");
                            Toast.makeText(getApplicationContext(), "이메일이나 패스워드가 없는 정보입니다." , Toast.LENGTH_LONG).show();
                        }

//                        callback.onFail("fail");

                    } catch (Exception e) {

                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d(TAG, "result Failure -->" + t);
                callback.onFail("fail");
                //서버가 죽은경우?
            }
        });
    }

    @Override
    public void onFindSet(String check) {
        Log.d(TAG, "onFindSet check..." + check);

        Intent intent = new Intent(this, FindAccountActivity.class);
        intent.putExtra("check", check);
        startActivity(intent);
      /*  if (check.equals("Email")) {
            Intent intent = new Intent(this, )
        } else if (check.equals("Password")) {

        }*/
    }

    //Firebase Token Retrofit
    /**
     * 등록 토큰이 변경되는 경우
     * 앱에서 인스턴스 ID 삭제
     * 새 기기에서 앱 복원
     * 사용자가 앱 삭제/재설치
     * 사용자가 앱 데이터 소거
     */
    private void sendFirebaseToken() {
        String resToken = null;
        Log.d(TAG, "sendFirebase Token start..");
        //get Token
        FirebaseInstanceId.getInstance().getToken();

        if (FirebaseInstanceId.getInstance().getToken() != null) {
            Log.d(TAG, "token result -->" + FirebaseInstanceId.getInstance().getToken());

            resToken = FirebaseInstanceId.getInstance().getToken();
        }
        JsonObject jsonObject = new JsonObject();
        JsonObject userJsonObject = new JsonObject();

        Log.d(TAG, "resultToken ->" + resToken);

        userJsonObject.addProperty("fcm_token", resToken);

        jsonObject.add("user", userJsonObject);

        Log.d(TAG, "user jsonbody ->" + jsonObject);

        String authToken =pref.getString("loginToken", "default");

        Log.d(TAG, "authToken --->" + authToken);

        Retrofit retrofit = APIClient.getClient();
        APIInterface tokenAPI = retrofit.create(APIInterface.class);
        Call<TokenSerialized> result = tokenAPI.postToken(jsonObject, authToken);
//        Call<TokenSerialized> res = RequestFCMToken.getInstance().getService().postToken(jsonObject, authToken);
//        Log.d(TAG, "res --->" + res.toString());
        result.enqueue(new Callback<TokenSerialized>() {
            @Override
            public void onResponse(Call<TokenSerialized> call, Response<TokenSerialized> response) {

                if (response.isSuccessful()) {
                    Log.d(TAG, "response FCM ->" + response.body().toString());
                } else {
                    try {
                        //response error FCM ->{"code":401,"message":["Unauthorized auth_token."]}
                        Log.d(TAG, "response error FCM ->" + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<TokenSerialized> call, Throwable t) {
                Log.d(TAG, "fail FCM ->" + t.toString());
            }
        });
    }


    public interface ApiCallback{
        void onSuccess(String result);
        void onFail(String result);
    }
/*
        String idt = pref.getString("prefEmail", "");
        String pwt = pref.getString("prefPw", "");

        String base64pw = getBase64decode(pwt);
        Log.d(TAG, "idt --->" + idt + "--" + pwt + "--" + base64pw);
        //Preference 에 저장된 정보가 없어 검사를 할 수없을 경우 (최초)
        if (idt.equals("") || base64pw.equals("")) {
            Log.d(TAG, "회원가입이 필요합니다..");
            Toast.makeText(this, "회원가입이 필요합니다.", Toast.LENGTH_LONG).show();
            return false;
        }
        //입력받은 id,pw와 저장된 id,pw 같을경우
        else if (idt.equals(id) && base64pw.equals(pw)) {
            //login success
            return true;
        } else if (idt != id || base64pw != pw) {
            //저장 된 정보랑 다를 경우
            Log.d(TAG, "저장 된 정보가 아닙니다..");
            Toast.makeText(this, "저장 된 정보가 아닙니다.", Toast.LENGTH_LONG).show();
            return false;
        } else {
            //login failed
            return false;
        }
    }*/

    /**
     * 이메일 형식 체크
     */
    private boolean checkEmail(String email) {
        String mail = "^[_a-zA-Z0-9-\\.]+@[\\.a-zA-Z0-9-]+\\.[a-zA-Z]+$";
        Pattern p = Pattern.compile(mail);
        Matcher m = p.matcher(email);
        return m.matches();
    }


    /**
     * 패스워드 유효성검사
     * 특수문자, 숫자, 소문자 입력
     * 정규식 (영문, 숫자, 특수문자 조합, 4~20자리)
     */
    private boolean checkPassWord(String password) {
        String valiPass =  "^(?=.*\\d)(?=.*[~`!@#$%\\^&*()-])(?=.*[a-z]).{4,20}$";
        Pattern pattern = Pattern.compile(valiPass);

        Matcher matcher = pattern.matcher(password);

        return matcher.matches();
    }

    /**
     * Base64 디코딩(해독) password
     * @param content
     * @return
     */
    public static String getBase64decode(String content){

        return new String(Base64.decode(content, 0));

    }

}