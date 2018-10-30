package com.playgilround.schedule.client.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import com.playgilround.schedule.client.R;
import com.playgilround.schedule.client.base.app.BaseActivity;
import com.playgilround.schedule.client.dialog.InputLocationDialog;
import com.playgilround.schedule.client.dialog.SelectDateDialog;
import com.playgilround.schedule.client.dialog.SelectEventSetDialog;
import com.playgilround.schedule.client.gson.UserJsonData;
import com.playgilround.schedule.client.listener.OnTaskFinishedListener;
import com.playgilround.schedule.client.realm.EventSetR;
import com.playgilround.schedule.client.realm.ScheduleR;
import com.playgilround.schedule.client.retrofit.APIClient;
import com.playgilround.schedule.client.retrofit.APIInterface;
import com.playgilround.schedule.client.gson.Result;
import com.playgilround.schedule.client.task.eventset.LoadEventSetRMapTask;
import com.playgilround.schedule.client.utils.CalUtils;
import com.playgilround.schedule.client.utils.DateUtils;
import com.playgilround.schedule.client.utils.ToastUtils;

import org.joda.time.DateTime;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 18-06-29
 * 스케줄을 위치 등등, 자세하게 적을수있는 Activity
 *
 * 스케줄 항목, 시간, 위치, 내용 Realm적용 후
 * 각각 Dialog 확인만 눌러도
 * Realm 에 바로 저장되는 문제가 있어서,
 *
 * 최종 confirm()에서만 Realm Transaction이 실행되도록 수정.
 */
public class ScheduleDetailActivity extends BaseActivity implements View.OnClickListener,
        OnTaskFinishedListener<Map<Integer, EventSetR>>, SelectEventSetDialog.OnSelectEventSetListener, SelectDateDialog.OnSelectDateListener, InputLocationDialog.OnLocationBackListener {

    static final String TAG = ScheduleDetailActivity.class.getSimpleName();

    public static int UPDATE_SCHEDULE_CANCEL = 1;
    public static int UPDATE_SCHEDULE_FINISH = 2;

    private View vSchedule;
    private ImageView ivEventIcon;
    private EditText etTitle, etDesc;

    private TextView tvEventSet, tvTime, tvLocation, tvShare;
    private Map<Integer, EventSetR> mEventSetsMap;

    private ScheduleR mSchedule;

    private int scheId;
    public static String SCHEDULE_OBJ = "schedule.obj";
    public static String CALENDAR_POSITION = "calendar.position";

    private int mPosition = -1;

    private SelectEventSetDialog mSelectEventSetDialog;
    private SelectDateDialog mSelectDateDialog;
    private InputLocationDialog mInputLocationDialog;

    Realm realm;

    String location; //위치
    int eventColor, eventSetId; //뷰 색상, 스케줄분류 아이디
    int resYear, resMonth, resDay;
    long resTime;

    private Button btnArrived;



    //realm 에 time을 보기 편하게 변환
    private String HUMAN_TIME_FORMAT = "";
    private String resultTime;
    //선택된 스케줄 Primary 데이터
    int curScheduleSeq;

    SharedPreferences pref;

    //자기자신의 아이디
    int userId;

    String authToken;
    @Override
    protected void bindView() {
        setContentView(R.layout.activity_schedule_detail);
        TextView tvTitle = searchViewById(R.id.tvTitle);
        tvTitle.setText(getString(R.string.schedule_event_detail_setting));

        searchViewById(R.id.tvCancel).setOnClickListener(this);
        searchViewById(R.id.tvFinish).setOnClickListener(this);
        searchViewById(R.id.llScheduleEventSet).setOnClickListener(this);
        searchViewById(R.id.llScheduleTime).setOnClickListener(this);
        searchViewById(R.id.llScheduleLocation).setOnClickListener(this);
        searchViewById(R.id.llShare).setOnClickListener(this);

        vSchedule = searchViewById(R.id.vScheduleColor);
        ivEventIcon = searchViewById(R.id.ivScheduleEventSetIcon);

        etTitle = searchViewById(R.id.etScheduleTitle);
        etDesc = searchViewById(R.id.etScheduleDesc);
        HUMAN_TIME_FORMAT = getString(R.string.human_time_format);

        tvEventSet = searchViewById(R.id.tvScheduleEventSet);
        tvTime = searchViewById(R.id.tvScheduleTime);
        tvLocation = searchViewById(R.id.tvScheduleLocation);

        tvShare = searchViewById(R.id.tvShare);

        btnArrived = searchViewById(R.id.btnArrived);
        searchViewById(R.id.btnArrived).setOnClickListener(this);

        pref = getSharedPreferences("loginData", Context.MODE_PRIVATE);
        authToken = pref.getString("loginToken", "default");
        realm = Realm.getDefaultInstance();

    }

    @Override
    protected void initData() {
        super.initData();
        mEventSetsMap = new HashMap<>();
//        mSchedule = (ScheduleR)getIntent().getSerializableExtra(SCHEDULE_OBJ);

        curScheduleSeq = (int) getIntent().getSerializableExtra(SCHEDULE_OBJ);
//        Log.d(TAG, "mSchedule Result ->" + curScheduleSeq);

        /**
         * 스케줄 클릭 시 seq 값을 받아서 스케줄 세팅.
         */
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
//                Log.d(TAG, "find Schedule Data =====>"+ curScheduleSeq);
                ScheduleR resScheduleR = realm.where(ScheduleR.class)
                        .equalTo("seq", curScheduleSeq).findFirst();



                 mSchedule = resScheduleR;

                scheId = mSchedule.getScheId();

                if (mSchedule.getEventSetId() == -2) {
                    btnArrived.setVisibility(View.VISIBLE);
                }
            }
        });

        mPosition = getIntent().getIntExtra(CALENDAR_POSITION, -1);

