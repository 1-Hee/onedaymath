package co.kr.onedaymath

import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import co.kr.onedaymath.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var b:ActivityMainBinding;

    companion object {
        private const val UNIT_TIME:Long = 50; // 회전작업을 수행할 단위 시간, 50ms 로 고정!
        private const val TIME_LIMIT:Long = 20; // 1초, 초당 20
    }
    var accFactor = 0f; // 가속도 변수
    var deg1 = 0f; // 프레임 각도 값
    var deg2 = 0f; // 버튼 각도 값
    var timeFactor:Long = 0;  // 시간을 재기 위한 변수 (1초간 누르면 화면 이동 기능 용)
    var spinPlayer:MediaPlayer? = null; // 메인 프레임 회전 시 재생할 브금을 실행하기 위한 플레이어
    var movePlayer:MediaPlayer? = null; // 화면 전환 시 재생할 브금을 실행하기 위한 플레이어

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        b = ActivityMainBinding.inflate(layoutInflater);
        setContentView(b.root);

        // 버튼들
        val plusBtn = findViewById<Button>(R.id.main_plus_btn); // 더하기 버튼
        val minusBtn = findViewById<Button>(R.id.main_minus_btn); // 빼기 버튼
        val multBtn = findViewById<Button>(R.id.main_multiply_btn); // 곱하기 버튼
        val divBtn = findViewById<Button>(R.id.main_divide_btn); // 나누기 버튼
        val mainBtnFrame:FrameLayout = findViewById<FrameLayout>(R.id.main_btn_frame); // 회전의 중심이 되는 프레임
        val playTimeIndicatorText = findViewById<TextView>(R.id.play_time_indicator_text); // 플레이 타임을 표시할 텍스트 뷰

        // SQLite를 사용하여 DB로부터 저장된 행의 개수를 셈해 플레이 타임을 가져옵니다. (행의 개수 = 플레이 타임!)
        val playTime = getPlayTime(); // 내부에 DBHelper 클래스 생성하고 닫고 다함.
        playTimeIndicatorText.text = "${playTime} 회\n플레이";

        // 버튼 회전 시 재생할 브금용 MediaPlayer 생성
        spinPlayer = MediaPlayer.create(this, R.raw.spin_btn_bgm);
        spinPlayer?.setVolume(.8f, .8f);
        spinPlayer?.isLooping = true;

        // 화면 이동 시 재생할 브금용 MediaPlayer 생성
        movePlayer = MediaPlayer.create(this, R.raw.round_move_bgm);
        movePlayer?.setVolume(.5f, .5f);

        // 더하기 라운드 버튼 리스너
        plusBtn.setOnClickListener {
            PageIndexComp.pageIdx = 0;
            moveRoundActivity(); // 메인 라운드 이동
        }

        // 빼기 라운드 버튼 리스너
        minusBtn.setOnClickListener {
            PageIndexComp.pageIdx = 1;
            moveRoundActivity(); // 메인 라운드 이동
        }

        // 곱하기 라운드 버튼 리스너
        multBtn.setOnClickListener {
            PageIndexComp.pageIdx = 2;
            moveRoundActivity(); // 메인 라운드 이동
        }

        // 나누기 라운드 버튼 리스너
        divBtn.setOnClickListener {
            PageIndexComp.pageIdx = 3;
            moveRoundActivity(); // 메인 라운드 이동
        }

        // 피젯 스피너 관련 리스너들
        deg1 = 0f;
        deg2 = 0f;

        // 프레임을 오래 누르면 돌게하는 함수
        var timer:Timer? = null;
        mainBtnFrame.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        timer = Timer();
                        timerStart(timer!!, mainBtnFrame, plusBtn, minusBtn, multBtn, divBtn);
                        if((spinPlayer?.isPlaying)==false){
                            spinPlayer?.start();
                        }
                    }
                    MotionEvent.ACTION_UP ->{
                        if(timer != null){
                            if(timeFactor >= TIME_LIMIT){
                                PageIndexComp.pageIdx = 4;
                                moveRoundActivity();

                            }
                            timer?.cancel();
                            accFactor = 0f;
                            timeFactor = 0;
                        }
                        spinPlayer?.pause();
                    }
                }
                //리턴값이 false면 동작 안함
                return true //or false
            }
        })
    }

    // SQLite를 사용하여 플레이 횟수를 가져오기 위해 사용하는 함수
    fun getPlayTime():Int{
        // SQLite로 데이터 베이스의 값을 가져오는 코드 (select)
        // step 1. DBHelper 객체를 생성한다.
        val dbHelper = DBHelper(this);
        // step 2. 카운트를 위한 쿼리문 작성
        val sql2 = """
            select count(*) 
            from math_rec
        """.trimIndent();
        // step 2-1. 전체 행의 갯수를 가져오는 방법
        val db:SQLiteDatabase = dbHelper.writableDatabase;
        val numRows = DatabaseUtils.queryNumEntries(db, "math_rec").toInt();

        // 사용후에는 닫아 줍니다.
        dbHelper.writableDatabase.close();
        return numRows;
    }

    // 타이머 함수, 타이머는 cancel하면 재시작하면 오류나서
    // 타이머를 새롭게 정의해주어야함. 그래서 타이머를 생성하고 다시시작하는 함수 생성
    fun timerStart(mTimer:Timer, mainBtnFrame:FrameLayout, plusBtn:Button, minusBtn:Button, multBtn:Button, divBtn:Button) {
        // 타이머
        // 반복적으로 사용할 TimerTask
        val mTimerTask = object : TimerTask() {
            override fun run() {
                val mHandler = Handler(Looper.getMainLooper())
                mHandler.postDelayed({
                    // 반복실행할 구문
                    accFactor += 0.001f;
                    timeFactor += 1;
                    deg1 += (1 + accFactor);
                    deg2 -= (1 + accFactor);
                     rotateMainButtons(mainBtnFrame, plusBtn, minusBtn, multBtn, divBtn);
                }, 0);
            }
        }
        mTimer.schedule(mTimerTask, 0, UNIT_TIME);
    }

    // 액티비티 이동 함수
    fun  moveRoundActivity(){
        val intent = Intent(this, RoundActivity::class.java);
        // 액티비티 이동 시 값을 초기화
        accFactor = 0f;
        deg1 = 0f;
        deg2 = 0f;
        timeFactor = 0;
        movePlayer?.start();
        startActivity(intent);
        finishAffinity();
    }

    // 피젯을 돌리는 함수
    fun  rotateMainButtons(mainBtnFrame:FrameLayout, plusBtn:Button, minusBtn:Button, multBtn:Button, divBtn:Button){
        deg1 = (deg1+45f);
        deg2 = (deg2-45f);
        mainBtnFrame.animate().rotation(deg1).start();
        plusBtn.animate().rotation(deg2).start();
        minusBtn.animate().rotation(deg2).start();
        multBtn.animate().rotation(deg2).start();
        divBtn.animate().rotation(deg2).start();
    }

    override fun onDestroy() {
        super.onDestroy()
        PageIndexComp.realeaseAllPlayer(
            arrayOf<MediaPlayer?>(
                movePlayer, spinPlayer
            )
        );
    }

}


