package com.example.tracedis.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.tracedis.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {


    lateinit var etName: EditText
    lateinit var etEmail: EditText
    lateinit var etPhoneNum: EditText
    lateinit var etConfPass: EditText
    private lateinit var etPass: EditText
    private lateinit var btnSignUp: Button
    lateinit var tvRedirectLogin: TextView

    // create Firebase authentication object
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // View Bindings
        etName = findViewById(R.id.etSname);
        etEmail = findViewById(R.id.etSEmailAddress)
        etPhoneNum = findViewById(R.id.etSPhoneNumber)
        etConfPass = findViewById(R.id.etSConfPassword)
        etPass = findViewById(R.id.etSPassword)
        btnSignUp = findViewById(R.id.btnSSigned)
        tvRedirectLogin = findViewById(R.id.tvRedirectLogin)

        // Initialising auth object
        auth = FirebaseAuth.getInstance()

        btnSignUp.setOnClickListener {
            signUpUser()
        }

        // switching from signUp Activity to Login Activity
        tvRedirectLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }

    private fun signUpUser() {
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()
        val confirmPassword = etConfPass.text.toString()
        val phoneNum =etPhoneNum.text.toString()
        val name = etName.text.toString()
        // check pass
        if (email.isBlank() || pass.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Email and Password can't be blank", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmPassword) {
            Toast.makeText(this, "Password and Confirm Password do not match", Toast.LENGTH_SHORT)
                .show()
            return
        }
        // If all credential are correct
        // We call createUserWithEmailAndPassword
        // using auth object and pass the
        // email and pass in it.
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) {authTask->
            if (authTask.isSuccessful) {

                //add to firestore
                val newUser = hashMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "age" to 24,
                    "phoneNumber" to phoneNum,
                    "password" to pass
                )

                val uid=authTask.result.user!!.uid

                FirebaseFirestore.getInstance().collection("users").document(uid).set(newUser).addOnSuccessListener {
                        Log.d("Register", "signUpUser: added document with ID ${authTask.result.user!!.uid}")

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        // using finish() to end the activity
                        finish()
                    }.addOnFailureListener { exception ->
                        Log.w("Register", "Error adding document $exception")
                    }


                Toast.makeText(this, "Successfully Singed Up", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Singed Up Failed!", Toast.LENGTH_SHORT).show()
            }
        }

    }
}