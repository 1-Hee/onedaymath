package co.kr.onedaymath

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import co.kr.onedaymath.databinding.ActivityRoundBinding
import java.util.*


class RoundActivity : AppCompatActivity() {

    // 음원 소스 저장용 컴페니언 객체
    companion object{
        val bgmList = arrayOf(
            R.raw.girasol_quincas_moreira,
            R.raw.sand_castles_the_green_orbs,
            R.raw.splashing_around_the_green_orbs,
            R.raw.sugar_zone_silent_partner,
            R.raw.walking_the_dog_silent_partner,
            R.raw.why_did_you_do_it_everet_almond
        );
    }

    lateinit var b: ActivityRoundBinding;
    var currentTime:Long = 0; // 실시간으로 현재 문제의 시간을 재는 변수
    var totalTime = 0f; // 누적 시간을 계산하기 위해 변수를 하나 생성했는데 안쓸 수도 있음...
    var correctNumber = 0; // 맞힌 개수
    private val PROBLEM_SIZE = 15; // 출제 문제 수
    // 각 문제별 자릿수 범위 FACTOR
    var plusRangeFactor  = 1;
    var minusRangeFactor  = 1;
    var multRangeFactor  = 1;
    var divRangeFactor  = 1;
    var randRangeFactor = 1;
    var level = 1;
    // answer
    var answer:Long = -1;
    // timer
    var timer:Timer? = null;

