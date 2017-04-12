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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * This class interacts with the Intro page that simply discusses what SOSUML does.
 * We also put in a placeholder meal donation if there hasn't been one set already.
 *
 * @author Roy Van Liew
 */
public class Intro extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Set text with HTML
        TextView introTxt = (TextView) findViewById(R.id.introDescription);
        introTxt.setText(Html.fromHtml(getString(R.string.intro_description)));

        // Add a listener to the button that takes us to the convert page
        Button btnGoToMainMenu = (Button) findViewById(R.id.introBtnGoToMainMenu);

        btnGoToMainMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToMainMenu();
            }
        });

        /* This check is done at the start to guarantee we have one meal donation request
         * entry by the time we get to the Meal Form page. The Main Idea is this:
         *
         * Check if there is Meal ID 1 in the database. If not, put a placeholder
         * date like March 5th, 1975 to guarantee it'll be a previous date.
         */

        // Get a new instance of a Meal Donation Db Handler, and check to see if entry exists
        MealDbHandler db = new MealDbHandler(this);
        MealDbDate newer = null;
        Log.d("CalendarTest: ", "Attempting to get Meal ID 1.");
        try {
            newer = db.getMealDbDate(MealDbDate.MEAL_ID);
        } catch (Exception e) {
            // do nothing, this just means we don't have the entry in the DB
        }

        // Check if the entry request was successful. If not, then enter the placeholder.
        if (newer == null) {
            Log.d("Intro: ", "Entry Id 1 is not in the DB yet. Will insert now.");
            MealDbDate placeholder = new MealDbDate(MealDbDate.MEAL_ID, 2, 5, 1975);
            db.addMealDbDate(placeholder);
            Log.d("Intro: ", "Added placeholder Meal Donation.");
        } else {
            Log.d("Intro: ", "Yes, a DB entry does already exist.");
            String n_id = Integer.toString(newer.getID());
            String n_month = Integer.toString(newer.getMonth());
            String n_day = Integer.toString(newer.getDay());
            String n_year = Integer.toString(newer.getYear());
            Log.d("Intro: ", "Id: " + n_id + ", Month: " + n_month
                    + ", Day: " + n_day + ", Year: " + n_year);
        }

    }

    /**
     * Creates an intent to go to the Main Menu page.
     */
    private void goToMainMenu() {
        Intent intent = new Intent(Intro.this, MainMenu.class);
        this.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_intro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.introMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
