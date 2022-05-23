package com.example.tracedis.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tracedis.MessageTrace
import com.example.tracedis.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MessagesActivity : AppCompatActivity() {

    var messages: ArrayList<MessageTrace> = ArrayList()
    var uid = FirebaseAuth.getInstance().currentUser!!.uid
    var firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        val messagesRV = findViewById<RecyclerView>(R.id.messagesrecyclerView)
        val nomessagesTV = findViewById<TextView>(R.id.nomsgs)


        if(messages.isEmpty()){
            Log.d("messages", "onCreateView: is messages empty?")
            nomessagesTV.visibility = View.VISIBLE
            messagesRV.visibility= View.GONE
        }
        // this creates a vertical layout Manager
        messagesRV.layoutManager = LinearLayoutManager(this)
// This will pass the ArrayList to our Adapter
        val adapter = MessageAdapter(messages)

        // Setting the Adapter with the recyclerview
        messagesRV.adapter = adapter

        firestore.collection("users").document(uid).collection("messages").get().addOnSuccessListener { mList->

            for(ms in mList){
                var nMsg = MessageTrace()
                nMsg = ms.toObject(MessageTrace::class.java)
                messages.add(nMsg)


            }
            if(messages.isNotEmpty()){
                nomessagesTV.visibility = View.INVISIBLE
                messagesRV.visibility = View.VISIBLE
            }
            adapter.notifyDataSetChanged()

        }


// calling the action bar
//        var actionBar = getSupportActionBar()
//
//        // showing the back button in action bar
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true)
//            actionBar.setTitle("Messages")
//        }
        val myToolbar: Toolbar = findViewById<View>(R.id.toolbar3) as Toolbar
        setSupportActionBar(myToolbar)
        myToolbar.setBackgroundColor(getColor(R.color.blue_700))
        myToolbar.setTitle("MESSAGES")
        myToolbar.setNavigationOnClickListener(View.OnClickListener {
            // back button pressed
            startActivity(Intent(this, MainActivity::class.java))

        })



    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this, MainActivity::class.java))
                return true
            }
        }
        return super.onContextItemSelected(item)
    }
}