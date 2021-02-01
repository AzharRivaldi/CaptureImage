package com.azhar.captureimage.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.azhar.captureimage.BuildConfig
import com.azhar.captureimage.R
import com.azhar.captureimage.utils.GPSTracker
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    var gpsTracker: GPSTracker? = null
    var REQ_CAMERA = 100
    var fileSize = 0
    var imageFilePath: String? = null
    var encodedImage: String? = null
    var timeStamp: String? = null
    var imageName: String? = null
    var imageSize: String? = null
    var fileDirectoty: File? = null
    var imageFilename: File? = null
    var numberFormat: NumberFormat? = null
    lateinit var imageBytes: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //cek permission
        val verfiyPermission: Int = Build.VERSION.SDK_INT
        if (verfiyPermission > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (checkIfAlreadyhavePermission()) {
                requestForSpecificPermission()
            }
        }

        btnCapture.setOnClickListener {
            showPictureDialog()
        }
    }

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")

        val pictureDialogItems = arrayOf(
                "Select photo from gallery",
                "Capture photo from camera")
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> UploadImage()
                1 -> takeCameraImage()
            }
        }
        pictureDialog.show()
    }

    //ambil gambar dari kamera
    private fun takeCameraImage() {
        Dexter.withContext(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            try {
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                        FileProvider.getUriForFile(this@MainActivity,
                                                BuildConfig.APPLICATION_ID.toString() + ".provider", createImageFile()))
                                startActivityForResult(intent, REQ_CAMERA)
                            } catch (ex: IOException) {
                                Toast.makeText(this@MainActivity, "Gagal membuka kamera!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()
    }

    //ambil gambar dari galeri
    private fun UploadImage() {
        Dexter.withContext(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            val galleryIntent = Intent(Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            startActivityForResult(galleryIntent, REQUEST_PICK_PHOTO)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        timeStamp = SimpleDateFormat("dd MMMM yyyy HH:mm").format(Date())
        imageName = "JPEG_"
        fileDirectoty = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "")
        imageFilename = File.createTempFile(imageName, ".jpg", fileDirectoty)
        imageFilePath = imageFilename.getAbsolutePath()
        return imageFilename
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAMERA && resultCode == Activity.RESULT_OK) {
            convertImage(imageFilePath)
        } else if (requestCode == REQUEST_PICK_PHOTO && resultCode == Activity.RESULT_OK) {
            val selectedImage: Uri = data.getData()
            val filePathColumn = arrayOf<String>(MediaStore.Images.Media.DATA)
            assert(selectedImage != null)

            val cursor: Cursor = contentResolver.query(selectedImage, filePathColumn,
                    null, null, null)!!
            cursor.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val mediaPath = cursor.getString(columnIndex)

            cursor.close()
            imageFilePath = mediaPath
            convertImage(mediaPath)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun convertImage(urlImg: String?) {
        val imgFile = File(urlImg)
        if (imgFile.exists()) {
            val options: BitmapFactory.Options = BitmapFactory.Options()
            val bitmap: Bitmap = BitmapFactory.decodeFile(imageFilePath, options)

            imagePreview.setImageBitmap(bitmap)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

            imageBytes = baos.toByteArray()
            encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            //menghitung size gambar
            numberFormat = DecimalFormat()
            (numberFormat).setMaximumFractionDigits(2)
            fileSize = (imgFile.length() / 1024).toString().toInt()
            imageSize = (numberFormat).format(fileSize.toLong())
            gpsTracker = GPSTracker(this@MainActivity)

            //menampilkan data gambar
            if (gpsTracker.isGPSTrackingEnabled) {
                val latitude: Double? = gpsTracker.getLatitude()
                val longitude: Double? = gpsTracker.getLongitude()
                val lokasiGambar: String? = gpsTracker.getAddressLine()
                val modelHP = Build.MODEL
                val brandHP: String = Build.BRAND

                tvLocation.text = lokasiGambar + "\n\n" + latitude + ", " + longitude
                tvDateTime.text = timeStamp
                tvImageName.text = imageFilePath + "\n\n" + imageSize + " MB"
                tvDevice.text = brandHP + " " + modelHP
            } else {
                Toast.makeText(this@MainActivity,
                        "Tidak mendapatkan lokasi. Silahkan periksa GPS atau koneksi internet anda!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkIfAlreadyhavePermission(): Boolean {
        val result: Int = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 101)
    }

    companion object {
        private const val REQUEST_PICK_PHOTO = 1
    }
}