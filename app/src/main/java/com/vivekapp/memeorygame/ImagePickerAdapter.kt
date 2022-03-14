package com.vivekapp.memeorygame

import android.content.Context
import android.media.Image
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.vivekapp.memeorygame.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val choosenImageUriList:List<Uri>,
    private val boardSize: BoardSize,
    private val imageClisckListener:ImageClickListener
    ): RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener{
        fun onPlaceHolderClicked()
    }

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private val customIV= itemView.findViewById<ImageView>(R.id.custom_image_IV)
        fun bind(uri: Uri) {
            customIV.setImageURI(uri)
            customIV.setOnClickListener(null)

        }

        fun bind() {
           customIV.setOnClickListener {
               //Launch an intent to selectImages
               imageClisckListener.onPlaceHolderClicked()
           }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth= parent.width/boardSize.getWidth()
        val cardHeight= parent.height/boardSize.getHeight()
        val cardSidelength= min(cardHeight,cardWidth)

        val view:View=  LayoutInflater.from(context).inflate(R.layout.card_image,parent,false)
        val layoutParams:ViewGroup.LayoutParams =view.findViewById<ImageView>(R.id.custom_image_IV).layoutParams
        layoutParams.height=cardSidelength
        layoutParams.width=cardSidelength
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position<choosenImageUriList.size){
            holder.bind(choosenImageUriList[position])
        }
        else{
            holder.bind()

        }
    }


    override fun getItemCount(): Int {
        return boardSize.getNoOfPairs()
    }
}
