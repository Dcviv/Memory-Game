package com.vivekapp.memeorygame.models

enum class BoardSize( val numOfCards:Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24)
    ;
    companion object{
        fun getByValue(value:Int)= values().first{it.numOfCards==value}
    }
     fun  getWidth():Int{
        return when(this){
            EASY -> 2
            MEDIUM ->3
            HARD -> 4
        }
    }
    fun getHeight():Int{
        return numOfCards/getWidth()
    }
    fun getNoOfPairs():Int{
        return numOfCards/2
    }
}