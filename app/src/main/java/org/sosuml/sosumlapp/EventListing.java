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
import android.widget.ImageButton;
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
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.sosuml.sosumlapp.GraphicsUtilities.*;

/**
 * This class interacts with the Event Listing page where the Google Calendar API is
 * used to fetch upcoming events from a public Google Calendar. In this case, we are
 * trying to fetch upcoming events from a public Google Calendar from SOSUML.
 *
 * @author Roy Van Liew
 */
public class EventListing extends AppCompatActivity {
    GoogleAccountCredential googleCredential;
    private ImageButton btnRefreshListing;
    private TextView eventListingHelp;

    // Five Buttons inside the grid
    private final String NOEVENT = "No Event";
    private final int NUMBUTTONS = 10;
    private List<Button> btnList;

    // Our DB handler for the meal donation request
    private MealDbHandler db;

    // User's registration configured email
    private String configuredEmail;
    private String PUBLICCALENDAR = "test@gmail.com";

    // The four pieces of information we store about the events, to pass to the intent.
    private List<String> eventTitles;
    private List<String> eventTimes;
    private List<String> eventLocations;
    private List<String> eventDescriptions;

    // The complete time information for each event needed for the DateTime object
    // RFC 3339 standard, e.g. 1996-12-19T16:39:57-08:00
    private List<String> eventTimesRfc3339;
    private List<String> endTimesRfc3339;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventlisting);

        // Initialize credentials and service object.
        googleCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        // Description for the user to help them with the listed events.
        eventListingHelp = (TextView)findViewById(R.id.eventListingHelp);
        eventListingHelp.setText(Html.fromHtml(getString(R.string.event_listing_description)));

        // We assume the user is already registered, get their selected account name from db
        db = new MealDbHandler(this); // First get an instance of our database.
        RegisterDbEntry register = db.getRegister(RegisterDbEntry.REGISTER_ID);
        configuredEmail = register.getEmail();
        Log.d("EventListing: ", "Account name is '" + configuredEmail + "'");

        // Set the account name from registration for proper authentication.
        SharedPreferences settings =
                getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NAME, configuredEmail);
        editor.apply();
        googleCredential.setSelectedAccountName(configuredEmail);

        // Get the buttons in the grid
        btnList = new ArrayList<Button>();
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid0));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid1));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid2));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid3));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid4));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid5));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid6));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid7));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid8));
        btnList.add((Button)findViewById(R.id.eventListingBtnGrid9));

        // Add Callback Listeners for the buttons
        for (int i = 0; i < NUMBUTTONS; i++) {
            final int index = i;
            btnList.get(i).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    goToEventByIndex(index);
                }
            });
        }

        // Call API Button
        btnRefreshListing = (ImageButton)findViewById(R.id.eventListingBtnRefresh);
        btnRefreshListing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("EventListing: ", "REFRESH: Attempting Refresh for Calendar Listing.");
                btnRefreshListing.setEnabled(false);

                // Disable all 5 buttons first.
                disableEventButtons();
                getResultsFromApi();
                btnRefreshListing.setEnabled(true);
            }
        });

        // Initialize List of event info Strings
        eventTitles = new ArrayList<String>();
        eventTimes = new ArrayList<String>();
        eventLocations = new ArrayList<String>();
        eventDescriptions = new ArrayList<String>();
        eventTimesRfc3339 = new ArrayList<String>();
        endTimesRfc3339 = new ArrayList<String>();

        // Initialize everything to "None" strings, five of them since we have five results
        String placeholder = "None";
        for (int i = 0; i < NUMBUTTONS; i++) {
            eventTitles.add(placeholder);
            eventTimes.add(placeholder);
            eventLocations.add(placeholder);
            eventDescriptions.add(placeholder);
            eventTimesRfc3339.add(placeholder);
            endTimesRfc3339.add(placeholder);
        }

        try {
            // Disable all 5 buttons first.
            disableEventButtons();
            getResultsFromApi(); // Automatically get results. User can refresh later

        } catch (Exception e) {
            Log.d("EventListing: ", "Automatic Calendar Refresh on page enter failed because "
                    + e.getMessage());
        }
    }

    /**
     * Disables the calendar result buttons. This is used in the case where the results
     * are being fetched and they weren't retrieved.
     */
    private void disableEventButtons() {
        for (int i = 0; i < NUMBUTTONS; i++) {
            btnList.get(i).setEnabled(false);
        }
    }

    /**
     * Re-enables the calendar result buttons. This is to be used after the Calendar
     * fetching task is done.
     */
    private void enableEventButtons() {
        for (int i = 0; i < NUMBUTTONS; i++) {
            btnList.get(i).setEnabled(true);
        }
        String toastSuccess = "Successfully got events. Click any result to get more info.";
        Toast.makeText(EventListing.this, toastSuccess,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * The information for each Calendar Event result is stored in order by index.
     * This provides an easy way to pass the data to another page; in our case,
     * going to the Event Info page that displays more information about one
     * event and allowing signup or adding to the user's calendar.
     *
     * @param index Which calendar result info we want to display in the
     *              Event Info page.
     */
    private void goToEventByIndex(int index) {

        // First check to see if there was no event.
        if (eventTitles.get(index).equals(NOEVENT)) {
            displayAlert(EventListing.this,
                    "Any result that says '" + NOEVENT + "' means there were no further results.",
                    NOEVENT,
                    "OK");
            return;
        }

        // Create the intent to transfer control over to the event info page.
        Intent intent = new Intent(EventListing.this, EventInfo.class);

        // Make sure we pass the correct values (by index) over as well.
        intent.putExtra("title", eventTitles.get(index));
        intent.putExtra("time", eventTimes.get(index));
        intent.putExtra("location", eventLocations.get(index));
        intent.putExtra("description", eventDescriptions.get(index));
        intent.putExtra("rfc3339time", eventTimesRfc3339.get(index));
        intent.putExtra("rfc3339end", endTimesRfc3339.get(index));
        this.startActivity(intent);
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (! isDeviceOnline()) {
            String toastSuccess = "No network connection available.";
            Toast.makeText(EventListing.this, toastSuccess,
                    Toast.LENGTH_LONG).show();
        } else {
            Log.d("getResultsFromApi: ", "Executing MakeRequestTask");
            new MakeRequestTask(googleCredential).execute();
        }
    }

    /**
     * Checks whether the device currently has a network connection.
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
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
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
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                EventListing.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar calendarService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {

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
                Log.d("doInBackground: ", "Calling getDataFromApi()");
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                Log.d("getDataFromApi: ", "Exception: " + e.getMessage());
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the upcoming 10 events from the primary calendar. This
         * function retrieves the events and parses the information from them.
         *
         * @return List of Strings describing returned events.
         */
        private List<String> getDataFromApi() {
            // List the next 10 events from the primary calendar.
            try {
                DateTime now = new DateTime(System.currentTimeMillis());
                List<String> eventStrings = new ArrayList<String>();
                Events events = calendarService.events().list(PUBLICCALENDAR)
                        .setMaxResults(NUMBUTTONS)
                        .setTimeMin(now)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute();
                List<Event> items = events.getItems();

                int i = 0;
                for (Event event : items) {

                    // Get event info
                    String title = event.getSummary();
                    String location = event.getLocation();
                    String description = event.getDescription();
                    DateTime start = event.getStart().getDateTime();
                    DateTime end = event.getEnd().getDateTime();
                    if (start == null) {
                        // All-day events don't have start times, so just use
                        // the start date.
                        start = event.getStart().getDate();
                    }
                    if (end == null) {
                        end = event.getEnd().getDate();
                    }
                    String time = start.toString();
                    String endTime = end.toString();
                    Log.d("PARSE: ", "Start Time is " + time);
                    Log.d("PARSE: ", "End Time is " + endTime);
                    int hour = 0, minute = 0;
                    int month = 0, day = 0, year = 0;
                    String timeOfDay = null;
                    String minuteStr = null;
                    String partialDayTime = null;
                    String fullTimeStr = null;
                    boolean partialDay = false;

                    // We've got the string in the time variable, now parse individual data
                    if (time.contains("T")) {
                        // This means we have a specific time
                        String[] longTimeStringParts = time.split("T");
                        String sectionDate = longTimeStringParts[0];
                        String sectionTime = longTimeStringParts[1];

                        // Now get the individual month, day, and year
                        String[] dateParts = sectionDate.split("-");
                        year = Integer.parseInt(dateParts[0]);
                        month = Integer.parseInt(dateParts[1]);
                        day = Integer.parseInt(dateParts[2]);

                        // Now get the time hour and minutes
                        String[] timeParts = sectionTime.split(":");
                        hour = Integer.parseInt(timeParts[0]);
                        minute = Integer.parseInt(timeParts[1]);

                        partialDay = true;
                    } else {
                        // This means we have an all-day event
                        // Now get the individual month, day, and year
                        String[] dateParts = time.split("-");
                        year = Integer.parseInt(dateParts[0]);
                        month = Integer.parseInt(dateParts[1]);
                        day = Integer.parseInt(dateParts[2]);


                    }

                    if (partialDay) {
                        Log.d("PARSE: ", "Found partial day.");
                        Log.d("PARSE: ", "YEAR: '" + Integer.toString(year) +
                                "' | MONTH: '" + Integer.toString(month) +
                                "' | DAY: '" + Integer.toString(day) +
                                "' | HOUR: '" + Integer.toString(hour) +
                                "' | MINUTE: '" + Integer.toString(minute) + "'");
                    } else {
                        Log.d("PARSE: ", "Found full day.");
                        Log.d("PARSE: ", "YEAR: '" + Integer.toString(year) +
                                "' | MONTH: '" + Integer.toString(month) +
                                "' | DAY: '" + Integer.toString(day) + "'");
                    }

                    // Do some checks to get an appropriate time
                    if (partialDay) {

                        // Modify the hour as needed
                        if (hour == 0) {
                            hour = 12;
                            timeOfDay = "AM";
                        } else if (hour < 12) {
                            timeOfDay = "AM";
                        } else if (hour == 12) {
                            timeOfDay = "PM";
                        } else {
                            hour = hour - 12;
                            timeOfDay = "PM";
                        }

                        // Modify the minute as needed so it's two digits.
                        if (minute < 10) {
                            minuteStr = "0" + Integer.toString(minute);
                        } else {
                            minuteStr = Integer.toString(minute);
                        }

                        partialDayTime = Integer.toString(hour) + ":" + minuteStr + " " + timeOfDay;
                        fullTimeStr = "";
                        fullTimeStr += Integer.toString(month) + "/";
                        fullTimeStr += Integer.toString(day) + "/";
                        fullTimeStr += Integer.toString(year) + ", " + partialDayTime;
                        Log.d("PARSE: ", "TIME: " + fullTimeStr);
                    } else {
                        fullTimeStr = "";
                        fullTimeStr += Integer.toString(month) + "/";
                        fullTimeStr += Integer.toString(day) + "/";
                        fullTimeStr += Integer.toString(year) + ", all day";
                        Log.d("PARSE: ", "TIME: " + fullTimeStr);
                    }


                    // Add to string list that will be used for filling in current page buttons
                    // Date/Time goes first, then the event title.
                    eventStrings.add(
                            String.format("%s\n%s", fullTimeStr, event.getSummary()));

                    // We need to keep this event info for the intent if user goes to next page
                    // Get current element and replace it
                    eventTitles.set(i, title);
                    eventTimes.set(i, fullTimeStr);
                    eventLocations.set(i, location);
                    eventDescriptions.set(i, description);
                    eventTimesRfc3339.set(i, time);
                    endTimesRfc3339.set(i, endTime);
                    i++; // Increment i to replace the next event
                }
                int numOfEvents = eventStrings.size();
                Log.d("EventListing: ", "Size of eventStrings is " + Integer.toString(numOfEvents));
                if (numOfEvents < NUMBUTTONS) {
                    int numOfPlaceholders = NUMBUTTONS - numOfEvents;
                    for (i = numOfEvents; i < NUMBUTTONS; i++) {
                        eventTitles.set(i, NOEVENT);
                        eventTimes.set(i, NOEVENT);
                        eventLocations.set(i, NOEVENT);
                        eventDescriptions.set(i, NOEVENT);
                        eventTimesRfc3339.set(i, NOEVENT);
                        endTimesRfc3339.set(i, NOEVENT);
                    }
                    for (int j = 0; j < numOfPlaceholders; j++) {
                        eventStrings.add(NOEVENT);
                    }
                }
                return eventStrings;
            } catch (IOException e) {
                mLastError = e;

                if (mLastError instanceof UserRecoverableAuthIOException) {
                    Log.d("getDataFromApi: ", "User needs to give permission for Calendar");
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Log.d("getDataFromApi: ", "IOException: " + e.getMessage());
                }
            } catch (Exception e) {
                Log.d("getDataFromApi: ", "Exception: " + e.getMessage());
                mLastError = e;

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
                Log.d("EventListing: ", "No output");
            } else {
                for (int i = 0; i < NUMBUTTONS; i++) {
                    btnList.get(i).setText(output.get(i));
                }
                enableEventButtons();
            }
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
        getMenuInflater().inflate(R.menu.menu_eventlisting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.eventListingMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}