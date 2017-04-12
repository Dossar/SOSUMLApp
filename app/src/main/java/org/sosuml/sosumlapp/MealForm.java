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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.sosuml.sosumlapp.SendEmail.*;
import static org.sosuml.sosumlapp.GraphicsUtilities.*;

/**
 * This class interacts with the Meal Form page that covers the Guest Meal Donation Request.
 *
 * @author Roy Van Liew
 */
public class MealForm extends AppCompatActivity {

    // Google authorization
    GoogleAccountCredential mCredential;

    // Our DB handler for the meal donation request
    private MealDbHandler db;

    // Activity page layout elements
    private TextView mealDescription;
    private Button btnDonate, btnRegister;

    // Strings we extract from the form
    private String inputName, inputEmail, accountName;
    private String SOSEMAIL = "test@gmail.com";

    // Other helping variables
    private boolean sendWasSuccessful;
    private int c_year, c_month, c_day;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REGISTER_ACTIVITY = 1004;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {GmailScopes.MAIL_GOOGLE_COM};

    /**
     * Create the Meal Form activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mealform);

        // Set HTML text
        mealDescription = (TextView)findViewById(R.id.mealFormDescription);
        mealDescription.setText(Html.fromHtml(getString(R.string.meal_form_description)));

        // First get an instance of our database.
        db = new MealDbHandler(this);

        // Get the button for configuring the user's email account.
        btnRegister = (Button) findViewById(R.id.mealFormBtnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegisterForm();
            }
        });

        // Acquire the button and textview from the layout file
        btnDonate = (Button) findViewById(R.id.mealFormBtnDonate);
        btnDonate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If the user is registered we can go ahead and try making the donation.
                if (isRegistered()) {

                    // Only continue with the donation if the user is able to
                    if (canUserDonate()) {
                        // Set the account name variable to the DB entry
                        // Along with input name and email
                        RegisterDbEntry register = db.getRegister(RegisterDbEntry.REGISTER_ID);
                        accountName = register.getEmail();
                        inputName = register.getName();
                        inputEmail = register.getStudentEmail();

                        // Get results from Google API
                        String toastInfo = "Attempting to send an e-mail for your guest meal " +
                                "donation. This may take a few seconds.";
                        Toast.makeText(MealForm.this, toastInfo,
                                Toast.LENGTH_SHORT).show();
                        getResultsFromApi();
                    } else {

                        // Getting here means the user was not able to make the donation.
                        // Check first if the current date was in the summer.
                        Calendar current_donation = new GregorianCalendar(TimeZone.getTimeZone("EST"));
                        int c_month = current_donation.get(Calendar.MONTH);
                        int c_day = current_donation.get(Calendar.DAY_OF_MONTH);

                        // Check what the current month is. If it's in the breaks, user cannot donate.
                        // If the current months are valid (during a semester) get most recent date.
                        if (c_month >= Calendar.MAY && c_month <= Calendar.AUGUST) {
                            displayAlert(MealForm.this,
                                    "Sorry, you can only request donations during a semester. "
                                            + " Please try again in the Fall Semester, Sep 1st - Dec 9th.",
                                    "Cannot Donate in Summer",
                                    "OK");
                        } else if ((c_month == Calendar.DECEMBER && c_day > 9) ||
                                (c_month == Calendar.JANUARY && c_day < 17)) {
                            displayAlert(MealForm.this,
                                    "Sorry, you can only request donations during a semester. "
                                            + " Please try again in the Spring Semester, Jan 17th - Apr 28th.",
                                    "Cannot Donate in Winter Break",
                                    "OK");
                        } else {

                            // Getting here means the request was during the semester.
                            // Make sure the user gets the information of their previous donation date.
                            MealDbDate previous_mealDbDonation = db.getMealDbDate(MealDbDate.MEAL_ID);
                            String p_month = Integer.toString(previous_mealDbDonation.getMonth() + 1);
                            String p_day = Integer.toString(previous_mealDbDonation.getDay());
                            String p_year = Integer.toString(previous_mealDbDonation.getYear());
                            displayAlert(MealForm.this,
                                    "You already requested a meal donation this semester on " +
                                            p_month + "/" + p_day + "/" + p_year +
                                            ". Please try again next semester.",
                                    "Already Donated This Semester",
                                    "OK");
                        }

                    }
                } else {
                    // This means the user is not registered.
                    displayAlert(MealForm.this,
                            "You must register before requesting a meal donation.",
                            "Not Registered",
                            "OK");
                }
            }
        });

        // Check to see if the user is already registered.
        if (isRegistered()) {
            btnRegister.setBackgroundColor(getResources().getColor(R.color.completed_green));
            btnRegister.setText("Registered " + Html.fromHtml("&#x2714;"));

            // In addition, check to see if the user can make a donation. If not,
            // Visually display that they have already sent a donation.
            if (canUserDonate() == false) {
                btnDonate.setBackgroundColor(getResources().getColor(R.color.completed_green));
                btnDonate.setText("Donated " + Html.fromHtml("&#x2714;"));

                String toastInfo = "You cannot send a request to donate a guest meal " +
                        "at this time. Please try again next semester.";
                Toast.makeText(MealForm.this, toastInfo,
                        Toast.LENGTH_LONG).show();
            }
        }

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

    }

    /**
     * Creates an intent to go to the Registration page.
     */
    private void goToRegisterForm() {
        Intent intent = new Intent(MealForm.this, RegisterForm.class);
        startActivityForResult(intent, REGISTER_ACTIVITY);
    }

