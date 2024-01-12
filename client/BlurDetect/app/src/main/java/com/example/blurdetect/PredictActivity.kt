package com.example.blurdetect

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.blurdetect.api.ApiConfig
import com.example.blurdetect.api.ApiService
import com.example.blurdetect.model.ImageRespon
import com.example.blurdetect.utils.Utils
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.nio.ByteBuffer


class PredictActivity : AppCompatActivity() {
    lateinit var textView: TextView
    lateinit var imageView: ImageView
    lateinit var predictBtn : Button
    lateinit var uri_global:Uri
    lateinit var progressDialog : ProgressDialog
    private var client: ApiService = ApiConfig.retrofit.create(ApiService::class.java)
    private var is_deblur = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)

        imageView = findViewById<ImageView>(R.id.image_view)
        textView = findViewById(R.id.notifi)
        predictBtn = findViewById(R.id.predict)

        // init progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Please wait ...")

        val imageUri = intent.getStringExtra("imageUri")
        if (imageUri != null) {
            val uri = Uri.parse(imageUri)
            uri_global = uri
            imageView.setImageURI(uri)
        }

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val layoutParams = imageView.layoutParams
        layoutParams.width = size.x
        layoutParams.height = size.x // Đảm bảo rằng chiều cao cũng bằng chiều rộng
        imageView.layoutParams = layoutParams

        clickButton()
    }

    fun clickButton(){
        predictBtn.setOnClickListener {
            if (uri_global!=null){
                if(is_deblur){
                    callApiDeblurImage(uri_global)
                    is_deblur = false
                }else {
                    callApiUploadImage(uri_global)
                    is_deblur = true
                }
            }else {
                Toast.makeText(this@PredictActivity,"Uri Global is NULL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun callApiUploadImage(uri: Uri){
        progressDialog.show()

        val file  = File(Utils.getPathFromUri(this, uri))
        val requestBody = RequestBody.create(MediaType.parse(contentResolver.getType(uri)!!), file)
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestBody)
        val call:Call<ImageRespon> = client.uploadImage(imagePart)
        call.enqueue(object : Callback<ImageRespon>{
            override fun onResponse(call: Call<ImageRespon>, response: Response<ImageRespon>) {
                progressDialog.dismiss()
                if(response.isSuccessful){
                    response.body()?.let {
                        val blurry = it.blur
                        val image_path = it.image
                        if(blurry){
                            textView.text = "Blur Image"
                        }else{
                            textView.text = "Sharp Image"
                        }
                        Glide.with(this@PredictActivity)
                            .load(image_path)
                            .into(imageView)
                        Log.e("API_RESPONSE_HAHA",blurry.toString())
                        Log.e("API_RESPONSE_HAHA",image_path)
                    }
                }
            }

            override fun onFailure(call: Call<ImageRespon>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@PredictActivity,t.toString(), Toast.LENGTH_SHORT).show()
                Log.e("API_ERROR",t.toString())
            }

        })
    }

    fun callApiDeblurImage(uri: Uri){
        progressDialog.show()

        val file  = File(Utils.getPathFromUri(this, uri))
        val requestBody = RequestBody.create(MediaType.parse(contentResolver.getType(uri)!!), file)
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestBody)
        val call:Call<ImageRespon> = client.deblurImage(imagePart)
        call.enqueue(object : Callback<ImageRespon>{
            override fun onResponse(call: Call<ImageRespon>, response: Response<ImageRespon>) {
                progressDialog.dismiss()
                if(response.isSuccessful){
                    response.body()?.let {
                        val image_path = it.image
                        Glide.with(this@PredictActivity)
                            .load(image_path)
                            .into(imageView)
                        Log.e("API_RESPONSE_HAHA",image_path)
                    }
                }
            }

            override fun onFailure(call: Call<ImageRespon>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@PredictActivity,t.toString(), Toast.LENGTH_SHORT).show()
                Log.e("API_ERROR_DEBLUR",t.toString())
            }

        })
    }
//
//    fun getByteBufferFromImageView(imageView: ImageView, targetWidth: Int, targetHeight: Int): ByteBuffer {
//        // Lấy tấm ảnh từ ImageView
//        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
//
//        // Resize kích thước của ảnh
//        val resizedBitmap = resizeBitmap(bitmap, targetWidth, targetHeight)
//
//        // Chuyển ảnh đã được resize thành ByteBuffer
//        val byteBuffer = bitmapToByteBuffer(resizedBitmap)
//
//        return byteBuffer
//    }
//
//    fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
//        val width = bitmap.width
//        val height = bitmap.height
//        val scaleWidth = targetWidth.toFloat() / width
//        val scaleHeight = targetHeight.toFloat() / height
//
//        val matrix = Matrix()
//        matrix.postScale(scaleWidth, scaleHeight)
//
//        // Tạo một Bitmap mới có kích thước đã thay đổi.
//        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
//    }
//
//    fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
//        val modelInputSize = 900 // Kích thước đầu vào của mô hình
//        val byteBuffer = ByteBuffer.allocateDirect(4 * modelInputSize * modelInputSize * 3) // 3 kênh màu RGB.
//
//        // Chuyển đổi ảnh Bitmap thành dữ liệu dạng ByteBuffer
////        val pixels = IntArray(modelInputSize * modelInputSize)
////        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
////
////        for (pixel in pixels) {
////            val r = (pixel shr 16 and 0xFF).toFloat()
////            val g = (pixel shr 8 and 0xFF).toFloat()
////            val b = (pixel and 0xFF).toFloat()
////
////            byteBuffer.putFloat(r / 255.0f)
////            byteBuffer.putFloat(g / 255.0f)
////            byteBuffer.putFloat(b / 255.0f)
////        }
//
//        byteBuffer.rewind()
//        return byteBuffer
//    }

}