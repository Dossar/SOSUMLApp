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
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.sosuml.sosumlapp.SendEmail.*;
import static org.sosuml.sosumlapp.GraphicsUtilities.*;

/**
 * This class interacts with the Event Info page that displays more information
 * about a Google Calendar Event from SOSUML. On this page, the user can
 * request to sign up for an event or add it to his/her calendar.
 *
 * @author Roy Van Liew
 */
public class EventInfo extends AppCompatActivity {

    private String title, time, location, description, rfc3339time, rfc3339end;
    private TextView txtTitle, txtTime, txtLocation, txtDesc;
    private Button btnSignup, btnAddToCalendar;

    // Our DB handler for the meal donation request
    private MealDbHandler db;

    // Strings we'd get from the Registration
    private String inputName, inputEmail, accountName;
    private String SOSEMAIL = "test@gmail.com";

    // Google authorization
    GoogleAccountCredential mCredential;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    // The progress dialog when attempting to send the e-mail
    private boolean sendWasSuccessful;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {GmailScopes.MAIL_GOOGLE_COM,
            CalendarScopes.CALENDAR};
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventinfo);

        // Get the event information passed through four variables.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            title = extras.getString("title", "None");
            time = extras.getString("time", "None");
            location = extras.getString("location", "None");
            description = extras.getString("description", "None");
            rfc3339time = extras.getString("rfc3339time", "None");
            rfc3339end = extras.getString("rfc3339end", "None");
        } else {
            finish(); // No point in continuing if we don't have values
        }

        // First get an instance of our database.
        db = new MealDbHandler(this);

        // Find the Buttons in the page.
        btnSignup = (Button) findViewById(R.id.eventInfoBtnSignup);
        btnAddToCalendar = (Button) findViewById(R.id.eventInfoBtnAddToCalendar);

        // Check if the user is already signed up. If they are, disable the Signup button.
        if (isSignedUpForEvent(title, time)) {
            btnSignup.setBackgroundColor(getResources().getColor(R.color.completed_green));
            btnSignup.setEnabled(false);
            btnSignup.setText("Signed Up " + Html.fromHtml("&#x2714;"));
            String toastInfo = "You have already signed up for this event.";
            Toast.makeText(EventInfo.this, toastInfo,
                    Toast.LENGTH_SHORT).show();
        }

        // Get the references to the TextView widgets that display the event info.
        txtTitle = (TextView) findViewById(R.id.eventInfoTitle);
        txtTime = (TextView) findViewById(R.id.eventInfoTime);
        txtLocation = (TextView) findViewById(R.id.eventInfoLocation);
        txtDesc = (TextView) findViewById(R.id.eventInfoDesc);

        // Set the text displayed in those TextViews to include the event info.
        txtTitle.setText(title);
        txtTime.setText(time);
        txtLocation.setText(location);
        txtDesc.setText(description);

        // Signup button: Calls the e-mail API
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                // Note we also assume that the user is registered here.
                // Set the account name variable to the DB entry, input name, and email
                RegisterDbEntry register = db.getRegister(RegisterDbEntry.REGISTER_ID);
                accountName = register.getEmail();
                inputName = register.getName();
                inputEmail = register.getStudentEmail();

                // Also realize that this button is only enabled if the user hasn't signed up yet.
                String toastInfo = "Attempting to sign up. This may take a few seconds.";
                Toast.makeText(EventInfo.this, toastInfo,
                        Toast.LENGTH_SHORT).show();
                getResultsFromApi();
            }
        });

        // Add to Calendar button: Planning on making this add the event to android calendar
        btnAddToCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                // Note we also assume that the user is registered here.
                // Set the account name variable to the DB entry, input name, and email
                RegisterDbEntry register = db.getRegister(RegisterDbEntry.REGISTER_ID);
                accountName = register.getEmail();
                inputName = register.getName();
                inputEmail = register.getStudentEmail();

                String toastInfo = "Attempting to add event to your calendar. This may take a few seconds.";
                Toast.makeText(EventInfo.this, toastInfo,
                        Toast.LENGTH_SHORT).show();
                addEvent();
            }
        });

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

    }

    /**
     * Helper function to determine if the user has already signed up for the event.
     *
     * @param eventTitle The title of the event as a string. Make sure it
     *                   does NOT have double quotes!
     * @param eventTime The time that the event is occurring as a string.
     *
     * @return false if the user does not have an entry for this event
     *         in the DB, true otherwise.
     */
    private boolean isSignedUpForEvent(String eventTitle, String eventTime) {
        CalSignupDbEntry calEntry = null;
        Log.d("isSignedUp: ", "Checking if the event already has a signup entry.");
        try {
            calEntry = db.getCalSignByDetails(eventTitle, eventTime);
        } catch (Exception e) {
            // Do nothing, this just means we don't have the entry in the DB
            Log.d("isSignedUp: ", "Exception: " + e.getMessage());
        }

        if (calEntry == null) {
            Log.d("isSignedUp: ", "User does not have a signup for " + eventTitle);
            return false;
        } else {
            Log.d("isSignedUp: ", "User already has a signup for " + eventTitle);
            return true;
        }
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
                EventInfo.this,
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
            displayAlert(EventInfo.this,
                    "This device has no network connections",
                    "No Network Connections",
                    "OK");
            return;
        } else {
            btnSignup.setEnabled(false); // Disable signup button before task executes
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Call for making the ASyncTask that will add an event to the user's calendar.
     */
    private void addEvent() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
            return;
        } else if (!isDeviceOnline()) {
            displayAlert(EventInfo.this,
                    "This device has no network connections",
                    "No Network Connections",
                    "OK");
            return;
        } else {
            // Set the account name from registration for proper authentication.
            SharedPreferences settings =
                    getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(PREF_ACCOUNT_NAME, accountName);
            editor.apply();
            mCredential.setSelectedAccountName(accountName);
            Log.d("addEvent: ", "accountName is " + accountName);
            btnAddToCalendar.setEnabled(false); // Disable calendar add button before task executes
            new CalendarAddTask(mCredential).execute();
        }
    }

    /**
     * An asynchronous task that handles the Gmail API call for signing up.
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
                    .setApplicationName("Event Signup")
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
         * Attempt to create and send an email.
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

                        /* The first email we will send is the one to the original user. */

                        /* Now build the Email Subject for email to user. */
                        String userSubject = "SOS Event Signup: " + title;

                        /* Now build the Email Body for email to user. */
                        String userBody = "Hello " + inputName + ",<br><br>";

                        // Body intro
                        userBody += "You are receiving this email as notification that you "
                                + "have requested to sign up, through the SOS app, for "
                                + "the following event:<br><br>"
                                + "<b>Title:</b> " + title + "<br>"
                                + "<b>Location:</b> " + location + "<br>"
                                + "<b>Time:</b> " + time + "<br>"
                                + "<b>Description:</b> " + description + "<br><br>";

                        // Body list
                        userBody += "Your registration information is:<br>";
                        userBody += "Student Name: " + inputName + "<br>";
                        userBody += "Student E-mail: " + inputEmail + "<br><br>";

                        // Body Image
                        userBody += "- The SOS Team<br><br><img src=";
                        userBody += "\"http://www.sosuml.org/img/sos_logo_2016.png\">";

                        // Create the e-mail for the user
                        try {
                            emailToSendUser = createHtmlEmail(currentUser, currentUser,
                                    userSubject, userBody);
                        } catch (Exception e) {
                            mLastError = e;
                            Log.d("createAndSendEmail: ", "Exception: " + e.getMessage());
                        }

                        // Send the e-mail to the user
                        try {
                            sentMessageUser = sendMessage(mService, currentUser, emailToSendUser);
                            sendWasSuccessful = true;
                        } catch (MessagingException e) {
                            mLastError = e;
                            Log.d("createAndSendEmail: ", "MessagingException: " + e.getMessage());
                        } catch (IOException e) {
                            mLastError = e;
                            if (mLastError instanceof UserRecoverableAuthIOException) {
                                Log.d("createAndSendEmail: ", "User needs to give permission for Gmail");
                                startActivityForResult(
                                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                                        REQUEST_AUTHORIZATION);
                            } else {
                                Log.d("createAndSendEmail: ", "IOException: " + e.getMessage());
                            }
                        }

                        /* The second email we will send is the one to SOS. */
                        String sosSubject = "Student Event Signup: " + title;

                        // Body intro
                        String sosBody = "A student has requested to sign up for the following "
                                + "event through the SOS app:<br><br>"
                                + "<b>Title:</b> " + title + "<br>"
                                + "<b>Location:</b> " + location + "<br>"
                                + "<b>Time:</b> " + time + "<br>"
                                + "<b>Description:</b> " + description + "<br><br>";

                        // Body list
                        sosBody += "The user's registration is:<br>";
                        sosBody += "Student Name: " + inputName + "<br>";
                        sosBody += "Student E-mail: " + inputEmail + "<br><br>";
                        sosBody += "<img src=\"http://www.sosuml.org/img/sos_logo_2016.png\">";

                        // Create the e-mail for the user
                        try {
                            emailToSendSos = createHtmlEmail(SOSEMAIL, currentUser,
                                    sosSubject, sosBody);
                        } catch (Exception e) {
                            mLastError = e;
                            Log.d("createAndSendEmail: ", "Exception: " + e.getMessage());
                        }

                        // Send the e-mail to SOS
                        try {
                            sentMessageSos = sendMessage(mService, currentUser, emailToSendSos);
                            sendWasSuccessful = true;
                        } catch (MessagingException e) {
                            mLastError = e;
                            Log.d("createAndSendEmail: ", "MessagingException: " + e.getMessage());
                        } catch (IOException e) {
                            mLastError = e;
                            if (mLastError instanceof UserRecoverableAuthIOException) {
                                Log.d("createAndSendEmail: ", "User needs to give permission for Gmail");
                                startActivityForResult(
                                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                                        REQUEST_AUTHORIZATION);
                            } else {
                                Log.d("createAndSendEmail: ", "IOException: " + e.getMessage());
                            }
                        }

                        /* If the signup was successful, update the DB entry. */
                        if (sendWasSuccessful) {

                            // Now we can add the Calendar Signup Entry
                            CalSignupDbEntry newSignup = new CalSignupDbEntry(title, time);
                            db.addCalSign(newSignup);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {

                                        displayAlert(EventInfo.this,
                                                "You have signed up for this event. An e-mail " +
                                                        "has been sent to you with the event's " +
                                                        "details as well.",
                                                "Signed Up",
                                                "OK");

                                        // Also be sure to disable the signup button after
                                        btnSignup.setBackgroundColor(getResources().getColor(R.color.completed_green));
                                        btnSignup.setEnabled(false);
                                        btnSignup.setText("Signed Up " + Html.fromHtml("&#x2714;"));
                                    } catch (Exception e) {
                                        Log.d("createAndSendEmail: ", "Exception: " + e.getMessage());
                                    }
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        btnSignup.setEnabled(true); // Re-enable signup button upon failure
                                    } catch (Exception e) {
                                        Log.d("createAndSendEmail: ", "Exception: " + e.getMessage());
                                    }
                                }
                            });
                        }

                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    btnSignup.setEnabled(true); // Re-enable signup button upon failure
                                    displayAlert(EventInfo.this,
                                            "Sorry, something went wrong trying to send your "
                                                    + "request. Please try again.",
                                            "Request Failed",
                                            "OK");
                                } catch (Exception e) {
                                    Log.d("createAndSendEmail: ", "Exception: " + e.getMessage());
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
            btnSignup.setEnabled(true); // Re-enable signup button upon failure
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

    /**
     * An asynchronous task that handles the Google Calendar API call to
     * add an event to the user's calendar.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class CalendarAddTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar calendarService = null;
        private Exception mLastError = null;

        public CalendarAddTask(GoogleAccountCredential credential) {

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            calendarService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Event Listing")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                Log.d("doInBackground: ", "Calling addCalendarEvent()");
                return addCalendarEvent();
            } catch (Exception e) {
                mLastError = e;
                Log.d("getDataFromApi: ", "Exception: " + e.getMessage());
                cancel(true);
                return null;
            }
        }

        /**
         * Quick Add an event to the user's calendar.
         * @return List of Strings describing returned events.
         */
        private List<String> addCalendarEvent() {
            try {

                Log.d("addCalendarEvent: ", "Setting event title, location, and description");
                Event event = new Event()
                        .setSummary(title)
                        .setLocation(location)
                        .setDescription(description);

                // EST Time Zone, start time
                Log.d("addCalendarEvent: ", "Setting event time");
                DateTime startDateTime = null;
                EventDateTime start = null;
                try {
                    if (rfc3339time.contains("T")) {
                        // In this case: There is an explicit start time
                        startDateTime = new DateTime(rfc3339time);
                        start = new EventDateTime()
                                .setDateTime(startDateTime)
                                .setTimeZone("America/New_York");
                        event.setStart(start);
                    } else {
                        startDateTime = new DateTime(rfc3339time);
                        start = new EventDateTime()
                                .setDate(startDateTime);
                        event.setStart(start);
                    }
                } catch (Exception e) {
                    Log.d("addCalendarEvent: ", e.getMessage());
                }

                // EST Time Zone, end time
                DateTime endDateTime = null;
                EventDateTime end = null;
                try {
                    if (rfc3339end.contains("T")) {
                        endDateTime = new DateTime(rfc3339end);
                        end = new EventDateTime()
                                .setDateTime(endDateTime)
                                .setTimeZone("America/New_York");
                        event.setEnd(end);
                    } else {
                        endDateTime = new DateTime(rfc3339end);
                        end = new EventDateTime()
                                .setDate(endDateTime);
                        event.setEnd(end);
                    }
                } catch (Exception e) {
                    Log.d("addCalendarEvent: ", e.getMessage());
                }

                // Event Reminder. Email one day before, popup 2 hours before.
                Log.d("addCalendarEvent: ", "Setting event reminder");
                EventReminder[] reminderOverrides = new EventReminder[] {
                        new EventReminder().setMethod("email").setMinutes(24 * 60),
                        new EventReminder().setMethod("popup").setMinutes(2 * 60),
                };
                Event.Reminders reminders = new Event.Reminders()
                        .setUseDefault(false)
                        .setOverrides(Arrays.asList(reminderOverrides));
                event.setReminders(reminders);

                // Set on user's current calendar
                Log.d("addCalendarEvent: ", "Adding to Calendar");
                String calendarId = mCredential.getSelectedAccountName();
                event = calendarService.events().insert(calendarId, event).execute();
                Log.d("addCalendarEvent: ", "Event created: " + event.getHtmlLink());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            btnAddToCalendar.setEnabled(true); // Re-enable calendar add button upon success
                            displayAlert(EventInfo.this,
                                    "Successfully Added Event to your Google Calendar!",
                                    "Added Event",
                                    "OK");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException e) {
                mLastError = e;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            btnAddToCalendar.setEnabled(true); // Re-enable calendar add button upon failure
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                if (mLastError instanceof UserRecoverableAuthIOException) {
                    Log.d("addCalendarEvent: ", "User needs to give permission for Calendar");
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Log.d("addCalendarEvent: ", "IOException: " + e.getMessage());
                }
            } catch (Exception e) {
                Log.d("addCalendarEvent: ", "Exception: " + e.getMessage());
                mLastError = e;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            btnAddToCalendar.setEnabled(true); // Re-enable calendar add button upon failure
                            displayAlert(EventInfo.this,
                                    "Sorry, something went wrong trying to send your "
                                            + "request. Please try again.",
                                    "Request Failed",
                                    "OK");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            return null;
        }


        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(List<String> output) {
            if (output == null || output.size() == 0) {
                // Do nothing
                Log.d("EventInfo: ", "No output");
            } else {
                // Do nothing
            }
        }

        @Override
        protected void onCancelled() {
            btnAddToCalendar.setEnabled(true); // Re-enable calendar add button upon failure
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
        getMenuInflater().inflate(R.menu.menu_eventinfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.eventInfoMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