//        new LoadEventSetMapTask(this, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new LoadEventSetRMapTask(this, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void bindData() {
        super.bindData();
        setScheduleData();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvCancel:
                setResult(UPDATE_SCHEDULE_CANCEL);
                finish();
                break;
            case R.id.tvFinish:
                confirm();
                break;
            case R.id.llScheduleEventSet:
                //스케줄 제목적혀있는 레이아웃 클릭
                showSelectEventSetDialog();
                break;

            case R.id.llScheduleTime:
                //날짜 선택 레이아웃 클릭 시.
                showSelectDateDialog();
                break;

            case R.id.llScheduleLocation:
                //위치 선택 레이아웃 클릭
                showInputLocationDialog();
                break;

            case R.id.llShare:
//                showS
                break;

            case R.id.btnArrived:
                //도착완료버튼 클릭

                arrivedDest(new LoginActivity.ApiCallback() {
                    @Override
                    public void onSuccess(String success) {

                        //도착완료 버튼이 눌리면 도착 순위 작업
                        //
                        /**
                         * {
                         * 	"id": 106,
                         * 	"title": "kill",
                         * 	"state": 0,
                         * 	"start_time": "2018-10-30 00:00:00",
                         * 	"latitude": 0.0,
                         * 	"longitude": 0.0,
                         * 	"user": [{
                         * 		"id": 1,
                         * 		"name": "c004245",
                         * 		"email": "c004245@naver.com",
                         * 		"arrive": true,
                         * 		"arrived_at": "2018-10-30 13:50:46"
                         *        }, {
                         * 		"id": 1,
                         * 		"name": "c004245",
                         * 		"email": "c004245@naver.com",
                         * 		"arrive": true,
                         * 		"arrived_at": "2018-10-30 13:50:46"
                         *    }, {
                         * 		"id": 5,
                         * 		"name": "hyun123",
                         * 		"email": "c00@naver.com",
                         * 		"arrive": true
                         *    }]
                         * }
                         */
                        Retrofit retrofit = APIClient.getClient();
                        APIInterface getDetailSche = retrofit.create(APIInterface.class);

                        Log.d(TAG, "authToken ->" + authToken + "--" + scheId);
                        Call<JsonObject> result = getDetailSche.getScheduleDetail(authToken, scheId);

                        result.enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                if (response.isSuccessful()) {
                                    Log.d(TAG, "result -->" + response.body().toString());
                                } else {
                                    try {
                                        String error = response.errorBody().string();

                                        Log.d(TAG, "result error -->" + error);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable t) {
                                Log.d(TAG, "t-->" + t.toString());
                            }
                        });
                    }

                    @Override
                    public void onFail(String result) {

                    }
                });


        }
    }


    //도착 완료 버튼 클릭
    private void arrivedDest(final LoginActivity.ApiCallback callback) {
        DateTime dateTime = new DateTime();
        String retTime = dateTime.toString("yyyy-MM-dd HH:mm:ss");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("arrived_at", retTime);

        Retrofit retrofit = APIClient.getClient();
        APIInterface postArriveSche = retrofit.create(APIInterface.class);
        Call<JsonObject> result = postArriveSche.postScheduleArrive(jsonObject, authToken, scheId);

        result.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "arrive result ->" + response.body().toString());
                    callback.onSuccess("success");

                } else {
                    try {
                        String error = response.errorBody().string();

                        Result result = new Gson().fromJson(error, Result.class);

                        List<String> message = result.message;

                        if (message.contains("Unauthorized auth_token.")) {
                            Toast.makeText(getApplicationContext(),"도착완료 토큰 에러입니다. 앱을 재실행해주세요.", Toast.LENGTH_LONG).show();
                        } else if (message.contains("Not found schedule.")) {
                            Toast.makeText(getApplicationContext(), "스케줄을 찾을 수 없습니다..", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }
    //확인 버튼
    private void confirm() {

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Log.d(TAG, "confirm");
                if (etTitle.getText().length() != 0) {
                    mSchedule.setTitle(etTitle.getText().toString());
                    mSchedule.setDesc(etDesc.getText().toString());

                    Log.d(TAG, "mSchedule getDesc -->" + mSchedule.getDesc());

                    /**
                     * onSelectEventSet
                     */
                    mSchedule.setColor(eventColor);
                    mSchedule.setEventSetId(eventSetId);

                    /**
                     * onSelectDate
                     */
                    mSchedule.setYear(resYear);
                    mSchedule.setMonth(resMonth);
                    mSchedule.setDay(resDay);
                    mSchedule.setTime(resTime);
                    mSchedule.sethTime(resultTime);

                    Log.d(TAG, "mSchedule getDate -->" + mSchedule.getYear() + "/" + mSchedule.getMonth() + "/" + mSchedule.getDay() + "/" + mSchedule.getTime() + "/" + mSchedule.gethTime());

                    /**
                     * onLocationBack
                     */
                    mSchedule.setLocation(location);

//                    mSchedule.setEventSetId();
                    setResult(UPDATE_SCHEDULE_FINISH);
//                    Log.d(TAG, "mSchedule Check title --->" + mSchedule.getTitle());
//                    Log.d(TAG, "mSchedule Check Desc --->" + mSchedule.getDesc());
//                    Log.d(TAG, "mschedule check seq --->" + mSchedule.getSeq());

//                    ScheduleR schedule = realm.where(Schedule.)
                  /*  new UpdateScheduleRTask(getApplicationContext(), new OnTaskFinishedListener<Boolean>() {
                        @Override
                        public void onTaskFinished(Boolean data) {
                            setResult(UPDATE_SCHEDULE_FINISH);
                            finish();
                        }
                    }, mSchedule).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);*/
//                  getMyUserID();
                } else {
                    ToastUtils.showShortToast(getApplicationContext(), R.string.schedule_input_content_is_no_null);
                }
            }
        });

        finish();

    }
    //공유 유저 칸 입력을 위해, 자기자신의 id 값 얻기
   private void getMyUserID() {

       /**
        * {
        *     "user" : {
        *        "name" : "test4"
        *     }
        * }
        */
        //자기 자신 유저 아이디 얻기


        String nickName = pref.getString("loginName", "");
        Log.d(TAG, "friend nickName -->" + nickName);

        JsonObject jsonObject = new JsonObject();
        JsonObject userJsonObject = new JsonObject();

        userJsonObject.addProperty("name", nickName);

        jsonObject.add("user", userJsonObject);
        Retrofit retrofit = APIClient.getClient();
        APIInterface getUserId = retrofit.create(APIInterface.class);
        Call<JsonObject> result = getUserId.postUserSearch(jsonObject, authToken);

        result.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    String strResponse = response.body().toString();

                    Type list  = new TypeToken<UserJsonData>() {
                    }.getType();

                    Log.d(TAG, "get DetailUser Id ->" + strResponse);

                    UserJsonData userList = new Gson().fromJson(strResponse, list);

                    userId = userList.id;

                    if (userList == null) {
                        Log.d(TAG, "error Detail UserInfo");
                    } else {
                        Log.d(TAG, "userId ---->" + userId);
                        addScheduleServer(userId);
                    }
                } else {
                    try {
                        String error = response.errorBody().string();

                        Log.d(TAG, "response Detail Error -->" + error);

                        Result result = new Gson().fromJson(error, Result.class);

                        int code = result.code;
                        List<String> message = result.message;

                        Log.d(TAG, "Detail Info fail...-->" + code + "--"+ message);


                        if (message.contains("Not found user.") || message.contains("Unauthorized auth_token.")) {
                            Log.d(TAG, "message ->" + message);
                            Toast.makeText(getApplicationContext(), "그런 유저는 없어요 ㅋ", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d(TAG, "fail Detail Id ->" + t.toString());
            }
        });
    }

    //서버에 스케줄 추가된 내용 저장
    /**
     * {
     *     "title": "오늘창업허브 ㅋㅋ",
     *     "start_time": "2018-09-30 13:00:00",
     *     "content": "adasdsadasdadsadasadasd",
     *     "latitude": 37.6237604,
     *     "longitude": 126.9218479,
     *     "user_ids" [ 2, 3 ]
     * }
     */
    public void addScheduleServer(int userId) {
        Log.d(TAG, "addScheduleServer -->" + userId);
        Log.d(TAG, "addSchedule ->" + etTitle.getText().toString() + "--" + resTime + "--" + etDesc.getText().toString());
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonObject.addProperty("title", etTitle.getText().toString());
        jsonObject.addProperty("start_time", resYear +"-"+resMonth+"-"+resDay);
        jsonObject.addProperty("content", etDesc.getText().toString());
        jsonObject.addProperty("latitude", 37.6237604);
        jsonObject.addProperty("longitude", 126.9218479);
        jsonArray.add(userId);
//        jsonObject.addProperty("user_ids", [1]);

        jsonObject.add("users_ids", jsonArray);
        Log.d(TAG, "jsonObject add ->" + jsonObject + "--" + authToken);

        Retrofit retrofit = APIClient.getClient();
        APIInterface postNewSche = retrofit.create(APIInterface.class);
        Call<JsonObject> result = postNewSche.postNewSchedule(jsonObject,  authToken);

        result.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    String success = response.body().toString();
                    Log.d(TAG, "success schedule -->" + success);
                } else  {
                    try {
                        String error = response.errorBody().string();

                        Log.d(TAG, "error schedule -->" + error);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d(TAG, "fail schedule -> " + t.toString());
            }
        });
    }

    @Override
    public void onTaskFinished(Map<Integer, EventSetR> data) {
        Log.d(TAG, "onTask eventDetail --> "+ data.size());
        mEventSetsMap = data;

        EventSetR eventSet = new EventSetR();
        eventSet.setName(getString(R.string.menu_no_category));

        mEventSetsMap.put(eventSet.getSeq(), eventSet);
//        EventSetR current = mEventSetsMap.get(mSchedule.getEventSetId());
        EventSetR current = mEventSetsMap.get(mSchedule.getEventSetId());
//        Log.d(TAG, "mschedule --->" + mSchedule.getEventSetId());
//        Log.d(TAG, "current ->" + current.getName() + "--" + current.getSeq());

//        Log.d(TAG, "current -> " +current.getName());
        EventSetR titleEvent = realm.where(EventSetR.class).equalTo("seq", mSchedule.getEventSetId()).findFirst();

        if (current != null) {
            if (current.getName().equals("미정")){
                tvEventSet.setText("미정");
            } else {
                tvEventSet.setText(titleEvent.getName());
            }
        } else if (current == null) {
            tvEventSet.setText("공휴일");
        }
    }

    //이벤트 설정 레이아웃클릭
    private void showSelectEventSetDialog() {
        if (mSelectEventSetDialog == null) {
            Log.d(TAG, "mSchedule eventset dialog -->" + mSchedule.getEventSetId());
            mSelectEventSetDialog = new SelectEventSetDialog(this, this, mSchedule.getEventSetId());
        }
        mSelectEventSetDialog.show();
    }


    //시간 설정 레이아웃클릭
    private void showSelectDateDialog() {
        if (mSelectDateDialog == null) {
            Log.d(TAG, "mSchedule getMonth state ->" + mSchedule.getMonth());
            mSelectDateDialog = new SelectDateDialog(this, this, mSchedule.getYear(), mSchedule.getMonth() -1, mSchedule.getDay(), mPosition);
        }
        mSelectDateDialog.show();
    }

    //위치 설정 레이아웃 클릭
    private void showInputLocationDialog() {
        if (mInputLocationDialog == null) {
            mInputLocationDialog = new InputLocationDialog(this, this);
        }
        mInputLocationDialog.show();
    }

    private void setScheduleData() {
        Log.d(TAG, "set eventid ==>"+ mSchedule.getEventSetId());
        eventSetId = mSchedule.getEventSetId();
        eventColor =mSchedule.getColor();

        vSchedule.setBackgroundResource(CalUtils.getEventSetColor(eventColor));//색상 설정
        ivEventIcon.setImageResource(eventSetId == 0 ? R.mipmap.ic_detail_category : R.mipmap.ic_detail_icon); //설정한 이벤트셋이 있다면.
        etTitle.setText(mSchedule.getTitle());
        etDesc.setText(mSchedule.getDesc()); //자세한 내용

        EventSetR current = mEventSetsMap.get(eventSetId);

        Log.d(TAG, "SetSchedule Data -->"+ current);
        if (current != null) {
            tvEventSet.setText(current.getName()); //스케줄 이름
        }
        resetDateTimeUi();

//        location = mSchedule.getLocation();

        Log.d(TAG, "location ->" + location);
        if (TextUtils.isEmpty(mSchedule.getLocation())) {
            Log.d(TAG, "mSchedule location - >" + mSchedule.getLocation());
            tvLocation.setText(R.string.click_here_select_location);
        } else {
            Log.d(TAG, "mschedule location 2- >" + mSchedule.getLocation());
            location = mSchedule.getLocation();
            tvLocation.setText(location);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SelectEventSetDialog.ADD_EVENT_SET_CODE) { //event set dialog에서 추가버튼을 누르고.
            if (resultCode == AddEventSetActivity.ADD_EVENT_SET_FINISH) {
//                EventSetR eventSet = (EventSetR) data.getSerializableExtra(AddEventSetActivity.EVENT_SET_OBJ); //작업끝
                final int eventSetId = (int) data.getSerializableExtra(AddEventSetActivity.EVENT_SET_OBJ);
                Log.d(TAG, "Schedule Detail activtyResult --->" + eventSetId);

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
//                        Log.d(TAG, "occur execute");
//                        long seq = realm.where(EventSetR.class).max("seq").longValue();

                        EventSetR eventSetR = realm.where(EventSetR.class).equalTo("seq", eventSetId).findFirst();
                        Log.d(TAG, "eventSetR detail -> " +eventSetR.getName());

                        if (eventSetR != null) {
                            mSelectEventSetDialog.addEventSet(eventSetR);
                        }
//                        sendBroadcast(new Intent(MainActivity.ADD_EVENT_SET_ACTION).putExtra(AddEventSetActivity.EVENT_SET_OBJ, eventSetR));
                        sendBroadcast(new Intent(MainActivity.ADD_EVENT_SET_ACTION).putExtra(AddEventSetActivity.EVENT_SET_OBJ, eventSetR.getSeq()));

                    }
                });
