package com.example.tracedis.ui


import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.tracedis.R
import com.example.tracedis.util.Permissions.hasLocationPermission
import com.google.firebase.auth.FirebaseAuth


//@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

       // navController = findNavController(R.id.navhostFragment)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navhostFragment) as NavHostFragment?

        if(FirebaseAuth.getInstance().currentUser==null){
            startActivity(Intent(this, LoginActivity::class.java))
        }else {
            if (navHostFragment != null) {
                val navController = navHostFragment.navController
                if (hasLocationPermission(this)) {
                    navController.navigate(R.id.action_permissionFragment_to_mapsFragment)
                }
                // Setup NavigationUI here
                val dialog = AlertDialog.Builder(this)
                dialog.setTitle("ALERTS")
                dialog.setMessage("DON'T FORGET TO FREQUENTLY CHECK YOUR MESSAGES FOR IMPORTANT ALERTS")
                val btn = Button(this)
                btn.text = "GO TO MESSAGES"
                btn.setBackgroundColor(resources.getColor(R.color.orange))
                btn.setOnClickListener {
                    startActivity(Intent(this, MessagesActivity::class.java))
                }
                btn.width= 140
                dialog.setView(btn)

                dialog.setPositiveButton("Go To Messages", DialogInterface.OnClickListener { dialog, which ->
//                    Toast.makeText(
//                        this,
//                        "Hello",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    startActivity(Intent(this, MessagesActivity::class.java))
                    dialog.dismiss()
                })
                dialog.setNegativeButton("Close", DialogInterface.OnClickListener{
                        dialog, which ->
                    dialog.dismiss()
                })

                dialog.show()

            }

            val myToolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            setSupportActionBar(myToolbar)
            myToolbar.setTitle("TraceDis")
        }






    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                Toast.makeText(applicationContext, "click on setting", Toast.LENGTH_LONG).show()
                true
            }
            R.id.action_trace ->{
                startActivity(Intent(this, TraceActivity::class.java))
//                Toast.makeText(applicationContext, "click on share", Toast.LENGTH_LONG).show()
                return true
            }
            R.id.action_messages ->{
                startActivity(Intent(this, MessagesActivity::class.java))
//                Toast.makeText(applicationContext, "click on exit", Toast.LENGTH_LONG).show()
                return true
            }
//            R.id.action_track ->{
//                startActivity(Intent(this, MainActivity::class.java))
////                Toast.makeText(applicationContext, "click on exit", Toast.LENGTH_LONG).show()
//                return true
//            }
            else -> super.onOptionsItemSelected(item)
        }

    }

}