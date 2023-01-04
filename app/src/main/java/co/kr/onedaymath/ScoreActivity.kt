package co.kr.onedaymath

import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import co.kr.onedaymath.databinding.ActivityScoreBinding


class ScoreActivity : AppCompatActivity() {

    lateinit var b: ActivityScoreBinding;
    var moveHomeMediaPlayer:MediaPlayer? = null; // 홈 화면 이동 시 재생할 브금 플레이어;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)
        b = ActivityScoreBinding.inflate(layoutInflater);
        setContentView(b.root);

        // view 선언
        val scoreBoardLinearLayout = findViewById<LinearLayout>(R.id.score_board_linear); // 스코어 보드
        val goMainBtn = findViewById<ImageButton>(R.id.score_go_main_btn); // 홈버튼
        val scoreTotalTimeTextView = findViewById<TextView>(R.id.score_total_time_text); // 전체 시간 텍스트
        val scoreAvgTimeTextView = findViewById<TextView>(R.id.score_avg_time_text); // 평균 시간 텍스트
        val scorePastTimeAvgTextView = findViewById<TextView>(R.id.score_past_time_avg); // 직전 평균 기록
        val scorePastTimeTotalTextView = findViewById<TextView>(R.id.score_past_time_total); // 직전 전체 기록
        val scoreAllTimeAvgTextView = findViewById<TextView>(R.id.score_all_time_avg); // 레벨 별 전체 평균 기록
        val scoreAllTimeTotalTextView = findViewById<TextView>(R.id.score_all_time_total); // 레벨 별 전체 총합 기록
        val scoreLevelTextView = findViewById<TextView>(R.id.score_level_text); // 현재 레벨을 표시할 텍스트 뷰

        // 브금 플레이어 세팅
        moveHomeMediaPlayer = MediaPlayer.create(this, R.raw.home_move_bgm);
        moveHomeMediaPlayer?.setVolume(.3f, .3f);

        // 데이터 불러오기!
        getStaticData(scoreAllTimeAvgTextView, scoreAllTimeTotalTextView); // 평균 데이터
        getPastData(scorePastTimeAvgTextView, scorePastTimeTotalTextView); // 직전데이터

        // 액티비티가 생성되면 자동으로 점수판 UI를 구성해줌.
        createScordTextViews(scoreBoardLinearLayout, scoreTotalTimeTextView, scoreAvgTimeTextView);

        // 레벨 설정
        scoreLevelTextView.text = "${PageIndexComp.playLevel}";

        // 홈버튼 누르면 시작 화면으로 돌아가는 함수
        goMainBtn.setOnClickListener {
            moveMainActivity();
            moveHomeMediaPlayer?.start();
            PageIndexComp.pageIdx = 0; // 스코어보드에서 화면으로 돌아갈 때에만 값을 초기화!
        }

