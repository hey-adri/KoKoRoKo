@file:Suppress("RemoveSingleExpressionStringTemplate", "RedundantSamConstructor",
    "RedundantSamConstructor", "RedundantSamConstructor", "RedundantSamConstructor",
    "RedundantSamConstructor")

package com.example.kokoroko

import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.transition.TransitionManager
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.zxing.integration.android.IntentIntegrator
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var webHookUrl:String? =""
    private var playSound:Boolean=true

    //Splash screen timeout
    private val splashActivityTimeOut:Long = 3000

    //CurrentMood 0 standard, 1 nodding, 2 sad, 3 settings
    private var currentMood =0
    private var timeToReturnToMainLayout:Long = 15000

    //MediaPlayer Object
    private lateinit var mMediaPlayer: MediaPlayer
    private var mediaPlayerInitialized : Boolean = false

    //Qr Code scanner object
    private lateinit var mQrResultLauncher : ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Setting up qr scanner
        qrScannerListenerSetUp()

        setSplashScreenLayout()








    }

    //Set Layout Methods

    //Sets up the spalsh screen while loads data and gets Qr Scanner ready
    private fun setSplashScreenLayout(){
        setContentView(R.layout.activity_splash)
        vibrate(50)
        //Cargamos datos para reproducir audio o no

        playSound(R.raw.kokoroko_pirana_audio)


        if (loadData()){
            Handler().postDelayed(
                {setMainLayout()
                }
                , splashActivityTimeOut)

        }else{
            Handler().postDelayed(
                {setSettingsLayout()
                }
                , splashActivityTimeOut)

        }


    }

    private fun setMainLayout(){
        setContentView(R.layout.activity_main)
        //vibrate(50)



        //XML to Kotlin Object definition


        val newAlarmButton = findViewById<Button>(R.id.NewAlarmButton)
        val cancelAlarmButton= findViewById<Button>(R.id.CancelAlarmsButton)
        val headerTextView = findViewById<TextView>(R.id.headertextView)



        //Adding listeners to buttons
        newAlarmButton.setOnClickListener{newAlarmButtonClick()}
        cancelAlarmButton.setOnClickListener{cancelAlarmsButtonClicked()}
        headerTextView.setOnLongClickListener(View.OnLongClickListener { playSound(R.raw.click_audio);setSettingsLayout()})
    }

    private fun setSettingsLayout():Boolean{
        currentMood=3
        //vibrate(50)
        setContentView(R.layout.settings_layout)

        val qrScanImageView = findViewById<ImageView>(R.id.qrScanImageView)
        val webhookEditText = findViewById<EditText>(R.id.webHookEditText)
        val soundSwitch = findViewById<Switch>(R.id.SoundSwitch)
        val doneButton = findViewById<Button>(R.id.doneButton)



            //on click listeners
            doneButton.setOnClickListener{doneButtonClicked()}
            qrScanImageView.setOnClickListener{qrScanButtonClicked()}




        //Set app variable values to interactive views
        webhookEditText.setText(webHookUrl)
        soundSwitch.isChecked=playSound

        return true
    }


    //ButtonClicked Methods
    private fun newAlarmButtonClick(){
        playSound(R.raw.click_audio)
        vibrate(50)


        currentMood=1
        val timeFormat = SimpleDateFormat("hh:mm")

        //Creation of calendar to get the day , month and year right now to pass it to the timePicker
        val now = Calendar.getInstance()//calendar.getInstance() returns current time
        //timePickerDialog Creation
        val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
            val selectedTime = Calendar.getInstance()
            selectedTime.set(Calendar.HOUR_OF_DAY,hourOfDay)
            selectedTime.set(Calendar.MINUTE,minute)
            setAlarmAt(selectedTime.get(Calendar.HOUR_OF_DAY),selectedTime.get(Calendar.MINUTE))
            animateToAlarmCreatedlayout()
            playSound(R.raw.confirmation_short_audio)
            vibrate(50)

        },
            now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true)
        timePicker.show()








    }
    private fun cancelAlarmsButtonClicked() {
        playSound(R.raw.click_audio)
        vibrate(50)
        currentMood=2
        animateToAlarmCancelledlayout()
        val completeUrl = "${webHookUrl}?hour=-999&minutes=-999"
        getContentsUrl(completeUrl)
        playSound(R.raw.cancelled_audio)
        val messageTextView = findViewById<TextView>(R.id.MessageTextView)
        val timeTextView = findViewById<TextView>(R.id.timeTextView)
        messageTextView.text = getString(R.string.alarm_has_been_cancelled_text)
        timeTextView.text=""

    }
    private fun doneButtonClicked(){
        vibrate(50)
        playSound(R.raw.click_audio)
        val webhookEditText = findViewById<EditText>(R.id.webHookEditText)
        val soundSwitch = findViewById<Switch>(R.id.SoundSwitch)

        //Recogemos el valor de webhookEditText
        webHookUrl=webhookEditText.text.toString()
        playSound=soundSwitch.isChecked



        if(storeData()&&checkData()) {setMainLayout()}
    }
    private fun qrScanButtonClicked(){
        playSound(R.raw.click_audio)
        vibrate(50)
        // Starts QR scanner
        startScanner()
    }


    //Animate Layout Methods
    private fun animateToAlarmCreatedlayout() {
        val root = findViewById<ConstraintLayout>(R.id.root)
        var set=false
        val startingConstraintSet= ConstraintSet()
        startingConstraintSet.clone(root)
        val finishingConstraintSet=ConstraintSet()
        finishingConstraintSet.clone(this,R.layout.new_alarm_created)


        TransitionManager.beginDelayedTransition(root)
        val constraint=if(set) startingConstraintSet else finishingConstraintSet
        constraint.applyTo(root)
        set=!set

        Handler().postDelayed(
            Runnable {
                if(currentMood==1) {
                    val messageTextView = findViewById<TextView>(R.id.MessageTextView)
                    messageTextView.text = ""
                    animateToMainlayout()
                }
            }
            , timeToReturnToMainLayout)

    }

    private fun animateToAlarmCancelledlayout() {
        val root = findViewById<ConstraintLayout>(R.id.root)
        var set=false
        val startingConstraintSet= ConstraintSet()
        startingConstraintSet.clone(root)
        val finishingConstraintSet=ConstraintSet()
        finishingConstraintSet.clone(this,R.layout.alarm_cancelled)


        TransitionManager.beginDelayedTransition(root)
        val constraint=if(set) startingConstraintSet else finishingConstraintSet
        constraint.applyTo(root)
        set=!set


        Handler().postDelayed(Runnable {
            if (currentMood==2){
                animateToMainlayout()
            }
        }, timeToReturnToMainLayout)

    }

    private fun animateToMainlayout() {
        currentMood =0
        //vibrate(50)
        val root = findViewById<ConstraintLayout>(R.id.root)
        var set=false
        val startingConstraintSet= ConstraintSet()
        startingConstraintSet.clone(root)
        val finishingConstraintSet=ConstraintSet()
        finishingConstraintSet.clone(this,R.layout.activity_main)


        TransitionManager.beginDelayedTransition(root)
        val constraint=if(set) startingConstraintSet else finishingConstraintSet
        constraint.applyTo(root)
        set=!set


    }


    //Set Alarm Method
    @SuppressLint("SetTextI18n")
    private fun setAlarmAt(hours:Int,minutes:Int){
        val messageTextView = findViewById<TextView>(R.id.MessageTextView)
        val timeTextView = findViewById<TextView>(R.id.timeTextView)

        val completeUrl = "${webHookUrl}?hour=${hours}&minutes=${minutes}"
        getContentsUrl(completeUrl)

        messageTextView.text = getString(R.string.the_alarm_will_be_set_at_time_text)
        timeTextView.text="${String.format("%02d:%02d", hours, minutes)}"
    }

    //Get Url Method
    private fun getContentsUrl(web:String){
        val urlGetterthread = Thread(
            Runnable {
                val queue = Volley.newRequestQueue(this)
                // Request a string response from the provided URL.
                val stringRequest = StringRequest(Request.Method.GET, web, { }, { })
                // Add the request to the RequestQueue.
                queue.add(stringRequest)
            }
        )
        urlGetterthread.start()

    }


    //UX Methods
    private fun playSound(soundFileId: Int){
        if(playSound){
            if(mediaPlayerInitialized){
                mMediaPlayer.stop()
                mMediaPlayer.release()
            }

            val t1 = Thread(Runnable {
                mMediaPlayer = MediaPlayer.create(this, soundFileId)
                mediaPlayerInitialized = true

                mMediaPlayer.setOnCompletionListener{
                    mMediaPlayer.stop()
                    mMediaPlayer.release()
                    mediaPlayerInitialized = false
                }

                mMediaPlayer.start()
            })
            t1.start()
        }

    }
    private fun vibrate(milliseconds:Long){
        (this.getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    //toastThis is overloaded
    private fun toastThis(textInput:Int){
        Toast.makeText(this,textInput,Toast.LENGTH_LONG).show()
    }
    private fun toastThis(textInput:String){
        Toast.makeText(this,textInput,Toast.LENGTH_LONG).show()
    }


    //Store, Load & Check Data Methods

    //createShared Preferences file
    //store data {webhookUrl,...}
    private fun storeData():Boolean{
        //creation preferencesFile
        val preferencesFile = getPreferences(Context.MODE_PRIVATE)
        val preferencesEditor = preferencesFile.edit()//Object that edits shared preferences, we supply pairs of ("key",value) to the editor

        //set webhookUrl as ready for storing
        preferencesEditor.putString("WEBHOOK_URL",webHookUrl)
        preferencesEditor.putBoolean("PLAY_SOUND",playSound)
        val returnVal=preferencesEditor.commit()
        if(!returnVal){toastThis(R.string.preferences_have_been_stored)}
        return returnVal


    }

    //getSharedPreferences file
    //Assign values to variables {webhookUrl,...}
    //before finishing calls checkdata() and returns its value
    private fun loadData():Boolean {
        val preferencesFile = getPreferences(Context.MODE_PRIVATE) //We get the stored values from the sharedPreferences file supplying it pairs of ("key",defaultvalueInCaseNotFound)
        webHookUrl=preferencesFile.getString("WEBHOOK_URL","")
        playSound = preferencesFile.getBoolean("PLAY_SOUND",true)

        return checkData()


    }

    //Checks that all essential variables have values, if so, returns true,else if some haven't toast them and returns false
    private fun checkData():Boolean{
        var returnVar = true
        var toastMessage:String = getString(R.string.the_following_variables_dont_have_value)

        if (webHookUrl==""){
            toastMessage+="\nwebhhokUrl"
            returnVar=false
        }
        //toast if any variable doesnt have value
        if (!returnVar){toastThis(toastMessage)}
        return  returnVar
    }


    //Qr Scanner Methods

    //Sets up the Listener for the qr scanner
    //Modify what you want to do with the result here!!
    //Needed by Qr scanner
    private fun qrScannerListenerSetUp(){

        // Alternative to "onActivityResult", because that is "deprecated"
        mQrResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val result = IntentIntegrator.parseActivityResult(it.resultCode, it.data)

                if(result.contents != null) {
                    playSound(R.raw.confirmation_short_audio)
                    // Do something with the contents (this is usually a URL)
                    vibrate(50)
                    webHookUrl=result.contents
                    //Actualizamos el editText

                    val webhookEditText = findViewById<EditText>(R.id.webHookEditText)
                    webhookEditText.setText(result.contents)

                }
            }
        }

    }

    //Needed by Qr scanner
    //Start the QR Scanner
    private fun startScanner() {
        val scanner = IntentIntegrator(this)
        scanner.setBeepEnabled(false)
        // QR Code Format
        scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        // Set Text Prompt at Bottom of QR code Scanner Activity
        scanner.setPrompt(getString(R.string.scan_webhhok_qr_code))
        // Start Scanner (don't use initiateScan() unless if you want to use OnActivityResult)
        mQrResultLauncher.launch(scanner.createScanIntent())
    }


    }