    /**
     * Called when an activity launched here exits, giving you the requestCode you
     * started it with, the resultCode it returned, and any additional data from it.
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
            case REGISTER_ACTIVITY:
                // Check if the user has registration info now. NEED TO TEST THIS!
                if (isRegistered()) {
                    recreate();
                }
                break;
        }
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
     * Helper function to check if the user is able to request a meal donation.
     *
     * @return false if the user isn't able to donate at this time, true otherwise.
     */
    boolean canUserDonate() {

        // We have a private member variable for our previous donation.
        // We need to get the current time in EST.
        Calendar current_donation = new GregorianCalendar(TimeZone.getTimeZone("EST"));

        // Debug info
        c_year = current_donation.get(Calendar.YEAR);
        c_month = current_donation.get(Calendar.MONTH);
        c_day = current_donation.get(Calendar.DAY_OF_MONTH);
        Log.d("MealForm: ", "canUserDonate: CURRENT: Year: " + c_year);
        Log.d("MealForm: ", "canUserDonate: CURRENT: Month: " + c_month);
        Log.d("MealForm: ", "canUserDonate: CURRENT: Day: " + c_day);

        // Note that in the actual app we would just get the MealDbDate for the
        // previous donation. The Logging and String stuff is for debugging.
        MealDbDate previous_mealDbDonation = db.getMealDbDate(MealDbDate.MEAL_ID);
        Log.d("MealForm: ", "canUserDonate: PREVIOUS: Here is the most recent meal donation.");
        String p_month = Integer.toString(previous_mealDbDonation.getMonth());
        String p_day = Integer.toString(previous_mealDbDonation.getDay());
        String p_year = Integer.toString(previous_mealDbDonation.getYear());
        Log.d("MealForm: ", "canUserDonate: PREVIOUS: Year: " + p_year);
        Log.d("MealForm: ", "canUserDonate: PREVIOUS: Month: " + p_month);
        Log.d("MealForm: ", "canUserDonate: PREVIOUS: Day: " + p_day);

        // Before we can make a comparison, we need to make sure that we have a Calendar
        // Object for the previous donation.
        int p_month_int = previous_mealDbDonation.getMonth();
        int p_day_int = previous_mealDbDonation.getDay();
        int p_year_int = previous_mealDbDonation.getYear();
        Calendar previous_donation = new GregorianCalendar(p_year_int, p_month_int, p_day_int);

        // Now compare the two dates.
        if (isDonationDateValid(current_donation, previous_donation)) {
            Log.d("MealForm: ", "canUserDonate: User CAN make donation. Update with new date.");
            return true;
        } else {
            Log.d("MealForm: ", "canUserDonate: User CANNOT make donation. Previous date stays.");
            return false;
        }

    }

