package com.vivekapp.memeorygame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RecoverySystem
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.vivekapp.memeorygame.models.BoardSize
import com.vivekapp.memeorygame.utils.*
import java.io.ByteArrayOutputStream
import java.util.jar.Manifest

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val PICK_PHOTO_REQUEST_CODE=2064
        private const val READ_REQUEST_CODE=1024
        private const val TAG= "CreateActivity"
        private const val MIN_GAME_NAME_LENGTH=3
        private const val MAX_GAME_NAME_LENGTH=14
        private const val READ_PHOTOS_PERMISSION=android.Manifest.permission.READ_EXTERNAL_STORAGE

    }

    private val choosenImageUriList= mutableListOf<Uri>()
    private lateinit var boardSize: BoardSize
    private var numOfImagesRequired=-1
    private lateinit var imagePicker_RV:RecyclerView
    private lateinit var gameName_ET : EditText
    private lateinit var saveButton:Button
    private lateinit var progressBar:ProgressBar
    private lateinit var adapter:ImagePickerAdapter
    private val storage= Firebase.storage

    private val firestore= Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        imagePicker_RV= findViewById(R.id.image_picker_RV)
        saveButton= findViewById(R.id.save_button)
        gameName_ET= findViewById(R.id.game_name_ET)
        progressBar= findViewById(R.id.progressBar)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize= intent.getSerializableExtra(EXTRA_BOARD_SIZE)as BoardSize
        numOfImagesRequired= boardSize.getNoOfPairs()
        supportActionBar?.title="Choose images:(0 / $numOfImagesRequired)"

        saveButton.setOnClickListener {
            saveDataToFirebase()
        }



        gameName_ET.filters= arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        gameName_ET.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                saveButton.isEnabled= shouldEnableSaveButton()
            }
        })

        adapter= ImagePickerAdapter(this, choosenImageUriList, boardSize,object :ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                    launchIntentToChooseImages()
                }
                else{
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_REQUEST_CODE)
                }

            }
        })
        imagePicker_RV.adapter= adapter
        imagePicker_RV.setHasFixedSize(true)
        imagePicker_RV.layoutManager= GridLayoutManager(this,boardSize.getWidth())
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode== READ_REQUEST_CODE){
            if(grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                launchIntentToChooseImages()
            }
            else{
                Toast.makeText(this,"In order to play a custom game, you need to grant the access to your photos.",Toast.LENGTH_SHORT)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode!= PICK_PHOTO_REQUEST_CODE|| resultCode!= Activity.RESULT_OK|| data==null){
            Log.w(TAG, "Did not got the data back, user likely cancelled flow")
            return
        }
        val selectedUri=data.data
        val clipData= data.clipData
        if(clipData!=null){
            Log.i(TAG,"no of images ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount){
                val clipItem=clipData.getItemAt(i)
                if(choosenImageUriList.size<numOfImagesRequired) {
                    choosenImageUriList.add(clipItem.uri)
                }
                //till here
            }
        }
        else if(selectedUri!=null){
            Log.i(TAG,"data: $selectedUri")
            choosenImageUriList.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title= "Choose images:(${choosenImageUriList.size} / $numOfImagesRequired)"
        saveButton.isEnabled= shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if(choosenImageUriList.size!=numOfImagesRequired||gameName_ET.length()< MIN_GAME_NAME_LENGTH || gameName_ET.text.isBlank()){
            return false
        }
        return true
    }

    private fun launchIntentToChooseImages() {
        intent= Intent(Intent.ACTION_PICK)
        intent.type="image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        startActivityForResult(Intent.createChooser(intent,"Choose pics"),PICK_PHOTO_REQUEST_CODE)
    }

    private fun saveDataToFirebase(){
        saveButton.isEnabled=false
        val customgamename= gameName_ET.text.toString()
        firestore.collection("games").document(customgamename).get().addOnSuccessListener { document->
            if(document!=null && document.data!=null){
                AlertDialog.Builder(this)
                    .setTitle("A game already exist with the name ${customgamename}. Please choose another name.")
                    .setPositiveButton("OK",null)
                    .show()
                saveButton.isEnabled=true

            }else{
                handleImageUploading(customgamename)
            }
        }.addOnFailureListener { exception->
            Log.e(TAG,"encountered error while saving memory game", exception)
            Toast.makeText(this,"Encountered error while saving game.", Toast.LENGTH_SHORT).show()

        }
    }

    private fun handleImageUploading(customgamename: String) {
        progressBar.visibility=View.VISIBLE

        var didEncounteredError=false
        val uploadedImageUrls= mutableListOf<String>()

        Log.i(TAG,"saving to firebase")
        for((index,photoUri)in choosenImageUriList.withIndex()){
            val imageByteArray=getImageByteArray(photoUri)
            val filePath="images/$customgamename/${System.currentTimeMillis()}-$index.jpg"
            val photoReference= storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask->
                    photoReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask->
                    if(!downloadUrlTask.isSuccessful){
                        Toast.makeText(this,"Failed to upload images.", Toast.LENGTH_SHORT).show()
                        didEncounteredError= true
                        return@addOnCompleteListener
                    }
                    if(didEncounteredError){
                        progressBar.visibility=View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl= downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    progressBar.progress=uploadedImageUrls.size*100/choosenImageUriList.size
                    if(uploadedImageUrls.size==choosenImageUriList.size){
                        handleAllImagesUploaded(customgamename,uploadedImageUrls)
                    }
                }

        }

    }

    private fun handleAllImagesUploaded(gamenameEt:String, uploadedImageUrls: MutableList<String>) {
        firestore.collection("games").document(gamenameEt)
            .set(mapOf("images" to uploadedImageUrls))
            .addOnCompleteListener { gameCreationTask->
                if(!gameCreationTask.isSuccessful){
                    Toast.makeText(this,"Failed Game Creation",Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                progressBar.visibility=View.GONE
                AlertDialog.Builder(this)
                    .setTitle("Upload successful! Play your game '$gamenameEt'.")
                    .setPositiveButton("OK"){_,_ ->
                        val resultData= Intent()
                        resultData.putExtra(EXTRA_GAME_NAME,gamenameEt)
                        setResult(Activity.RESULT_OK,resultData)
                        finish()
                        
                    }.show()
                
            }

    }

    private fun getImageByteArray(photoUri: Uri):ByteArray {
        val originalBitmap= if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
            val source= ImageDecoder.createSource(contentResolver,photoUri)
            ImageDecoder.decodeBitmap(source)
        }else{
            MediaStore.Images.Media.getBitmap(contentResolver,photoUri)
        }
        val scaleBitmap= BitMapScaler.scaleToFitHeight(originalBitmap,250)
        val byteOutputStream=ByteArrayOutputStream()
        scaleBitmap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream)
        return byteOutputStream.toByteArray()

    }
}