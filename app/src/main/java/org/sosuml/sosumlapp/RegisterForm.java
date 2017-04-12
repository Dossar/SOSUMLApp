/*
 *  Copyright 2017 Roy Van Liew
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.sosuml.sosumlapp;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.AfterPermissionGranted;
import org.sosuml.sosumlapp.EasyPermissionsDialog;

import org.sosuml.sosumlapp.SendEmail.*;
import static org.sosuml.sosumlapp.GraphicsUtilities.*;

/**
 * This class interacts with the Register Form page that allows the user to
 * configure their registration information for the SOSUML app.
 *
 * @author Roy Van Liew
 */
public class RegisterForm extends AppCompatActivity
        implements EasyPermissionsDialog.PermissionCallbacks {

    // Google authorization
    GoogleAccountCredential mCredential;

    // Our DB handler for the meal donation request
    private MealDbHandler db;

    // Activity page layout elements
    private Button btnConfig, btnRegister, btnCheck;

    // Form elements to get data from
    private EditText txtName, txtEmail;

    // Strings we extract from the form
    private String inputName, inputEmail;

    // The selected e-mail account name to save into the database
    private String accountName;

    // Activity codes
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {GmailScopes.MAIL_GOOGLE_COM};

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registerform);

        // First get an instance of our database.
        db = new MealDbHandler(this);

        // Get the form widget fields
        txtName = (EditText) findViewById(R.id.registerFormTxtName);
        txtEmail = (EditText) findViewById(R.id.registerFormTxtEmail);

        // Get the buttons at the start so we can do some preliminary checks
        btnConfig = (Button) findViewById(R.id.registerFormBtnSelectEmail);
        btnRegister = (Button) findViewById(R.id.registerFormBtnRegister);
        btnCheck = (Button) findViewById(R.id.registerFormBtnCheck);

        // Check first to see if the user already has an account
        if (isRegistered()) {
            btnCheck.setBackgroundColor(getResources().getColor(R.color.completed_green));
            btnCheck.setText("I Am Registered " + Html.fromHtml("&#x2714;"));
            displayAlert(RegisterForm.this,
                    "You are registered for this app. You can check your registration "
                    + "info in Step 3 at the bottom of this page, and you can change your "
                    + "registration info at any time.",
                    "Already Registered",
                    "OK");
        }

        // Button for selecting the user's e-mail account
        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configureEmailAccount();
            }
        });

        // Get the button for registering the user's information.
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Does nothing for now
                if(hasNoEmptyFields()) {
                    if(accountName != null) {
                        Log.d("RegisterForm: ", "register: form was filled correctly.");
                        // Call a function here that will add a DB entry
                        RegisterDbEntry register = new RegisterDbEntry(inputName, inputEmail,
                                accountName);
                        if (isRegistered()) {
                            // This means the user is registered. Update the current DB entry.
                            int rowsAffected = db.updateRegister(register);
                            Log.d("RegisterForm: ", "register: " +
                                    Integer.toString(rowsAffected) + " rows updated.");
                            btnRegister.setBackgroundColor(getResources().getColor(R.color.completed_green));
                            btnRegister.setText("Register " + Html.fromHtml("&#x2714;"));
                            String toastSuccess = "Successfully updated your Registration!";
                            Toast.makeText(RegisterForm.this, toastSuccess,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // This means the user is not registered. Add a new DB entry.
                            db.addRegister(register);
                            btnRegister.setBackgroundColor(getResources().getColor(R.color.completed_green));
                            btnRegister.setText("Register " + Html.fromHtml("&#x2714;"));
                            btnCheck.setBackgroundColor(getResources().getColor(R.color.completed_green));
                            btnCheck.setText("I Am Registered " + Html.fromHtml("&#x2714;"));
                            String toastSuccess = "Successfully Registered!";
                            Toast.makeText(RegisterForm.this, toastSuccess,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        displayAlert(RegisterForm.this,
                                "You need to select an e-mail account.",
                                "Good Input",
                                "OK");
                    }
                }
            }
        });

        // Get the button for checking if the user is registered.

        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (isRegistered()) {
                    RegisterDbEntry register = db.getRegister(RegisterDbEntry.REGISTER_ID);
                    String r_name = register.getName();
                    String r_se = register.getStudentEmail();
                    String r_email = register.getEmail();
                    displayAlert(RegisterForm.this,
                            "You are registered for this app. Your info is:"
                            + "\nName: " + r_name
                            + "\nStudent Email: " + r_se
                            + "\nEmail: " + r_email,
                            "Registered",
                            "OK");
                } else {
                    displayAlert(RegisterForm.this,
                            "You are NOT registered for this app.",
                            "Not Registered",
                            "OK");
                }
            }
        });

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

    }

    /**
     * Helper function to determine if the user has registered or not.
     *
     * @return false if the user does not have a register entry in the DB, true otherwise.
     */
    boolean isRegistered() {
        RegisterDbEntry register = null;
        try {
            register = db.getRegister(RegisterDbEntry.REGISTER_ID);
        } catch (Exception e) {
            // do nothing, this just means we don't have the entry in the DB
        }

        // Check if the entry exists or not
        if (register == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Private function to make sure that the user has filled out the form fields.
     *
     * @return true if the data is valid, false if any data is missing
     */
    private boolean hasNoEmptyFields() {
        String name, email;
        boolean emptyInput = false;

        // Check if the inputs are empty
        name = txtName.getText().toString();
        emptyInput |= name.isEmpty();
        if (emptyInput) {
            displayAlert(RegisterForm.this,
                    "Cannot have empty name.",
                    "Empty Name",
                    "OK");
            return false;
        }

        email = txtEmail.getText().toString();
        emptyInput |= email.isEmpty();
        if (emptyInput) {
            displayAlert(RegisterForm.this,
                    "Cannot have empty email.",
                    "Empty Email",
                    "OK");
            return false;
        }

        // Now we need to check if the email is a UMass Lowell student email.
        String emailPattern = "^[A-Z0-9+_.-]+@(student\\.uml\\.edu)$";
        Pattern compiledPattern = Pattern.compile(emailPattern, Pattern.CASE_INSENSITIVE);
        Matcher matched = compiledPattern.matcher(email);
        if (matched.matches()) {

            // The email entered ends in student.uml.edu
            // We have valid information, so update our member variables
            inputName = name;
            inputEmail = email;
            return true;
        } else {
            displayAlert(RegisterForm.this,
                    "Please make sure the email is a UMass Lowell student email.",
                    "Need Student Email",
                    "OK");
            return false;
        }

    }

    /**
     * Let the user configure an email account.
     */
    private void configureEmailAccount() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
            return;
        } else if (!isDeviceOnline()) {
            displayAlert(RegisterForm.this,
                    "This device has no network connections",
                    "No Network Connections",
                    "OK");
            return;
        }

        chooseAccount();
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissionsDialog.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {

            // Start a dialog from which the user can choose an account
            startActivityForResult(
                    mCredential.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);

        } else {

            // Before showing the dialog, show a long toast stating the user will
            // select their email account.
            String toastSelectAccount = "This app will only use your email account to send " +
                    "the appropriate emails.";
            Toast.makeText(RegisterForm.this, toastSelectAccount,
                    Toast.LENGTH_LONG).show();

            // Request the GET_ACCOUNTS permission via a user dialog
            String dialogExplanation = "The SOS app uses the Gmail API, which needs to access " +
                    "Google Contacts to specify the email account you'll use to send the email.";
            EasyPermissionsDialog.requestPermissions(
                    this,
                    dialogExplanation,
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    // Do nothing.
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        Log.d("REQ_ACCOUNT_PICKER: ", "Selected e-mail " + accountName);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        btnConfig.setBackgroundColor(getResources().getColor(R.color.completed_green));
                        btnConfig.setText("Selected Email Account " + Html.fromHtml("&#x2714;"));
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {

                    // This case means the user has authorized their google account for this app.
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissionsDialog.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                RegisterForm.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_registerform, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.registerFormMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}