    /**
     * Helper function to determine if the user can make a meal donation request
     * by comparing the previous meal donation date and the current date.
     *
     * @param current The current date.
     * @param previous The date of the previous donation.
     *
     * @return false if user can't make donation, true if user can make donation.
     */
    boolean isDonationDateValid(Calendar current, Calendar previous) {

        // Initialization for the current time
        int month = current.get(Calendar.MONTH);
        int day = current.get(Calendar.DAY_OF_MONTH);
        int year = current.get(Calendar.YEAR);

        // Initialization for the most recent donation
        int prev_month = previous.get(Calendar.MONTH);
        int prev_day = previous.get(Calendar.DAY_OF_MONTH);
        int prev_year = previous.get(Calendar.YEAR);

        /* First series of checks will be the summer and specific
         * day boundaries.
         */

        // Months May-August is during the summer, so immediately invalid
        if (month >= Calendar.MAY && month <= Calendar.AUGUST) {
            return false;
        }

        // Cannot donate before January 17th. Spring window is Jan 17 - Apr 28
        if (month == Calendar.JANUARY && day < 17) {
            return false;
        }

        // Cannot donate after December 9th. Fall window is Sep 1 - Dec 9
        if (month == Calendar.DECEMBER && day > 9) {
            return false;
        }

        // Cannot donate after Apr 28th, leading into summer
        if (month == Calendar.APRIL && day > 28) {
            return false;
        }

        // It's guaranteed that a donation made last year is in a previous semester
        // We can do this because of specific day checks done above
        if (prev_year < year) {
            return true;
        }

        /* Already covered specific day checks at this point, so now we don't need
         * to explicitly check the day in the coming checks. In addition we know that
         * the year will be the same in these checks, since the case above covers the
         * case where the last donation is indeed from a previous semester if it was last year.
         */

        // If our current month is in the Fall, see if last donation was in Spring this year.
        if (month >= Calendar.SEPTEMBER && month <= Calendar.DECEMBER) {
            if (prev_month >= Calendar.JANUARY && prev_month <= Calendar.APRIL) {
                return true;
            } else {
                // Since we're in the same year, this means we donated in the Fall. Can't donate
                return false;
            }
        }

        // If our current month is in the Spring, the last donation was in same Spring Year.
        return false;

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
                MealForm.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
            return;
        } else if (!isDeviceOnline()) {
            displayAlert(MealForm.this,
                    "This device has no network connections",
                    "No Network Connections",
                    "OK");
            return;
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * An asynchronous task that handles the Gmail API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private Gmail mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            // Create the Gmail instance we'll use for sending the email
            mService = new Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Email Test")
                    .build();
        }