//                if (eventSet != null) {
//                    mSelectEventSetDialog.addEventSet(eventSet);
                    /**
                     * 스케줄 분류 항목추가.
                     * 스케줄 분류 다이얼로그에서 항목을 추가했을 경우,
                     * Broadcast로 좌측메뉴에도 그 항목을 추가한다고 전송.
                     */
                }
            }
        }


    private void resetDateTimeUi() {
        resYear = mSchedule.getYear();
        resMonth = mSchedule.getMonth();
        resDay = mSchedule.getDay();

        resTime = mSchedule.getTime();
        resultTime = mSchedule.gethTime();
        if (mSchedule.getTime() == 0) {
            if (mSchedule.getYear() != 0) {
//                Log.d(TAG, "ResetDateTimeUI -->" + mSchedule.getYear() + "/" + mSchedule.getMonth() + "/" + mSchedule.getDay());



                Log.d(TAG, "ResetDateTimeUI -->" + resYear + "/" + resMonth + "/" + resDay + "/" + resTime + "/" + resultTime);


//                tvTime.setText(String.format(getString(R.string.date_format_no_time), mSchedule.getYear(), mSchedule.getMonth() , mSchedule.getDay()));
                tvTime.setText(String.format(getString(R.string.date_format_no_time), resYear, resMonth, resDay));
            } else {
                tvTime.setText(R.string.click_here_select_date);
            }
        } else {
            Log.d(TAG, "ResetDateTimeUI -->" + resYear + "/" + resMonth + "/" + resDay + "/" + resTime + "/" + resultTime);

            tvTime.setText(DateUtils.timeStamp2Date(resTime, getString(R.string.date_format)));
        }
    }

    //위치 설정 다이얼로그 완료 버튼클릭
    @Override
    public void onLocationBack(final String text) {

      /*  realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                mSchedule.setLocation(text);

                if (TextUtils.isEmpty(mSchedule.getLocation())) {
                    tvLocation.setText(R.string.click_here_select_location);
                } else {
                    tvLocation.setText(mSchedule.getLocation());
                }
            }
        });*/

      Log.d(TAG, "onLocationBack -> " +text);
      Log.d(TAG, "mschedule location -> " + mSchedule.getLocation());
      location = text;

        if (TextUtils.isEmpty(location)) {
            tvLocation.setText(R.string.click_here_select_location);
        } else {
            tvLocation.setText(location);
        }

    }
    //스케줄 목록다이얼로그 클릭
   @Override
    public void onSelectEventSet(final EventSetR eventSet) {
        Log.d(TAG, "eventSet onSelectEventSet -->" + eventSet.getName());


        eventColor = eventSet.getColor();
        eventSetId = eventSet.getSeq();


        Log.d(TAG, "eventColor -> " +eventSet.getColor());
        Log.d(TAG, "eventSetId -> " +eventSet.getSeq());

        vSchedule.setBackgroundResource(CalUtils.getEventSetColor(eventColor));
        tvEventSet.setText(eventSet.getName());
        ivEventIcon.setImageResource(eventSetId == 0 ? R.mipmap.ic_detail_category : R.mipmap.ic_detail_icon);
    }

    //디테일 한 날짜/시간 설정 완료 클릭
    @Override
    public void onSelectDate(final int year, final int month, final int day, final long time, final int position) {
        Log.d(TAG, "onSelectDate");
//        realm.executeTransaction(new Realm.Transaction() {
//            @Override
//            public void execute(Realm realm) {
//                mSchedule.setYear(year);
//                mSchedule.setMonth(month);
//                mSchedule.setDay(day);
//                mSchedule.setTime(time);
//
//                SimpleDateFormat sdf = new SimpleDateFormat(HUMAN_TIME_FORMAT);
//                resultTime = sdf.format(time);
//
//                mSchedule.sethTime(resultTime);
//                mPosition = position;


        resYear = year;
        resMonth = month;
        resDay = day;
        resTime = time;

        SimpleDateFormat sdf = new SimpleDateFormat(HUMAN_TIME_FORMAT);
        resultTime = sdf.format(time);

        mPosition = position;

        //시간설정을 안하고 연도가 0이 아니면.
        if (resTime == 0) {
            if (resYear != 0) {
                tvTime.setText(String.format(getString(R.string.date_format_no_time), resYear, resMonth, resDay));
            } else {
                tvTime.setText(R.string.click_here_select_date);
            }
        } else {
            //시간설정을 했다면.
            tvTime.setText(DateUtils.timeStamp2Date(resTime, getString(R.string.date_format)));
        }
    }
   /*     if (mSchedule.getTime() == 0) {
            if (mSchedule.getYear() != 0) {
                tvTime.setText(String.format(getString(R.string.date_format_no_time), mSchedule.getYear(), mSchedule.getMonth() , mSchedule.getDay()));
            } else {
                tvTime.setText(R.string.click_here_select_date);
            }
        } else {
            tvTime.setText(DateUtils.timeStamp2Date(mSchedule.getTime(), getString(R.string.date_format)));
        }
                resetDateTimeUi();
            }*/
//        });

//    }



}
