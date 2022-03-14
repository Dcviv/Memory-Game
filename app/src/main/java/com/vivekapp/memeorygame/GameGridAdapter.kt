package com.vivekapp.memeorygame

import android.content.Context
import android.nfc.Tag
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vivekapp.memeorygame.models.BoardSize
import com.vivekapp.memeorygame.models.MemoryCard
import kotlin.math.*

class GameGridAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) : RecyclerView.Adapter<GameGridAdapter.ViewHolder>() {



     inner class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView) {
         private val imageButton= itemView.findViewById<ImageButton>(R.id.imageButt)
         fun bind(position: Int) {
             val memoryCard = cards[position]
             if(memoryCard.isFaceUp){
                 if(memoryCard.imageUrl!=null){
                     Picasso.get().load(memoryCard.imageUrl).into(imageButton)
                 }
                 else{
                     imageButton.setImageResource(memoryCard.identifier)
                 }

             }
             else{
                 imageButton.setImageResource(R.drawable.card_bg)
             }


             imageButton.alpha=if(memoryCard.isMatched) 0.4f else 1.0f
            val colorStateList =if(memoryCard.isMatched) ContextCompat.getColorStateList(context,
                androidx.appcompat.R.color.material_grey_300
            ) else null
             ViewCompat.setBackgroundTintList(imageButton,colorStateList)


             imageButton.setOnClickListener {
                 cardClickListener.onCardClicked(position)

                 Log.i(TAG,"Clicked card at "+position)
             }

         }
     }
    companion object{
        private const val MARGIN_SIZE=10
        private const val TAG="GameGridAdapter"
    }
    interface CardClickListener{
        fun onCardClicked(position: Int)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth= parent.width/boardSize.getWidth() -2* MARGIN_SIZE
        val cardHeight= parent.height/boardSize.getHeight() - 2* MARGIN_SIZE
        val cardSidelength=min(cardHeight,cardWidth)

        val view:View=  LayoutInflater.from(context).inflate(R.layout.memory_cars,parent,false)
        val layoutParams:ViewGroup.MarginLayoutParams =view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.height=cardSidelength
        layoutParams.width=cardSidelength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return boardSize.numOfCards
    }


}

