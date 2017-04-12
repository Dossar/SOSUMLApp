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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static org.sosuml.sosumlapp.GraphicsUtilities.*;

/**
 * This class interacts with the Event Bridge page that checks to see if the user is
 * registered before going to the Event Listing page.
 *
 * @author Roy Van Liew
 */
public class EventBridge extends AppCompatActivity {

    // Our DB handler for the meal donation request
    private MealDbHandler db;

    // Other page elements
    private TextView bridgeTxtDesc;

    // For determining if the user is registered
    static final int REGISTER_ACTIVITY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventbridge);

        // Get the three buttons on the activity page
        Button btnRegister = (Button) findViewById(R.id.eventBridgeBtnRegister);
        Button btnEventList = (Button) findViewById(R.id.eventBridgeBtnEventList);

        // Initialize description HTML text
        bridgeTxtDesc = (TextView)findViewById(R.id.eventBridgeDescription);
        bridgeTxtDesc.setText(Html.fromHtml(getString(R.string.event_bridge_description)));

        // First get an instance of our database.
        db = new MealDbHandler(this);
        
        // Get the button for configuring the user's email account.
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegisterForm();
            }
        });

        // Check to see if the user is already registered.
        if (isRegistered()) {
            btnRegister.setBackgroundColor(getResources().getColor(R.color.completed_green));
            btnRegister.setText("Registered " + Html.fromHtml("&#x2714;"));
        }
        
        btnEventList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (isRegistered()) {
                    goToEventListing();
                } else {
                    displayAlert(EventBridge.this,
                            "You must be registered to view SOS Events.",
                            "Not Registered",
                            "OK");
                }
            }
        });

    }

    /**
     * Creates an intent to go to the Registration page.
     */
    private void goToRegisterForm() {
        Intent intent = new Intent(EventBridge.this, RegisterForm.class);
        startActivityForResult(intent, REGISTER_ACTIVITY);
    }

    /**
     * Creates an intent to go to the Event Listing page.
     */
    private void goToEventListing() {
        Intent intent = new Intent(EventBridge.this, EventListing.class);
        this.startActivity(intent);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_eventbridge, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.eventBridgeMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