        // selectAllDBData();
    }

    // db 출력
    fun selectAllDBData(){
        // avg_time, total_time, level, round_idx
        val dbHelper = DBHelper(this);
        val query = """
            select avg_time, total_time, level, round_idx 
            from math_rec order by math_idx desc;
        """.trimIndent();

        val tableData: Cursor = dbHelper.writableDatabase.rawQuery(query, null);

        while(tableData.moveToNext()){
            val avI = tableData.getColumnIndex("avg_time"); // 가져올 컬럼명을 인자로 준다.
            val ttI = tableData.getColumnIndex("total_time");
            val lvI = tableData.getColumnIndex("level");
            val riI = tableData.getColumnIndex("round_idx")

            // getString 데이터를 통해 DB 속 값을 가져온다
            val av = tableData.getFloat(avI);
            val tt = tableData.getFloat(ttI);
            val lv = tableData.getString(lvI);
            val ri =  tableData.getString(riI);
       }
        // 사용후에는 자원을 닫는다.
        dbHelper.writableDatabase.close();
    }

    fun getSign(past:Float, current:Float):String{
        if(past > current) return "▲";
        else if(past < current) return "▼"
        else return "-"
    }

    // 유저의 플레이 타임 기록을 저장하는 메서드!
    // scoreBoard 를 통해서 저장한다.
    fun saveScore(avgTime:Float, totalTime:Float, level:Int){
        // SQLite로 데이터를 삽입하는 코드
        // step 1. 데이터 베이스를 연다
        val dbHelper = DBHelper(this);
        // step 2. 삽입 코드를 작성한다
        val sql = """
            insert into math_rec
            (avg_time, total_time, level, round_idx)
            values
            (?, ?, ?, ?)
        """.trimIndent()
        // step 3. 세팅될 값을 배열로 선언해준다
        val values = arrayOf(avgTime, totalTime, level, PageIndexComp.pageIdx);
        // step 4. DBHelper를 통해 쿼리문을 실행한다.
        dbHelper.writableDatabase.execSQL(sql, values); // sql문, 값(배열) 순이다.
        // step 5. DB 사용이 끝났다면 쿼리문을 닫아 준다.
        dbHelper.writableDatabase.close();
    }

    // 유저의 과거 기록을 통해 간단한 통계정보 제공하는 메서드
    // 두 가지 정보를 제공
    // 전체 평균보다 빨라졌는가?
    fun getStaticData(allAvgTextView: TextView, allTotalTextView: TextView){
        // SQLite로 데이터 베이스의 값을 가져오는 코드 (select)
        // step 1. DBHelper 객체를 생성한다.
        val dbHelper = DBHelper(this);

        // step 2. sql 쿼리문을 작성한다.
        val query = """
            select avg(avg_time) as all_avg, avg(total_time) as all_total 
            from math_rec where level = ? and round_idx = ?;
        """.trimIndent();

        // step 3. sql문을 실행하고 결과 객체를 받아온다
        // 쿼리문에 추가적인 조건 등을 넣지 않았기 때문에, null을 준다. 조건이 있다면 배열로 주면 됨.
        // 이 데이터의 자료형은 Cursor이다.
        val args = arrayOf(PageIndexComp.playLevel.toString(), PageIndexComp.pageIdx.toString()); // 인자는 String이어야 함.
        val tableData: Cursor = dbHelper.writableDatabase.rawQuery(query, args);

        // step 4. 데이터 꺼내오기
        // 데이터는 반복문을 통해 값을 꺼내와야 한다.
        // 가져올 데이터는 tableData에 있는데 이러한 값 하나하나를 참조하는데 쓸 key 값은 int 이며 index라고 부른다.
        // 따라서 네이밍 규칙에 Index를 붙여주어서 구분해보았다.
        // SQLite로 부터 얻어낸 Cursor 객체로부터 바로 값을 가져오려하면
        // 커서의 위치가 가장 첫 데이터를 향하고 있지 않기 때문에
        // 반복문을 통해서 커서를 이동시키고 그 이후에 값을 가져오는 식으로 진행해야 한다.
        // 따라서 값이 한개 던지 여러 개던지 while문 등을 사용하여 값을 가져와야 한다!

        while(tableData.moveToNext()){
            val allAvgIndex:Int = tableData.getColumnIndex("all_avg"); // 가져올 컬럼명을 인자로 준다.
            val allTotalIndex:Int = tableData.getColumnIndex("all_total");

            // getString 데이터를 통해 DB 속 값을 가져온다
            val allAvgTime = tableData.getFloat(allAvgIndex);
            val allTotalTime = tableData.getFloat(allTotalIndex);

            // step 5. 실제 값을 사용하고 싶은 곳에 사용!
            // null 체크!
            if (allAvgTime == null || allAvgTime == 0f) {
                allAvgTextView.text = "기록없음";
            }else {
                val avg = String.format("%.2f", allAvgTime);
                allAvgTextView.text = "$avg 초"
            }

            if (allTotalTime == null || allAvgTime == 0f) {
                allTotalTextView.text = "기록없음";
            }else {
                val total = String.format("%.1f", allTotalTime);
                allTotalTextView.text = "$total 초"
            }

        }
        // 사용후에는 자원을 닫는다.
        dbHelper.writableDatabase.close();
    }

    // 직전보다 빨라졌나요?
    fun getPastData(pastAvgTextView: TextView, pastTotalTextView: TextView){
        // step 1. DBHelper 객체를 생성한다.
        val dbHelper = DBHelper(this);

        // step 2. sql문을 작성한다.
        val query = """
            select avg_time, total_time
            from math_rec where level = ? and round_idx = ?;
        """.trimIndent();

        // step 3. Cursor 객체를 가져온다.
        val args = arrayOf(PageIndexComp.playLevel.toString(), PageIndexComp.pageIdx.toString()); // 인자는 String이어야 함.
        val pastData: Cursor = dbHelper.writableDatabase.rawQuery(query, args);

        var isEmpty = true;
        // step 4. 데이터를 추출한다.
        while(pastData.moveToNext()){
            val avgTimeIndex:Int = pastData.getColumnIndex("avg_time"); // 가져올 컬럼명을 인자로 준다.
            val totalTimeIndex:Int = pastData.getColumnIndex("total_time");
            isEmpty = false;

            // getString 데이터를 통해 DB 속 값을 가져온다
            val prevAvgTime = pastData.getFloat(avgTimeIndex);
            val prevTotalTime = pastData.getFloat(totalTimeIndex);

            // step 5. 실제 값을 사용하고 싶은 곳에 사용!
            val prevAvg = String.format("%.2f", prevAvgTime);
            val prevTotal = String.format("%.1f", prevTotalTime);
            pastAvgTextView.text = "$prevAvg 초";
            pastTotalTextView.text = "$prevTotal 초";
        }

        // 기록이 없을 경우의 값 세팅
        if(isEmpty){
            pastAvgTextView.text = "기록없음";
            pastTotalTextView.text = "기록없음";
        }

        // 사용후에는 자원을 닫는다.
        dbHelper.writableDatabase.close();
    }

    // 점수판 동적 구성 함수
    // 컴페니언 객체인 PageIndexComp.scoreBoard 클래스로부터 점수 기록을 불러와
    // 동적으로 UI를 구성하는 함수
    // 개별 기록 , 총점, 평균을 모두 계산하여 UI로 만들어 준다.
    fun createScordTextViews(scoreBoardLinearLayout:LinearLayout, totalView:TextView, avgView:TextView){
        val linearParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        val problemParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        );

        val secondParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        val levelParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        var userSum:Float = 0f;

        for(i in PageIndexComp.scordBoard){
            val scoreUnit = LinearLayout(this);
            scoreUnit.orientation = LinearLayout.HORIZONTAL;
            linearParams.gravity = Gravity.CENTER;
            scoreUnit.layoutParams = linearParams;

            val problemText = TextView(this);
            problemText.setTextColor(Color.WHITE);
            problemText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER;
            problemText.text ="${i.problem}";
            // problemText에대해 레이아웃을 동적으로 적용한다.
            val sizes:Array<Number> = getStringSizes(i.problem);
            // 첫번째는 폰트 사이즈, 두번째는 레이아웃 오른쪽 마진
            problemText.textSize = sizes[0].toFloat();
            problemParams.setMargins(0, 0,sizes[1].toInt(), 0);

            val type = ResourcesCompat.getFont(this, R.font.puradak_gentle_gothic);
            problemText.setTypeface(type);

            val secondText = TextView(this);
            secondText.textSize = 20f;
            secondText.setTextColor(Color.WHITE);
            secondText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER;
            secondText.text ="${i.second} 초";
            secondParams.setMargins(0, 0, 50, 0)

            secondText.setTypeface(type);

            userSum += i.second;

            val levelText = TextView(this);
            levelText.textSize = 20f;
            levelText.setTextColor(Color.WHITE);
            levelText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER;
            levelText.text = "Lv ${i.level}";
            levelParams.setMargins(50, 0, 0, 0);

            levelText.setTypeface(type);

            scoreUnit.addView(problemText, problemParams);
            scoreUnit.addView(secondText, secondParams);
            scoreUnit.addView(levelText, levelParams);

            scoreBoardLinearLayout.addView(scoreUnit)
        }

        val totalTime = String.format("%.1f", userSum).toFloat();
        val avgTime = String.format("%.2f", userSum/PageIndexComp.scordBoard.size).toFloat();
        totalView.text = "$totalTime 초"
        avgView.text = "$avgTime 초"

        // 전체 점수 결과를 저장!
        saveScore(avgTime, totalTime, PageIndexComp.playLevel);
        PageIndexComp.scordBoard.clear(); // 점수 구성 후에는 데이터 날림;
    }

    // 문자열 길이에 따라서 layout 속성을 정하는 함수
    fun getStringSizes(str:String):Array<Number>{
        // 글자크기, margin-right
        if(str.length >= 15) return arrayOf(14f, 50);
        else if(str.length >= 13) return arrayOf(16f, 65);
        else if(str.length >= 11) return arrayOf(18f, 80);
        else if(str.length >= 9) return arrayOf(20f, 95);
        else if(str.length >= 7) return arrayOf(20f, 120);
        else return arrayOf(20f, 145);
    }

    // 메인 액티비티로 이동하는 함수
    fun moveMainActivity() {
        val intent = Intent(this, MainActivity::class.java);
        startActivity(intent);
        finishAffinity();
        PageIndexComp.playLevel = 1; // 홈으로 이동하면 레벨은 초기화!
    }

    // 메모상에서 제거시 스코어 보드 리셋
    // 홈으로 이동하거나, 뒤로가기 시에만 리셋하도록
    // 가운데 버튼을 눌러서 Hold 하면 값이 유지되도록 함
    override fun onDestroy() {
        super.onDestroy();
        PageIndexComp.scordBoard.clear();
        // 플레이어 자원을 해제
        PageIndexComp.realeaseAllPlayer(
            arrayOf<MediaPlayer?>(
                moveHomeMediaPlayer
            )
        );
    }
}