package com.example.blurdetect

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ExifInterface
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    lateinit var capReq : CaptureRequest.Builder
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var cameraManager: CameraManager
    lateinit var texttureView : TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
//    lateinit var captureRequest: CaptureRequest
    lateinit var imageReader : ImageReader
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        get_permissions()

        init()
    }

    fun init(){

        texttureView = findViewById(R.id.texture_view)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler((handlerThread).looper)
        imageReader = ImageReader.newInstance(600,600, ImageFormat.JPEG,1)
        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener{
            override fun onImageAvailable(reader: ImageReader?) {
                var image = reader?.acquireLatestImage()
                var buffer =image!!.planes[0].buffer
                var bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val rotatedBytes = rotateImage(bytes, 90)
                val rotatedBitmap = BitmapFactory.decodeByteArray(rotatedBytes, 0, rotatedBytes.size)
                // Khi có đối tượng Bitmap, bạn có thể chỉnh kích thước
                val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 600, 600, false)
                // Sau đó, chuyển đối tượng Bitmap đã chỉnh kích thước thành mảng bytes
                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                val resizedBytes = outputStream.toByteArray()
                // Xác định thư mục album ảnh chính
                val albumPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera"
                // Tạo thư mục nếu nó không tồn tại
                val albumDir = File(albumPath)
                if (!albumDir.exists()) {
                    albumDir.mkdirs()
                }
                // Tạo tên tệp cho ảnh (có thể sử dụng timestamp hoặc tên tùy ý)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val imageFileName = "IMG_$timeStamp.jpg"
                // Tạo tệp ảnh
                val imageFile = File(albumDir, imageFileName)
                // Lưu ảnh đã chụp vào tệp
                val outStream = FileOutputStream(imageFile)
                outStream.write(resizedBytes) // Trong đó `bytes` là dữ liệu hình ảnh sau khi chụp
                outStream.close()
                // Cập nhật MediaStore để ảnh mới được thêm vào album
                MediaScannerConnection.scanFile(
                    this@MainActivity,
                    arrayOf(imageFile.absolutePath),
                    null,
                    null
                )
                Toast.makeText(this@MainActivity,"Image Capture", Toast.LENGTH_SHORT).show()
                image?.close()
            }
        },handler)

        findViewById<ImageButton>(R.id.capture).apply {
            setOnClickListener {
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capReq.addTarget(imageReader.surface)
                cameraCaptureSession.capture(capReq.build(),null,null)
            }
        }

        findViewById<ImageButton>(R.id.album).apply {
            setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, PICK_IMAGE_REQUEST)
            }
        }

        texttureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }
        }



        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val layoutParams = texttureView.layoutParams
        layoutParams.width = size.x
        layoutParams.height = size.x // Đảm bảo rằng chiều cao cũng bằng chiều rộng
        texttureView.layoutParams = layoutParams
    }

    fun get_permissions(){
        var permissionLst = mutableListOf<String>()
        if (checkSelfPermission(android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            permissionLst.add(android.Manifest.permission.CAMERA)
        }
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            permissionLst.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            permissionLst.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(permissionLst.size > 0){
            requestPermissions(permissionLst.toTypedArray(),101)
        }
    }

    @SuppressLint("MissingPermission")
    fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0],object : CameraDevice.StateCallback(){
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                var surface = Surface(texttureView.surfaceTexture)
                capReq.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface,imageReader.surface),object : CameraCaptureSession.StateCallback(){
                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        cameraCaptureSession.setRepeatingRequest(capReq.build(),null,null)
                    }
                },handler)
            }

            override fun onDisconnected(camera: CameraDevice) {

            }

            override fun onError(camera: CameraDevice, error: Int) {

            }
        },handler)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if(it != PackageManager.PERMISSION_GRANTED){
                get_permissions()
            }
        }
    }

    fun rotateImage(data: ByteArray, degree: Int): ByteArray {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())

        val originalBitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        val rotatedBitmap = Bitmap.createBitmap(
            originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
        )

        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return outputStream.toByteArray()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            // Bây giờ bạn có thể làm gì đó với URI của ảnh, ví dụ: hiển thị nó trên ImageView
            val intent = Intent(this, PredictActivity::class.java)
            intent.putExtra("imageUri", selectedImageUri.toString())
            startActivity(intent)
        }
    }
}