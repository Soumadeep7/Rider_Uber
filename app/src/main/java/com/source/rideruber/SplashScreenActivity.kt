package com.source.rideruber

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.source.rideruber.utils.Constatnts.RIDER_INFO_REFERENCE
import io.reactivex.rxjava3.core.Completable
import com.google.firebase.database.ValueEventListener
import com.source.rideruber.models.RiderModel
import com.source.rideruber.utils.Constatnts
import java.util.concurrent.TimeUnit
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var riderInfoRef: DatabaseReference

    private lateinit var getResult: ActivityResultLauncher<Intent>
    private lateinit var progressBar: ProgressBar

    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
       // FirebaseAuth.getInstance().signOut()
        init()

        getResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        displaySplashScreen()
    }

    private fun displaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread()) .subscribe(){
            firebaseAuth.addAuthStateListener(listener)
        }
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null){
            firebaseAuth.removeAuthStateListener(listener)
        }
        super.onStop()
    }

    private fun init() {
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        riderInfoRef = database.getReference(RIDER_INFO_REFERENCE)
        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if (user != null) {
                checkUserFromFirebase()
            } else {
                showRegisterLayout()

            }
        }
    }

    private fun checkUserFromFirebase() {
        riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        val model = snapshot.getValue(RiderModel::class.java)
                        goToHomeActivity(model)
                    } else {
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT).show()

                }
            }
        )
    }

    private fun showRegisterLayout() {

        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.sign_in_layout)
            .setPhoneButtonId(R.id.button_phone_sign_in)
            .setGoogleButtonId(R.id.button_google_sign_in)
            .build()

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setTheme(R.style.LoginTheme)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()

        getResult.launch(signInIntent)


    }

    private fun goToHomeActivity(model: RiderModel?) {
        Constatnts.currentUser = model
      //  startActivity(Intent(this, HomeActivity::class.java))
        //finish()
    }

    private fun showLoginLayout () {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.register_layout, null)

        val edit_text_name = itemView.findViewById<EditText>(R.id.edit_text_first_name) as TextInputEditText
        val edit_text_last_name = itemView.findViewById<EditText>(R.id.edit_text_last_name) as TextInputEditText
        val edit_text_phone_number = itemView.findViewById<EditText>(R.id.edit_text_phone_number) as TextInputEditText

        val buttonContinue = itemView.findViewById<Button>(R.id.button_register) as Button

        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null
            && !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber)) {
            edit_text_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        }

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        buttonContinue.setOnClickListener {
            if (TextUtils.isDigitsOnly(edit_text_name.text.toString())) {
                Toast.makeText(this@SplashScreenActivity, "Please enter a First name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener


            } else if (TextUtils.isDigitsOnly(edit_text_last_name.text.toString())) {
                Toast.makeText(this@SplashScreenActivity, "Please enter a Last name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener


            } else if (TextUtils.isEmpty(edit_text_phone_number.text.toString())) {
                Toast.makeText(this@SplashScreenActivity, "Please enter a Phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            } else {
                val model = RiderModel(
                    edit_text_name.text.toString(),
                    edit_text_last_name.text.toString(),
                    edit_text_phone_number.text.toString(),
                    0.0,
                    ""
                )

                riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model).addOnFailureListener {
                        Toast.makeText(this@SplashScreenActivity, "${it.message}", Toast.LENGTH_SHORT)
                            .show()
                        dialog.dismiss()
                    }.addOnSuccessListener {
                        Toast.makeText(this@SplashScreenActivity, "Register Successfully", Toast.LENGTH_SHORT)
                            .show()
                        dialog.dismiss()

                        goToHomeActivity(model)
                        progressBar.visibility = View.GONE
                    }
            }
        }
    }
}