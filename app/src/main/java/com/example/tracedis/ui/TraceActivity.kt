package com.example.tracedis.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.tracedis.LocData
import com.example.tracedis.MessageTrace
import com.example.tracedis.R
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.SphericalUtil
import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import java.time.LocalDateTime
import java.util.*
import java.lang.System

class TraceActivity : AppCompatActivity() {

    var vir:String=""
    var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    var userid: String= FirebaseAuth.getInstance().currentUser!!.uid
    var userLocs: ArrayList<LocData> = ArrayList()
    var otherLocs: ArrayList<LocData> = ArrayList()
    var mssgList: ArrayList<MessageTrace> = ArrayList()
    var peopleIds: ArrayList<String> = ArrayList()

    lateinit var dateNow: LocalDateTime
    var aCCOUNT_SID: String = ""
    var aUTH_TOKEN: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trace)

//         aCCOUNT_SID = java.lang.System.getenv("TWILIO_ACCOUNT_SID")!!
//         aUTH_TOKEN = java.lang.System.getenv("TWILIO_AUTH_TOKEN")!!
//        val myToolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
//        setSupportActionBar(myToolbar)
//        myToolbar.setTitle("Trace Contacts")

        val viruses = resources.getStringArray(R.array.Viruses)

        val spinner = findViewById<Spinner>(R.id.spinner_V)
        val traceBtn = findViewById<Button>(R.id.traceBtn)
        if (spinner != null) {
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item, viruses
            )
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View, position: Int, id: Long) {

                    vir = viruses[position]


                    Toast.makeText(this@TraceActivity,
                        getString(R.string.selected_item) + " " +
                                "" + viruses[position], Toast.LENGTH_SHORT).show()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }


            traceBtn.setOnClickListener {
                //firestore.collection("locations").whereEqualTo("userId",userid).get().addOnSuccessListener { userFrLocs->

                    dateNow= LocalDateTime.now()
                    Log.d("Trace Activity", "onCreate: two weeks ago ${dateNow.toString()}")
                    val twoWeeksAgo = dateNow.minusDays(14)
                    Log.d("Trace Activity", "onCreate: two weeks ago ${twoWeeksAgo.toString()}")
                    val c = Calendar.getInstance()
                    c.set(Calendar.DAY_OF_MONTH, twoWeeksAgo.dayOfMonth)
                    c.set(Calendar.MONTH, twoWeeksAgo.monthValue-1)
                    c.set(Calendar.YEAR, twoWeeksAgo.year)
                    val twoWeeksAgoLng=c.time.time
                    Log.d("Trace Activity", "onCreate: two weeks ago ${c.time.toString()}")
                    Log.d("Trace Activity", "onCreate: two weeks ago ${twoWeeksAgoLng.toString()}")


//                    for (loc in userFrLocs){
//                        var userlocdata= LocData()
//                        userlocdata = loc.toObject(LocData::class.java)
//                        if(userlocdata.longDate>=twoWeeksAgoLng){
//                            userLocs.add(userlocdata)
//                        }

                //    }

                    //get all locations from two weeks ago to now
                    firestore.collection("locations").whereGreaterThanOrEqualTo("longDate",twoWeeksAgoLng).get().addOnSuccessListener { otherFrLocs ->
                        for (loc2 in otherFrLocs){
                            var otherlocdata= LocData()
                            otherlocdata= loc2.toObject(LocData::class.java)
                            if(otherlocdata.userId != userid){
                                otherLocs.add(otherlocdata)
                            }else{
                                userLocs.add(otherlocdata)
                            }

                        }
                        Log.d("Trace Activity", "onCreate: user locations size ${userLocs.size}")


                        Log.d("Trace Activity", "onCreate: other locations size ${otherLocs.size}")

                        //loop and make messages here
                        for (userloc in userLocs){
                            var ulatlng= LatLng(userloc.lat,userloc.lng)
                            for (otherloc in otherLocs){
                                var othlatlng= LatLng(otherloc.lat,otherloc.lng)
                                var distance = SphericalUtil.computeDistanceBetween(
                                        ulatlng,
                                        othlatlng)

//                                        .toFloat()
//                                ) / 100.00

                                if(distance<= 50){
                                    if(!peopleIds.contains(otherloc.userId)){
                                        peopleIds.add(otherloc.userId)
                                        //make a message object and
                                        var newMsg= MessageTrace()
                                        newMsg.addr=otherloc.address
                                        newMsg.email = otherloc.email
                                        newMsg.userId = otherloc.userId
                                        newMsg.strDate = otherloc.strDate
                                        newMsg.vir = vir
                                        newMsg.mssg= "you may have come in contact with someone who tested positive for $vir on ${newMsg.strDate} at ${newMsg.addr}"

                                        mssgList.add(newMsg)
                                    }
                                }
                            }
                        }
                        Log.d("Trace Activity", "onCreate: messages size ${mssgList.size}")


                        //loop and send the messages to each user and email
                        sendMessages(mssgList)

                    }


                }

            //}

        }



        val myToolbar: Toolbar = findViewById<View>(R.id.toolbar2) as Toolbar
        setSupportActionBar(myToolbar)
        myToolbar.setTitle("TraceDis")
        myToolbar.setBackgroundColor(getColor(R.color.blue_700))
        myToolbar.setNavigationOnClickListener(View.OnClickListener {
            // back button pressed
            startActivity(Intent(this, MainActivity::class.java))

        })

// calling the action bar
//        var actionBar = supportActionBar
//
//        // showing the back button in action bar
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true)
//            actionBar.setTitle("Trace Contacts")
//        }


    }
    // Here "layout_login" is a name of layout file
    // created for LoginFragment
    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this, MainActivity::class.java))
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    fun traceContacts(virr: String){

    }

    fun sendMessages(mssgs: ArrayList<MessageTrace>){

        for(msg in mssgs) {
            //get user name
            firestore.collection("users").document(msg.userId).get().addOnSuccessListener { usr->
                var username= usr.data?.get("name")
                var phoneNumber = usr.data?.get("phoneNumber").toString()
                //send email
                sendEmail(msg.email, "$username, ${msg.mssg}")
                msg.mssg="$username, ${msg.mssg}"
                firestore.collection("users").document(msg.userId).collection("messages").add(msg).addOnSuccessListener {

//                    Log.d("TWILIO", "sendMessages: $aCCOUNT_SID")
//                    Log.d("TWILIO"," $aUTH_TOKEN")

//                    Twilio.init(
//                        "AC8a4ec3e52ddea7dd88e04c8108044a2b",
//                        "f53aa2a4baa7b909ada04cf71c4c1d5f"
//                    )
//
//                    val message = Message.creator(
//                        PhoneNumber(phoneNumber),
//                        PhoneNumber("+1 959 400 4974"),
//                        "From TraceDis App: $username, ${msg.mssg}}"
//                    ).create()
                }



            }
        }

    }

    fun sendEmail(addy: String, msg: String){

    }
}