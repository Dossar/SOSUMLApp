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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

/**
 * This class interacts with the Main Menu page that lists the different parts
 * of the app the user can visit.
 *
 * @author Roy Van Liew
 */
public class MainMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenu);

        // Add a listener to the button that takes us to the convert page
        Button btnGoToMealShortTerms = (Button) findViewById(R.id.mainMenuBtnDonateMeal);
        Button btnGoToRegister = (Button) findViewById(R.id.mainMenuBtnRegister);
        Button btnGoToEvents = (Button) findViewById(R.id.mainMenuBtnEvents);
        Button btnGoToFacebook = (Button) findViewById(R.id.mainMenuBtnFacebook);

        btnGoToMealShortTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToMealShortTerms();
            }
        });

        btnGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToRegisterForm();
            }
        });

        btnGoToEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToEventListing();
            }
        });

        btnGoToFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToFacebookListing();
            }
        });

    }

    /**
     * Creates an intent to go to the Terms & Conditions Page that appears
     * before the user can go to the Meal Donation page.
     */
    private void goToMealShortTerms() {
        Intent intent = new Intent(MainMenu.this, MealShortTerms.class);
        this.startActivity(intent);
    }

    /**
     * Creates an intent to go to the Registration page.
     */
    private void goToRegisterForm() {
        Intent intent = new Intent(MainMenu.this, RegisterForm.class);
        this.startActivity(intent);
    }

    /**
     * Creates an intent to go to the Event Bridge page that appears
     * before the user can go to the Event Listing page.
     */
    private void goToEventListing() {
        Intent intent = new Intent(MainMenu.this, EventBridge.class);
        this.startActivity(intent);
    }

    /**
     * Creates an intent to go to the Facebook Listing page.
     */
    private void goToFacebookListing() {
        Intent intent = new Intent(MainMenu.this, FacebookListing.class);
        this.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.mainMenuMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