        /**
         * Background task to call Gmail API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            List<String> labels = new ArrayList<String>();
            labels.add("me");
            return labels;
        }

        /**
         * Attempts to create and send three e-mails.
         * 1.) E-mail to the user's selected e-mail account from registration page
         * 2.) E-mail to the user's UMass Lowell student e-mail from registration page
         * 3.) E-mail to SOS about student's registration information
         */
        private void createAndSendEmail() {

            sendWasSuccessful = false;

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {

                    try {

                        // Set the account name from registration for proper authentication.
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);

                        /* Intialize variables for the two emails to send, which include the one
                         * to SOS and the original email sender. */
                        String currentUser = mCredential.getSelectedAccountName();
                        MimeMessage emailToSendSos = null;
                        Message sentMessageSos = null;
                        MimeMessage emailToSendUser = null;
                        Message sentMessageUser = null;
                        MimeMessage emailToSendStudent = null;
                        Message sentMessageStudent = null;

                        /* The first email we will send is the one to the original user. */
                        /* Now build the Email Subject for email to user. */
                        String userSubject = "SOS App Guest Meal Donation Confirmation";

                        /* Now build the Email Body for email to user. */
                        String userBody = "Hello " + inputName + ",<br><br>";

                        // Body intro
                        userBody += "You are receiving this email as notification that you "
                                + "have requested a Meal Donation through the SOS app. "
                                + "Remember your registration information is:<br><br>";

                        // Body list
                        userBody += "Student Name: " + inputName + "<br>";
                        userBody += "Student E-mail: " + inputEmail + "<br><br>";

                        // Body Warning
                        userBody += "SOS will reply back to you through sosatuml@gmail.com "
                                + "for the next step in your meal donation. If you did not make this "
                                + "request, please indicate that you did not make the request "
                                + "when you receive the SOS response.<br><br>- The SOS Team";

                        // Body Image
                        userBody += "<br><br><img src="
                                + "\"http://www.sosuml.org/img/sos_logo_2016.png\">";

                        /* The second email we will create is the one to SOS. */
                        String sosSubject = "Guest Meal Donation Request";

                        String sosBody = "A student has requested to donate a Guest Meal "
                                + "through the SOS app. The user's registration is:<br><br>";
                        sosBody += "Student Name: " + inputName + "<br>";
                        sosBody += "Student E-mail: " + inputEmail + "<br><br>";
                        sosBody += "<img src=\"http://www.sosuml.org/img/sos_logo_2016.png\">";

                        // Now Create the emails for both the user and SOS
                        try {
                            emailToSendUser = createHtmlEmail(currentUser, currentUser,
                                    userSubject, userBody);
                            emailToSendSos = createHtmlEmail(SOSEMAIL, currentUser,
                                    sosSubject, sosBody);
                            emailToSendStudent = createHtmlEmail(inputEmail, currentUser,
                                    userSubject, userBody);
                        } catch (Exception e) {
                            mLastError = e;
                            Log.d("createAndSendEmail: ", "Exception when trying to " +
                                    "create e-mails: " + e.getMessage());
                        }

                        // Send the email to SOS, user's selected account, and student e-mail
                        try {
                            sentMessageUser = sendMessage(mService, currentUser, emailToSendUser);
                            sentMessageSos = sendMessage(mService, currentUser, emailToSendSos);
                            sentMessageStudent = sendMessage(mService, currentUser, emailToSendStudent);
                            sendWasSuccessful = true;
                            Log.d("createAndSendEmail: ", "Successfully sent all three e-mails");
                        } catch (MessagingException e) {
                            mLastError = e;
                            Log.d("createAndSendEmail: ", "MessagingException in sending: " + e.getMessage());
                        } catch (IOException e) {
                            mLastError = e;
                            if (mLastError instanceof UserRecoverableAuthIOException) {
                                Log.d("createAndSendEmail: ", "User needs to give permission for Gmail");
                                startActivityForResult(
                                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                                        REQUEST_AUTHORIZATION);
                            } else {
                                Log.d("createAndSendEmail: ", "IOException in sending: " + e.getMessage());
                            }
                        } catch (Exception e) {
                            Log.d("createAndSendEmail: ", "Exception in sending: " + e.getMessage());
                        }

                        /* Now check to see if the send was successful. If it was, update DB */
                        if (sendWasSuccessful) {

                            // Now that we know the sending was successful, update the entry
                            MealDbDate current_mealDbDonation = new MealDbDate(c_month, c_day, c_year);
                            int rowsAffected = db.updateMealDbDate(current_mealDbDonation);
                            Log.d("MealForm: ", "createAndSendEmail: "
                                    + Integer.toString(rowsAffected) + " rows updated.");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        btnDonate.setBackgroundColor(getResources().getColor(R.color.completed_green));
                                        btnDonate.setText("Donated " + Html.fromHtml("&#x2714;"));
                                        displayAlert(MealForm.this,
                                                "Successfully sent a request to donate one of "
                                                        + "your guest meals. Thank you and please "
                                                        + "check your email in a few days for the "
                                                        + "reply back from SOS!",
                                                "Sent Donation Request",
                                                "OK");
                                    } catch (Exception e) {
                                        Log.d("createAndSendEmail: ", "Exception: " + e.getMessage());
                                    }
                                }
                            });
                        }

                    } catch (Exception e) {
                        Log.d("createAndSendEmail: ", "Exception: " + e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    displayAlert(MealForm.this,
                                            "Sorry, something went wrong trying to send your "
                                                    + "request. Please try again.",
                                            "Request Failed",
                                            "OK");
                                } catch (Exception e) {
                                    Log.d("createAndSendEmail: ", "Exception in Runnable: " + e.getMessage());
                                }
                            }
                        });
                    }
                }
            });
            thread.start();

        }

        @Override
        protected void onPreExecute() {
            createAndSendEmail();
        }

        @Override
        protected void onPostExecute(List<String> output) {

        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Log.d("onCancelled: ", "Exception: " + mLastError.getMessage());
                }
            } else {
                Log.d("onCancelled: ", "Request cancelled.");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mealform, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.mealFormMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}