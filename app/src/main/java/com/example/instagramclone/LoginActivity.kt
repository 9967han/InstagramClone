package com.example.instagramclone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Base64.encode
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var RC_SIGN_IN = 9000
    var callbackManager : CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        eamil_login_button.setOnClickListener {
            signinAndSignup()
        }
        google_signin_button.setOnClickListener {
            //Step 1
            googleLogin()
        }
        facebook_signin_button.setOnClickListener {
            //Step 1
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
//        printHashKey()
        callbackManager = CallbackManager.Factory.create()
    }

    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }

    fun printHashKey() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey: String = String(Base64.encode(md.digest(), 0))
                Log.i("TAG", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("TAG", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("TAG", "printHashKey()", e)
        }
    }


    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun facebookLogin(){
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult?) {
                    //Step 2
                    handleFacebookAcceessToken(result?.accessToken)
                }

                override fun onCancel() {
                    Log.i("facebook_callback", "cancel")
                }

                override fun onError(error: FacebookException?) {
                    Log.i("facebook_callback", error.toString())
                }

            })
    }

    fun handleFacebookAcceessToken(accessToken: AccessToken?) {
        var credential = FacebookAuthProvider.getCredential(accessToken?.token!!)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    //Step 3
                    //Login
                    moveMainPage(task.result?.user)
                } else {
                    //show the error message
                    Log.i("facebook_callback", task.exception?.message.toString())
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            var result = GoogleSignIn.getSignedInAccountFromIntent(data)
            var account = result.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account.idToken)
            }
        }
    }
    fun firebaseAuthWithGoogle(idToken: String?){
        var credential = GoogleAuthProvider.getCredential(idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    //Creating a user account
                    moveMainPage(task.result?.user)
                } else {
                    //show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun signinAndSignup(){
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    //Creating a user account
                    moveMainPage(task.result?.user)
                } else if(!task.exception?.message.isNullOrEmpty()) {
                    //Show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                } else {
                    signinEmail()
                    //login
                }
        }
    }
    fun signinEmail(){
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    //Creating a user account
                    moveMainPage(task.result?.user)
                } else {
                    //show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }
    fun moveMainPage(user: FirebaseUser?){
        if(user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }


}