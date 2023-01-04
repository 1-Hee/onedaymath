package co.kr.onedaymath

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// 기록들을 저장하고 불러오기 위한 DB 클래스
class DBHelper:SQLiteOpenHelper {

    // 생성자 ( 데이터 베이스의 이름을 정해줌. )
    constructor(context:Context) :super(context, "oneDayMathRecord.db", null, 1);

    // 데이터 베이스 파일이 생성될 때 자동 호출되는 메서드
    // 테이블 생성 코드를  작성한다
    // 데이터 베이스 파일이 생성될 때 호출
    override fun onCreate(p0: SQLiteDatabase?) {
        // 생성 쿼리문 작성
        // 메모사항
        // db의 level 열에는 1~6 까지의 값을 가지며 레벨 순으로 값을 비교해야해서 레벨 컬럼을 추가함
        val sql = """
            create table math_rec(
            	math_idx integer primary key autoincrement,
                avg_time float not null,
                total_time float not null,
                level int not null,
                round_idx int not null,
                rec_time TIMESTAMP not null DEFAULT CURRENT_TIMESTAMP 
            );
        """.trimIndent()
        p0?.execSQL(sql);
    }

    // 데이터 베이스가 업데이트?? 될때 호출
    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }


}