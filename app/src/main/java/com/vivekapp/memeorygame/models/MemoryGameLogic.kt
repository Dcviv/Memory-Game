package com.vivekapp.memeorygame.models

import com.vivekapp.memeorygame.utils.DEFAULT_ICONS

class MemoryGameLogic(private val boardSize: BoardSize, private val customImages: List<String>?){

    val cards:List<MemoryCard>
    var pairsFound=0

    private var noOfFlips=0
    private var indexOfSingleSelectedCard:Int?= null
    init {
        if(customImages==null) {
            val choosenImages: List<Int> = DEFAULT_ICONS.shuffled().take(boardSize.getNoOfPairs())
            val randomizedImagesList = (choosenImages + choosenImages).shuffled()
            cards = randomizedImagesList.map{ MemoryCard(it) }
        }
        else{
            val randomizedImageList= (customImages+customImages).shuffled()
            cards=randomizedImageList.map { MemoryCard(it.hashCode(),it) }
        }
    }
    fun flipCard(position: Int):Boolean {
        noOfFlips++
        val card= cards[position]
        var foundMatch = false
        if(indexOfSingleSelectedCard==null){
            //0 or 2 cards previously flipped
            restoreCards()
            indexOfSingleSelectedCard= position
        }
        else{
            foundMatch= checkForMatch(indexOfSingleSelectedCard!!,position)
            //exactly one card was previously flipped
            indexOfSingleSelectedCard=null
        }
        card.isFaceUp= !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean{
        if(cards[position1].identifier != cards[position2].identifier){
            return false
        }
        cards[position1].isMatched=true
        cards[position2].isMatched= true
        pairsFound++
        return true
    }

    private fun restoreCards() {
        for (card in cards){
            if(!card.isMatched){
                card.isFaceUp= false
            }
        }
    }

    fun haveWon(): Boolean {
        return pairsFound==boardSize.getNoOfPairs()

    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp

    }

    fun getNoOfMoves(): Int{
        return noOfFlips/2
    }

}