package com.vivekapp.memeorygame

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.vivekapp.memeorygame.models.BoardSize
import com.vivekapp.memeorygame.models.MemoryGameLogic
import com.vivekapp.memeorygame.models.UserImageList
import com.vivekapp.memeorygame.utils.EXTRA_BOARD_SIZE
import com.vivekapp.memeorygame.utils.EXTRA_GAME_NAME

class MainActivity : AppCompatActivity() {
    companion object{
        private const val CREATE_REQUEST_CODE= 100
        private const val TAG="Main Activity"
    }

    private lateinit var adapter: GameGridAdapter
    private lateinit var memoryGameLogic: MemoryGameLogic
    private lateinit var recyclerView: RecyclerView
    private lateinit var movesTextView: TextView
    private lateinit var clRoot:ConstraintLayout
    private lateinit var pairsTextView: TextView
    private val firestore= Firebase.firestore
    private var gameName:String?=null
    private var customGameImages:List<String>?= null

    private  var boardSize : BoardSize=BoardSize.EASY
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clRoot= findViewById(R.id.clRoot)
        movesTextView= findViewById(R.id.noOfMovesTV)
        pairsTextView= findViewById(R.id.noOfPairsTV)
        recyclerView= findViewById(R.id.recyclerView)

        setUpGame()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.mi_refresh ->{
                //set up game again
                if(memoryGameLogic.getNoOfMoves()>0 && !memoryGameLogic.haveWon()){
                    showAlertDialog("Want to quit this game?", null,View.OnClickListener {
                        setUpGame()
                    })
                }else {
                    setUpGame()
                }
                return true
            }
            R.id.mi_choose_game_level ->{
                showSizeDialog()
                return  true
            }
            R.id.mi_custom_game->{
                showCreationDialog()
               return true
            }
            R.id.mi_download_game->{
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode== CREATE_REQUEST_CODE && resultCode==Activity.RESULT_OK ){
            val customGameName= data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName==null){

                Log.e(TAG,"Got null custom game name from create Activity.")
                return
            }
            dowloadGame(customGameName)

        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    private fun showDownloadDialog() {
        val boardDownloadView= LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
        showAlertDialog("Fetch memory game",boardDownloadView,View.OnClickListener {
            //grab the text of the game name that the user wants to download
            val downloadGameEt= boardDownloadView.findViewById<EditText>(R.id.search_game_name_ET)
            val gameToDownload=downloadGameEt.text.toString().trim()
            dowloadGame(gameToDownload)
        })
    }
    private fun dowloadGame(customGameName: String) {
        firestore.collection("games").document(customGameName).get().addOnSuccessListener { document->
           val userImageList= document.toObject(UserImageList::class.java)
            if(userImageList?.images==null){
                Snackbar.make(clRoot,"Sorry, we could not get any such game, '$customGameName'",Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            val noOfCards= userImageList.images.size *2
            customGameImages= userImageList.images
            for(imageUrl:String in userImageList.images){
                Picasso.get().load(imageUrl).placeholder(R.drawable.ic_image).fetch()    //by fetch() we are saving in picasso catche even if we are not displaying it
            }
            Snackbar.make(clRoot,"You are playing $customGameName",Snackbar.LENGTH_SHORT).show()
            boardSize=BoardSize.getByValue(noOfCards)
            gameName= customGameName
            setUpGame()



        }.addOnFailureListener {

        }
    }


    private fun showCreationDialog() {
        val boardSizeView= LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize= boardSizeView.findViewById<RadioGroup>(R.id.radioGroup_id)

        showAlertDialog("Choose a custom size", boardSizeView,View.OnClickListener {
           val desiredBoardSize=when(radioGroupSize.checkedRadioButtonId){
                R.id.radioButton_easy-> BoardSize.EASY
                R.id.radio_button_medium-> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //navigate user to a new screen
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            startActivityForResult(intent,CREATE_REQUEST_CODE)


        })
    }

    private fun showSizeDialog() {
       val boardSizeView= LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize= boardSizeView.findViewById<RadioGroup>(R.id.radioGroup_id)
        when(boardSize){
            BoardSize.HARD -> radioGroupSize.check(R.id.radio_button_hard)
            BoardSize.MEDIUM-> radioGroupSize.check(R.id.radio_button_medium)
            BoardSize.EASY-> radioGroupSize.check(R.id.radioButton_easy)
        }
        showAlertDialog("Choose new Size", boardSizeView,View.OnClickListener {
            boardSize=when(radioGroupSize.checkedRadioButtonId){
                R.id.radioButton_easy-> BoardSize.EASY
                R.id.radio_button_medium-> BoardSize.MEDIUM

                else -> BoardSize.HARD
            }
            gameName=null
            customGameImages=null
            setUpGame()

        })
    }

    private fun showAlertDialog(title:String, view:View?,positiveButtonClickListener:View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("OK"){_,_ ->
                positiveButtonClickListener.onClick(null)
            }.show()
    }

    private fun setUpGame() {
        supportActionBar?.title=gameName?:getString(R.string.app_name)
        when (boardSize){
            BoardSize.EASY ->{
                pairsTextView.text= "Pairs: 0 / 4"
                movesTextView.text= "EASY: 4 x 2"
            }
            BoardSize.MEDIUM -> {
                pairsTextView.text= "Pairs: 0 / 9"
                movesTextView.text= "MEDIUM: 6 x 3"
            }
            BoardSize.HARD -> {
                pairsTextView.text= "Pairs: 0 / 12"
                movesTextView.text= "HARD: 6 x 4"
            }
        }
        pairsTextView.setTextColor(ContextCompat.getColor(this,R.color.no_progress_color))
        memoryGameLogic=MemoryGameLogic(boardSize,customGameImages)
        adapter= GameGridAdapter(this,boardSize,memoryGameLogic.cards, object:GameGridAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updteGameWithFLip(position)
            }
        })
        recyclerView.adapter= adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager= GridLayoutManager(this, boardSize.getWidth())

    }

    private fun updteGameWithFLip(position: Int) {
        // error handling
        if(memoryGameLogic.haveWon()){
            Snackbar.make(clRoot,"You have alreary won!!", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGameLogic.isCardFaceUp(position)){
            Snackbar.make(clRoot,"Invalid move!!", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGameLogic.flipCard(position)){
            val TAG= "Main Activity"
            Log.i(TAG, "Found a match. No of pairs found: ${memoryGameLogic.pairsFound}")

            val color= ArgbEvaluator().evaluate(
                memoryGameLogic.pairsFound.toFloat()/boardSize.getNoOfPairs(),
                ContextCompat.getColor(this,R.color.no_progress_color),
                ContextCompat.getColor(this,R.color.full_progress_color)
            )as Int
            pairsTextView.setTextColor(color)
            pairsTextView.text= "Pairs: ${memoryGameLogic.pairsFound}/ ${boardSize.getNoOfPairs()}"
            if(memoryGameLogic.haveWon()){
                Snackbar.make(clRoot,"Congratulations!! You Won.", Snackbar.LENGTH_SHORT).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.YELLOW,Color.GREEN,Color.RED,Color.MAGENTA)).oneShot()
            }
        }
        movesTextView.text="Moves: ${memoryGameLogic.getNoOfMoves()}"
        adapter.notifyDataSetChanged()
    }
}