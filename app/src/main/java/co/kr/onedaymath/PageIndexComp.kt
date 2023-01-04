package co.kr.onedaymath

import android.media.MediaPlayer

// 범 액티비티간 사용할 여러 값들을 저장할 Companion 객체
class PageIndexComp {
    companion object{
        // 하뤼분 종목 결정 인덱스
        var pageIdx = 0;
        var scordBoard:MutableList<Point> = mutableListOf<Point>();
        var playLevel = 1; // 사용자는 하나의 레벨 문제만 풀 수 있음!
        var isMuted = false; // 음소거 되었는지 저장하는 Companion 객체,
        // 다른 액티비티에서 초기화 되는 과정이 없어 프로그램 종료 후에도 설정이 유지됨
        var playIndex = 0;

        // 유저의 정보를 담을 companion 객체
        class Point{
            var problem:String = "";
            var second:Float = 0f;
            var level:Int = 0;

            constructor(){}
            constructor(problem:String, second:Float, level:Int){
                this.problem = problem;
                this.second = second;
                this.level = level;
            }
        }

        // 재생중인 플레이어 자원 해제
        fun realeaseAllPlayer(players:Array<MediaPlayer?>){
            Thread.sleep(500);
            for(player in players){
                player?.stop();
                player?.release();
            }
        }
    }

}
