package com.iridium.mpesastkpush.main;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.iridium.mpesastkpush.utility.Constants;
import com.iridium.mpesastkpush.R;

import org.json.JSONException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;

public class MainActivity extends AppCompatActivity implements ProviderInstaller.ProviderInstallListener
{

    private Button mpesaBtn;
    private Mpesa mpesaObject;
    private String passkey = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919";
    private String timeStamp = null;
    private  String passwordSalt = null;
    private String password = null;
    private String shortCode = "174379";
    private String accountRef = "CAS884BG75G5";
    private String transDesc = "Dynamic Capital loan facilitation charges payment";
    private final String TAG  = "MPESA_ACTIVITY";

    private static final int ERROR_DIALOG_REQUEST_CODE = 1;

    private boolean retryProviderInstall;
    private SSLContext sslContext = null;

    private String phoneNumber = "254729893875";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ProviderInstaller.installIfNeededAsync(this, this);

                try
                {
                    sslContext = SSLContext.getInstance("TLSv1.2");
                } catch (NoSuchAlgorithmException e)
                {
                    e.printStackTrace();
                }
                try
                {
                    sslContext.init(null, null, null);
                } catch (KeyManagementException e)
                {
                    e.printStackTrace();
                }
                SSLEngine engine = sslContext.createSSLEngine();
                try
                {
                    engine.setUseClientMode(true);
                    engine.beginHandshake();
                } catch (SSLException e)
                {
                    e.printStackTrace();
                }

        timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        passwordSalt = shortCode+passkey+timeStamp;
        password = Base64.encodeToString(passwordSalt.getBytes(), Base64.NO_WRAP);


        mpesaObject = new Mpesa(Constants.APP_KEY,Constants.APP_SECRET);;
        mpesaBtn = findViewById(R.id.toMpesaBtn);
        mpesaBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new AsyncCaller().execute();
            }
        });

    }

    /**
     *
     * START OF SSL CONFIGURATIONS
     *
     */
    @Override
    public void onProviderInstalled()
    {

    }

    /**
     * This method is called if updating fails; the error code indicates
     * whether the error is recoverable.
     */
    @Override
    public void onProviderInstallFailed(int errorCode, Intent recoveryIntent)
    {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        if (availability.isUserResolvableError(errorCode))
        {
            // Recoverable error. Show a dialog prompting the user to
            // install/update/enable Google Play services.
            availability.showErrorDialogFragment(
                    this,
                    errorCode,
                    ERROR_DIALOG_REQUEST_CODE,
                    new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                            // The user chose not to take the recovery action
                            onProviderInstallerNotAvailable();
                        }
                    });
        } else {
            // Google Play services is not available.
            onProviderInstallerNotAvailable();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ERROR_DIALOG_REQUEST_CODE)
        {
            // Adding a fragment via GoogleApiAvailability.showErrorDialogFragment
            // before the instance state is restored throws an error. So instead,
            // set a flag here, which will cause the fragment to delay until
            // onPostResume.
            retryProviderInstall = true;
        }
    }

    /**
     * On resume, check to see if we flagged that we need to reinstall the
     * provider.
     */
    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        if (retryProviderInstall)
        {
            // We can now safely retry installation.
            ProviderInstaller.installIfNeededAsync(this, this);
        }
        retryProviderInstall = false;
    }

    private void onProviderInstallerNotAvailable()
    {
        // This is reached if the provider cannot be updated for some reason.
        // App should consider all HTTP communication to be vulnerable, and take
        // appropriate action.
    }


    /**
     *
     * END OF SSL CONFIGURATIONS
     *
     */


    /**
     * INNER CLASS RUNNING SEPARATE THREAD
     * EXECUTING TRANSACTION
     */
    private class AsyncCaller extends AsyncTask<Void, Void, Void>
    {

            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
            }
            @Override
            protected Void doInBackground(Void... params)
            {
                try {
                 //   mpesaObject.authenticate();
                    mpesaObject.STKPushSimulation("174379",
                            password,
                            timeStamp,
                            "CustomerBuyGoodsOnline",
                            "1",
                            phoneNumber,
                            phoneNumber,
                            "174379",
                            "http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation",
                            "http://obscure-bayou-52273.herokuapp.com/api/Mpesa/C2BValidation",
                            accountRef,
                            transDesc);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                } catch (JSONException e)
                {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                super.onPostExecute(result);

                //this method will be running on UI thread
            }
    }
}