    // Player
    var moveMainPlayer:MediaPlayer? = null; // 메인 액티비티 이동 시 재생할 브금 플레이어
    var moveScorePlayer:MediaPlayer? = null; // 점수판 이동 시 재생할 브금 플레이어
    var correctPlayer:MediaPlayer? = null; // 정답 시 재생할 브금 플레이어
    var wrongPlayer:MediaPlayer? = null; // 오답시 재생할 브금 플레이어
    var bgmPlayer:MediaPlayer? = null; // 문제풀 때 재생할 미디어 플레이어

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_round)
        b = ActivityRoundBinding.inflate(layoutInflater);
        setContentView(b.root);

        // 모든 View 변수 선언
        val problemCategoryImgView = findViewById<ImageView>(R.id.problem_category_img); // 더하기, 빼기...
        val lvTextView = findViewById<TextView>(R.id.problem_level_text); // 레벨 TextView
        val problemCorrectText = findViewById<TextView>(R.id.problem_correct_text); // 맞힌 개수 TextView
        val problemRecTimeText = findViewById<TextView>(R.id.problem_rec_time_text);  // 타이머가 표시될 텍스트
        val boardImgBtn = findViewById<ImageButton>(R.id.problem_board_img_btn); // 문제 출제 영역(칠판)은 이미지 버튼
        val goMainBtn = findViewById<ImageButton>(R.id.round_go_main_btn); // 메인 엑티비티 이동 버튼
        val seekBar = findViewById<SeekBar>(R.id.problem_level_seekbar); // 레벨 설정을 위한 SeekBar
        val problemText = findViewById<TextView>(R.id.problem_text); // 문제 출제 영역 텍스트
        val editText = findViewById<EditText>(R.id.answer_edit_text); // 문제 정답을 적는 editText;
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager; // 모바일 키패드 올릴때 사용할 manager 클래스
        val musicControlBtn = findViewById<ImageButton>(R.id.music_control_btn); // 배경 음악을 컨트롤할 브금 플레이어

        // 화면 이동용 플레이어 세팅
        moveMainPlayer = MediaPlayer.create(this, R.raw.home_move_bgm);
        moveMainPlayer?.setVolume(.3f, .3f);
        moveScorePlayer = MediaPlayer.create(this, R.raw.score_move_bgm);
        moveScorePlayer?.setVolume(.2f, .2f);

        // 브금 플레이어 세팅
        if(!PageIndexComp.isMuted){
            bgmPlayer = MediaPlayer.create(this, bgmList[PageIndexComp.playIndex]);
            bgmPlayer?.start(); // 일단 실행!
            bgmPlayer?.isLooping = true;
            musicControlBtn.background = getDrawable(R.drawable.play_sound_ripple);
        }else {
            musicControlBtn.background = getDrawable(R.drawable.mute_sound_ripple);
        }

        // 브금 플레이어 버튼 리스너
        musicControlBtn.setOnClickListener {
            // 플레이어 정지
            if(!PageIndexComp.isMuted){
                bgmPlayer?.stop();
                bgmPlayer?.release();
                bgmPlayer = null;
                // 플레이어 재선언
                PageIndexComp.playIndex = (PageIndexComp.playIndex+1)% bgmList.size;
                bgmPlayer = MediaPlayer.create(this, bgmList[PageIndexComp.playIndex]);
                // 플레이
                bgmPlayer?.start();
            }
        }

        musicControlBtn.setOnLongClickListener {
            if(!PageIndexComp.isMuted){
                musicControlBtn.background = getDrawable(R.drawable.mute_sound_ripple);
                bgmPlayer?.stop();
                bgmPlayer?.release();
                bgmPlayer = null;
                PageIndexComp.isMuted = true;
            }else {
                musicControlBtn.background = getDrawable(R.drawable.play_sound_ripple);
                bgmPlayer = MediaPlayer.create(this, bgmList[PageIndexComp.playIndex]);
                bgmPlayer?.isLooping = true;
                bgmPlayer?.start();
                PageIndexComp.isMuted = false;
            }
            true;
        }

        // 라운드 타이틀 이미지 셋팅
        roundTitleInit(problemCategoryImgView);
        // 레벨 수준, 맞힌 개수 초기화
        initText(lvTextView, problemCorrectText, problemText);

        // 액티비티 옴과 동시에 타이머 시작
        timer = Timer();
        timerStart(timer, problemRecTimeText);

        // 칠판 이미지 버튼 누르면 문제 리셋
        boardImgBtn.setOnClickListener {
//             moveScoreActivity(); // 스코어 보드로 이동 ## 임시!!!
            judgeProblem(getFactor(PageIndexComp.pageIdx), problemText);
            currentTime = 0;
        }

        // 집모양 버튼 누르면 메인 엑티비티로
        goMainBtn.setOnClickListener {
            PageIndexComp.pageIdx = 0;
            bgmPlayer?.stop();
            bgmPlayer?.release();
            bgmPlayer = null;
            moveMainPlayer?.start();
            moveMainActivity(); // 홈으로 이동
        }

        // 레벨 설정 SeekBar의 step, range 세팅
        seekBar.progress = 0;
        seekBar.incrementProgressBy(1)
        seekBar.min = 1; // min 설정을 해줘야지 왼쪽으로 딱 붙음, (최소값 = 초기값)으로 할 것.
        seekBar.max = 6;

        // 난이도 조절을 위한 seekBar 이벤트 리스너
        // 시크바가 움직일 때 난이도가 조정되며, 난이도가 바뀌면 기존의 푼 기록을 날라갑니다.
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                PageIndexComp.playLevel = progress;
                lvTextView.text = "Lv $progress"
                currentTime = 0; // 레벨 조정하느라 움직일때마다 현재 시간은 초기화 해줌.
                // 푼 문제수 초기화
                correctNumber = 0;
                // 푼 데이터도 초기화
                PageIndexComp.scordBoard.clear();
                // 맞힌 개수를 다시 표시
                setAnswerText(problemCorrectText);
                judgeProblem(progress, problemText); // 세팅과 함께 문제도 변경된다.
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        });

        // 에딧 텍스트
        editText.setOnKeyListener { v, keyCode, event ->
            var isHandled = false;
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // 엔터 눌렀을때 행동
                isHandled = true;
                // Log.d("사용자 입력 ::: ", "${editText.text}")
                val result = checkAnswer(editText.text.toString(), editText, problemText);
                if(result) {// 정답이면?
                    judgeProblem(getFactor(PageIndexComp.pageIdx), problemText);
                    // 정답 효과음 플레이어 세팅
                    correctPlayer = MediaPlayer.create(this, R.raw.correct_sound2);
                    correctPlayer?.setVolume(.2f, .2f);
                    correctPlayer?.start();
                    setAnswerText(problemCorrectText) // 정답 카운트 세팅, 카운트 올리는건 checkAnswer에서 함.
                    if(correctNumber==PROBLEM_SIZE){
                        bgmPlayer?.stop();
                        bgmPlayer?.release();
                        bgmPlayer = null;
                        moveScoreActivity();
                        moveScorePlayer?.start()
                    }
                }else { // 오답이면
                    // 오답 효과음 플레이어 세팅
                    wrongPlayer = MediaPlayer.create(this, R.raw.wrong_sound);
                    wrongPlayer?.setVolume(2f, 2f);
                    wrongPlayer?.start(); // 재생!;
                }
            }
            // 리턴 값으로 true, false를 주는데 무조건 true를 주면 엔터 이후 프리징이 걸려버림
            // 그래서 변수 handle을 통해 가변적으로 값을 리턴...
            isHandled;
        }

        // editText가 포커스를 계속 유지하도록 하는 코드
        editText.setOnClickListener {
            editText.requestFocus()       // 포커스가 있어야 키보드가 노출된다.
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }

        // 초기 문제 출제 관련 코드
        setFactorAndLevel(PageIndexComp.pageIdx, seekBar, problemText);

    }

    // 뒤로가기 버튼 시
    override fun onDestroy() {
        super.onDestroy()
        if(bgmPlayer != null){
            bgmPlayer?.stop();
            bgmPlayer?.release();
            bgmPlayer = null;
        }
        PageIndexComp.realeaseAllPlayer(
            arrayOf<MediaPlayer?>(
                moveMainPlayer, moveScorePlayer, correctPlayer, wrongPlayer
            )
        );
    }

    // pause 눌렀을 시
    override fun onPause() {
        super.onPause();
        bgmPlayer?.pause();
    }

    override fun onStart() {
        super.onStart()
        bgmPlayer?.start();
    }

    // 함수들...
    // SQLite로부터 현재 설정한 레벨 정보를 조건으로
    // 직전 플레이 기록을 가져와 그 레벨로 자동 설정해준다
    fun getLevel(roundIdx:Int):Int{
        var dbHelper = DBHelper(this);

        var query = """
            select level, round_idx, rec_time
            from math_rec 
            where round_idx = ?            
            order by rec_time desc
            limit 1
        """.trimIndent()
        // rectime
        //            limit 1
        val args = arrayOf(roundIdx.toString());
        val dbData =  dbHelper.writableDatabase.rawQuery(query, args);

        var pastLevel:Int = 0;
        while(dbData.moveToNext()){
            val levelIndex = dbData.getColumnIndex("level");
            pastLevel = dbData.getInt(levelIndex);
        }
        dbHelper.writableDatabase.close();
        if(pastLevel==0) pastLevel = 1
        return pastLevel;
    }

    // 레벨을 세팅하는 함수
    fun setFactorAndLevel(pageIndex:Int, seekBar: SeekBar, textView: TextView){
        val pastLevel = getLevel(pageIndex);
        when(pageIndex){
            0->{
                plusRangeFactor = pastLevel;
                seekBar.progress = plusRangeFactor;
                judgeProblem(plusRangeFactor, textView);
            }
            1 ->{
                minusRangeFactor = pastLevel;
                seekBar.progress = minusRangeFactor;
                judgeProblem(minusRangeFactor, textView);
            }
            2 ->{
                multRangeFactor = pastLevel;
                seekBar.progress = multRangeFactor;
                judgeProblem(multRangeFactor, textView);
            }
            3 -> {
                divRangeFactor = pastLevel;
                seekBar.progress = divRangeFactor;
                judgeProblem(divRangeFactor, textView);
            }
            4 -> {
                randRangeFactor = pastLevel;
                seekBar.progress = randRangeFactor;
                judgeProblem(randRangeFactor, textView);
            }
            else -> {
                randRangeFactor = pastLevel;
                seekBar.progress = randRangeFactor;
                judgeProblem(randRangeFactor, textView);
            }
        }
    }


    // 무슨 종목의 문제를 낼지 결정하는 함수
    fun judgeProblem(level:Int, textView: TextView){
        when(PageIndexComp.pageIdx){
            0 ->{ // 더하기
                plusRangeFactor = level;
                plusProblemPresentation(textView, plusRangeFactor);
            }
            1 -> { // 빼기
                minusRangeFactor = level;
                minusProblemPresentation(textView, minusRangeFactor);
            }
            2 -> { // 곱하기
                multRangeFactor = level;
                multProblemPresentation(textView, multRangeFactor);
            }
            3 -> { // 나누기
                divRangeFactor = level;
                divProblemPresentation(textView, divRangeFactor);
            }
            4 -> {
                randRangeFactor = level;
                randProblemPresentation(textView, randRangeFactor);
            }
        }
    }

    // 정답을 맞췄으면, 정답 카운트로 칠판에 숫자 표시하는 함수
    fun setAnswerText(textView: TextView){
        textView.text = "$correctNumber/$PROBLEM_SIZE"
    }

    // 정답 검사하는 함수
    fun checkAnswer(input:String, editText: EditText, probText:TextView):Boolean{
        val ans = input;
        editText.setText("");
        if(!ans.isEmpty() && ans.toLong() == answer){
            // Log.d("채점결과 :: ", "정답입니다 ${input}");
            val point = PageIndexComp.Companion.Point(probText.text.toString(), currentTime/10f, getFactor(PageIndexComp.pageIdx));
            correctNumber++;
            currentTime = 0;
            // Log.d("채점 기록 :::", " RED = ${point.problem}, ${point.second}, ${point.level}");
            PageIndexComp.scordBoard.add(point);
            return true;
        }else {
            // Log.d("채점결과 :: ", "오답입니다 ${input}");
            return false;
        }
    }

    // page index를 기준으로 팩터(레벨 변수)를 리턴하는 함수
    fun getFactor(pageIndex:Int):Int{
        when(pageIndex){
            0->{ return plusRangeFactor }
            1 ->{ return  minusRangeFactor }
            2 ->{ return  multRangeFactor }
            3 -> { return  divRangeFactor }
            4 -> { return  randRangeFactor }
            else -> { return randRangeFactor }
        }
    }

    // 더하기 문제 내는 함수
    fun plusProblemPresentation(textView: TextView, factor: Int){
        val random = Random();
        val base = 10;
        val bound = Math.pow(base.toDouble(), factor.toDouble());
        val value1 = random.nextInt(bound.toInt());
        val value2 = random.nextInt(bound.toInt());
        textView.text = "$value1 + $value2"
        answer = (value1+value2).toLong();
    }

    // 빼기 문제 내는 함수
    // 빼기 문제는 -가 안나오도록 양수가 나오는 계산으로만 표시함
    fun minusProblemPresentation(textView: TextView, factor: Int){
        val random = Random();
        val base = 10;
        val bound = Math.pow(base.toDouble(), factor.toDouble());
        val value1 = random.nextInt(bound.toInt());
        val value2 = random.nextInt(bound.toInt());
        if(value2 > value1) {
            answer = (value2 - value1).toLong()
            textView.text = "$value2 - $value1"
        }else {
            answer = (value1 - value2).toLong()
            textView.text = "$value1 - $value2"
        }
    }

    // 곱하기 문제 내는 함수
    fun multProblemPresentation(textView: TextView, factor: Int){
        val random = Random();
        val base = 10;
        val bounds = getMultiplyBounds(factor);
        val range1 = Math.pow(base.toDouble(), bounds[0].toDouble());
        val range2 = Math.pow(base.toDouble(), bounds[1].toDouble());
        val value1 = random.nextInt(range1.toInt());
        val value2 = random.nextInt(range2.toInt());

        answer = (value1 * value2).toLong();
        textView.text = "$value1 × $value2"
    }

    // 곱하기의 난이도 설정을 위한 자릿수 밤위 설정하는 변수
    fun getMultiplyBounds(level:Int):Array<Int>{
        when(level){
            // 첫번째는 level bound, 두번째는 조정된 bound
            6 -> { return arrayOf(5, 3)}
            5 -> { return arrayOf(4, 2)}
            4 -> { return arrayOf(3, 2)}
            3 -> { return arrayOf(3, 1)}
            2 -> { return arrayOf(2, 1)}
            else ->{ return arrayOf(level, level) }
        }
    }

    // 나누기 문제 내는 함수
    // 나누기는 몫만 맞추는 것!
    fun divProblemPresentation(textView: TextView, factor:Int) {
        val random = Random();
        val base = 10;
        val bounds = getDivideBounds(factor);
        val range1 = Math.pow(base.toDouble(), bounds[0].toDouble());
        val range2 = Math.pow(base.toDouble(), bounds[1].toDouble());

        var value1 = random.nextInt(range1.toInt())+1;
        var value2 = random.nextInt(range2.toInt())+1;

        if(value1>value2){
            answer = (value1 / value2).toLong();
            textView.text = "$value1 ÷ $value2"
        }else {
            answer = (value2 / value1).toLong();
            textView.text = "$value2 ÷ $value1"
        }
    }

    // 나누기기의 난이도 설정을 위한 자릿수 밤위 설정하는 변수
    fun getDivideBounds(level:Int):Array<Int>{
        when(level){
            // 첫번째는 level bound, 두번째는 조정된 bound
            6 -> { return arrayOf(5, 3)}
            5 -> { return arrayOf(4, 2)}
            4 -> { return arrayOf(3, 2)}
            3 -> { return arrayOf(3, 1)}
            2 -> { return arrayOf(2, 1)}
            else ->{ return arrayOf(level, level)}
        }
    }

    // 랜덤 문제 내는 함수
    fun randProblemPresentation(textView: TextView, factor: Int) {
        val random = Random();
        val randRoundIndex = random.nextInt(4);
        // 랜덤 계수를 문제 내는 데에만 사용한다.
        when(randRoundIndex){
            0->{
                plusProblemPresentation(textView, factor);
            }
            1->{
                minusProblemPresentation(textView, factor);
            }
            2->{
                multProblemPresentation(textView, factor);
            }
            3->{
                divProblemPresentation(textView, factor);
            }
        }
    }

    // 타이머 함수, 타이머는 cancel하면 재시작하면 오류나서
    // 타이머를 새롭게 정의해주어야함. 그래서 타이머를 생성하고 다시시작하는 함수 생성
    fun timerStart(mTimer: Timer?, textView:TextView) {
        // 타이머
        // 반복적으로 사용할 TimerTask
        val mTimerTask = object : TimerTask() {
            override fun run() {
                val mHandler = Handler(Looper.getMainLooper())
                mHandler.postDelayed({
                    currentTime +=1
                    textView.setText("${currentTime/10f} 초");
                }, 0);
            }
        }
        mTimer?.schedule(mTimerTask, 1000, 100);
    }

    // ROUND TITLE INIT 함수
    // 라운드 액티비티의 종목명을 위해 이미지뷰를 세팅해주고,
    // 이미지뷰를 세팅할 때 쓴 값으로 문제를 출제한다.
    // 리턴 값으로 정수값 ( 이미지 뷰 인덱스 )을 주는 역할까지 함.
    fun roundTitleInit(imgView:ImageView){
        when(PageIndexComp.pageIdx){
            0 -> {
                imgView.setImageResource(R.drawable.problem_category_plus);
            }
            1 -> {
                imgView.setImageResource(R.drawable.problem_category_minus)
            }
            2 -> {
                imgView.setImageResource(R.drawable.problem_category_multiply);
            }
            3 -> {
                imgView.setImageResource(R.drawable.problem_category_divide);
            }
            4 -> {
                imgView.setImageResource(R.drawable.problem_category_random);
            }
        }
    }

    // 함수들...
    // 시작시 레벨, 맞힌 문제를 초기화하는 함수, (추가될 수 있음)
    fun initText(lvTextView:TextView, numberTextView:TextView, probText: TextView) {
        lvTextView.text = "Lv ${PageIndexComp.playLevel}";
        numberTextView.text = "$correctNumber/$PROBLEM_SIZE";
        probText.text = ""
    }

    // 변수를 초기화하는 함수
    fun initValues(){
        currentTime = 0;
        totalTime = 0f;
        correctNumber = 0;
        plusRangeFactor  = 1;
        minusRangeFactor  = 1;
        multRangeFactor  = 1;
        divRangeFactor  = 1;
        randRangeFactor = 1;
        answer = -1;
    }

    // 홈 이동 함수
    fun moveMainActivity() {
        val intent = Intent(this, MainActivity::class.java);
        initValues();
        timer?.cancel();
        startActivity(intent);
        finishAffinity();
        PageIndexComp.playLevel = 1; // 홈으로 이동하면 레벨을 초기화 해줌.
        PageIndexComp.scordBoard.clear();
    }

    // 결과창 이동 함수
    fun moveScoreActivity() {
        val intent = Intent(this, ScoreActivity::class.java);
        initValues();
        timer?.cancel();
        startActivity(intent);
        finishAffinity();
    }
}