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
import com.source.rideruber.utils.Constants.RIDER_INFO_REFERENCE
import io.reactivex.rxjava3.core.Completable
import com.google.firebase.database.ValueEventListener
import com.source.rideruber.models.RiderModel
import com.source.rideruber.utils.Constants
import java.util.concurrent.TimeUnit
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authListener: FirebaseAuth.AuthStateListener
    private lateinit var riderInfoRef: DatabaseReference
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var getResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        firebaseAuth = FirebaseAuth.getInstance()
        riderInfoRef = FirebaseDatabase.getInstance()
            .getReference(Constants.RIDER_INFO_REFERENCE)

        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        getResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show()
            }
        }

        authListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user == null) {
                openFirebaseLogin()
            } else {
                checkUserFromFirebase(user.uid)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(authListener)
    }


    private fun openFirebaseLogin() {

        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.sign_in_layout)
            .setPhoneButtonId(R.id.button_phone_sign_in)
            .setGoogleButtonId(R.id.button_google_sign_in)
            .build()

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setTheme(R.style.Theme_RiderUber_Login)
            .setIsSmartLockEnabled(false)
            .build()

        getResult.launch(signInIntent)

    }

    private fun checkUserFromFirebase(uid: String) {
        riderInfoRef.child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val model = snapshot.getValue(RiderModel::class.java)
                        goToHomeActivity(model)
                    } else {
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.Theme_RiderUber_Dialog)
        val view = layoutInflater.inflate(R.layout.register_layout, null)
        builder.setView(view)
        val dialog = builder.create()
        dialog.show()

        val firstName = view.findViewById<TextInputEditText>(R.id.edit_text_first_name)
        val lastName = view.findViewById<TextInputEditText>(R.id.edit_text_last_name)
        val phone = view.findViewById<TextInputEditText>(R.id.edit_text_phone_number)
        val btn = view.findViewById<Button>(R.id.button_register)

        phone.setText(firebaseAuth.currentUser?.phoneNumber)

        btn.setOnClickListener {
            if (firstName.text.isNullOrEmpty() || lastName.text.isNullOrEmpty()) {
                Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val model = RiderModel(
                firstName.text.toString(),
                lastName.text.toString(),
                phone.text.toString(),
                0.0,
                ""
            )

            riderInfoRef.child(firebaseAuth.currentUser!!.uid)
                .setValue(model)
                .addOnSuccessListener {
                    dialog.dismiss()
                    goToHomeActivity(model)
                }
        }
    }

    private fun goToHomeActivity(model: RiderModel?) {
        Constants.currentUser = model
        // startActivity(Intent(this, HomeActivity::class.java))
    }
}
