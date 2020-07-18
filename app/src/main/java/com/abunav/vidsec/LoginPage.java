package com.abunav.vidsec;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Base64;

public class LoginPage extends AppCompatActivity {

    private FirebaseAuth mAuth;


    public void createAccount(final String email, final String password){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            Log.d("Sign up Status", "createUserWithEmail:success");
                            Toast.makeText(LoginPage.this, "Sign In Successful!",
                                    Toast.LENGTH_SHORT).show();
                            // need to add this user to the database along with their public key and store the private key internally
                            try {
                                RSA rsa = new RSA();
                                String publicKey = Base64.getEncoder().encodeToString(rsa.getPublicKey().getEncoded());
                                String privateKey = Base64.getEncoder().encodeToString(rsa.getPrivateKey().getEncoded());


                                // encrypt private key with the user's password and save in the local storage.

                                String encryptedPrivateKey = AES.encrypt(privateKey, password);
                                String path = "storage/emulated/0/VideoEncryptorFiles/key/"+email+"/";
                                File dir = new File(path);
                                if (!dir.exists()) {
                                    dir.mkdirs();
                                }
                                File file = new File(path + "private_key_encrypted.txt");
                                try {
                                    FileWriter fw = new FileWriter(file.getAbsoluteFile());
                                    BufferedWriter bw = new BufferedWriter(fw);
                                    bw.write(encryptedPrivateKey);
                                    bw.close();
                                    Toast.makeText(getApplicationContext(), "Your Private Key has been encrypted and written to /VideoEncryptorFiles/keys in your internal storage. You can decrypt this key while decrypting a text file by using your login password.", Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                /*
                                BufferedReader br = new BufferedReader(new FileReader(file));
                                StringBuilder stringBuilder = new StringBuilder();
                                String line = null;
                                String ls = System.getProperty("line.separator");
                                while((line = br.readLine()) != null){
                                    stringBuilder.append(line);
                                    stringBuilder.append(ls);
                                }
                                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                                br.close();

                                String fileContents = stringBuilder.toString();
                                String decryptedPrivateKey = AES.decrypt(fileContents, password);
                                Log.i("AESSSSS", String.valueOf(decryptedPrivateKey.equals(privateKey)));

                                */

                                FirebaseDatabase rootnode;
                                DatabaseReference reference;

                                rootnode = FirebaseDatabase.getInstance();
                                reference = rootnode.getReference("Users");

                                String mail = email.replace(".",",");
                                reference.child(mail).setValue(publicKey);
                                Toast.makeText(getApplicationContext(), "Your public key has been uploaded to the server. Any user who wants to send you a video can use this key to encrypt their file!", Toast.LENGTH_LONG).show();
                            }catch (Exception e) {
                                e.printStackTrace();
                            }
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Sign up Status", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginPage.this, "Sign Up failed: ".concat(task.getException().getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void signin(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Sign In Status", "signInWithEmail:success");
                            Toast.makeText(LoginPage.this, "Log In Successful!",
                                    Toast.LENGTH_LONG).show();

                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Sign in Status", "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginPage.this, "Log In failed: ".concat(task.getException().getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }

                    }
                });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);
        getSupportActionBar().hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        /*
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url

            String email = user.getEmail();
            String uid = user.getUid();

            Log.i("Signed in Email", email);
            Log.i("Signed in UID", uid);
        }*/


        mAuth = FirebaseAuth.getInstance();
        findViewById(R.id.signup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getApplicationContext(), "Please Wait while we sign you up", Toast.LENGTH_SHORT).show();
                    String email = ((EditText) findViewById(R.id.emailField)).getText().toString();
                    String password = ((EditText) findViewById(R.id.passwordField)).getText().toString();
                    createAccount(email, password);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });

        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getApplicationContext(), "Please Wait while we log you in", Toast.LENGTH_SHORT).show();
                    String email = ((EditText) findViewById(R.id.emailField)).getText().toString();
                    String password = ((EditText) findViewById(R.id.passwordField)).getText().toString();
                    signin(email, password);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });


    }
}