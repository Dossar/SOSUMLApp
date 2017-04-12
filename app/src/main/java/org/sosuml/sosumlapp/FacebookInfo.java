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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * This class interacts with the Facebook Info page to display more information
 * about one Facebook post result.
 *
 * @author Roy Van Liew
 */
public class FacebookInfo extends AppCompatActivity {

    private String pictureUrl, message, description, time, name, story;
    private TextView txtMessage, txtDescription, txtHeader, txtTime;
    private ImageView imgPicture;
    private int imgHeight, imgWidth, phoneHeight, phoneWidth;
    private float scale;

    // Placeholders
    private String NAME_PLACEHOLDER = "SOSUML created a post.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebookinfo);

        // Get the event information passed through four variables.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            pictureUrl = extras.getString("pictureUrl", "None");
            message = extras.getString("message", "None");
            description = extras.getString("description", "None");
            time = extras.getString("time", "None");
            name = extras.getString("name", "None");
            story = extras.getString("story", "None");
        } else {
            finish(); // No point in continuing if we don't have values
        }

        // Get the references to the widgets.
        txtHeader = (TextView) findViewById(R.id.facebookInfoHeader);
        txtTime = (TextView) findViewById(R.id.facebookInfoTime);
        txtMessage = (TextView) findViewById(R.id.facebookInfoMessage);
        txtDescription = (TextView) findViewById(R.id.facebookInfoDescription);
        imgPicture = (ImageView) findViewById(R.id.facebookInfoImage);

        boolean messageExists = false, descriptionExists = false;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int densityDpi = (int)metrics.density;
        int MARGIN_VALUE_DP = 15;
        int MARGIN_VALUE_PX = MARGIN_VALUE_DP * densityDpi;

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.border);

        /* First check if we've got the story field for the header. If not, then we will go
           by the name field (this would be the case where SOS didn't link an article and just
           made a plain text post, or linked to an external article instead of sharing a post). */
        if (!story.equals("None")) {

            // In this case we do have the story which is basically the "headline" of shared posts.
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            relativeParams.setMargins(MARGIN_VALUE_PX, MARGIN_VALUE_PX,
                    MARGIN_VALUE_PX, MARGIN_VALUE_PX);
            //txtHeader.setBackground(drawable);
            txtHeader.setText(story);
            txtHeader.setLayoutParams(relativeParams);
        } else if (!name.equals("None")) {

            // In this case it is not a shared story.
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            relativeParams.setMargins(MARGIN_VALUE_PX, MARGIN_VALUE_PX,
                    MARGIN_VALUE_PX, MARGIN_VALUE_PX);
            //txtHeader.setBackground(drawable);

            // "Timeline Photos" is redundant and useless, so instead use placeholder name
            if (name.equals("Timeline Photos")) {
                txtHeader.setText(NAME_PLACEHOLDER);
            } else {
                txtHeader.setText(name);
            }
            txtHeader.setLayoutParams(relativeParams);
        } else {

            // In this case SOS just made a plain text post.
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            relativeParams.setMargins(MARGIN_VALUE_PX, MARGIN_VALUE_PX,
                    MARGIN_VALUE_PX, MARGIN_VALUE_PX);
            //txtHeader.setBackground(drawable);

            // Since we had no name field, we'll use a placeholder stating SOS made the post.
            txtHeader.setText(NAME_PLACEHOLDER);
            txtHeader.setLayoutParams(relativeParams);
        }

        /* It is guaranteed we will have the time, so we can do this without any problems. */
        if (!time.equals("None")) {
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.BELOW, R.id.facebookInfoHeader);
            relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            relativeParams.setMargins(MARGIN_VALUE_PX, MARGIN_VALUE_PX,
                    MARGIN_VALUE_PX, MARGIN_VALUE_PX);
            //txtTime.setBackground(drawable);
            txtTime.setText(getReadableTime(time));
            txtTime.setLayoutParams(relativeParams);
        }

        /* Check if we've got the message. If not, pass through. */
        if (!message.equals("None")) {
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            relativeParams.setMargins(MARGIN_VALUE_PX, MARGIN_VALUE_PX,
                    MARGIN_VALUE_PX, MARGIN_VALUE_PX);
            //txtMessage.setBackground(drawable);
            txtMessage.setText(message);

            // If the message does exists, it'll be directly under the time field.
            relativeParams.addRule(RelativeLayout.BELOW, R.id.facebookInfoTime);
            txtMessage.setLayoutParams(relativeParams);
            messageExists = true;
        }

        /* Check if we've got the description. If not, pass through. */
        if (!description.equals("None")) {
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            relativeParams.setMargins(MARGIN_VALUE_PX, MARGIN_VALUE_PX,
                    MARGIN_VALUE_PX, MARGIN_VALUE_PX);
            //txtDescription.setBackground(drawable);
            txtDescription.setText(description);
            if (messageExists) {
                relativeParams.addRule(RelativeLayout.BELOW, R.id.facebookInfoMessage);
                txtDescription.setLayoutParams(relativeParams);
                descriptionExists = true;
            } else {
                relativeParams.addRule(RelativeLayout.BELOW, R.id.facebookInfoTime);
                txtDescription.setLayoutParams(relativeParams);
                descriptionExists = true;
            }
        }

        if (!pictureUrl.equals("None")) {
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            relativeParams.setMargins(MARGIN_VALUE_PX, MARGIN_VALUE_PX,
                    MARGIN_VALUE_PX, MARGIN_VALUE_PX);
            //imgPicture.setBackground(drawable);
            new DownloadImageTask(imgPicture).execute(pictureUrl);
            if (descriptionExists) {
                relativeParams.addRule(RelativeLayout.BELOW, R.id.facebookInfoDescription);
                imgPicture.setLayoutParams(relativeParams);
            } else if (messageExists) {
                relativeParams.addRule(RelativeLayout.BELOW, R.id.facebookInfoMessage);
                imgPicture.setLayoutParams(relativeParams);
            } else {
                relativeParams.addRule(RelativeLayout.BELOW, R.id.facebookInfoTime);
                imgPicture.setLayoutParams(relativeParams);
            }

        }

    }

    /**
     * Takes a string formatted by RFC3339 specifications and makes it human-readable.
     *
     * @param rfc3339Time The time string formatted by RFC3339 specifications.
     * @return String that details the time in a human-readable format.
     */
    String getReadableTime(String rfc3339Time) {

        int year, month, day, hour, minute;
        String timeOfDay, minuteStr, partialDayTime, fullTimeStr, timeZone;
        TimeZone phoneTz, utcTz;
        Calendar phoneCal, utcCal;

        try {

            // In the case of Facebook Times, it is always a specific time
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

            /* TIME MECHANICS: CONVERT FROM UTC TO PHONE'S TIME ZONE */
            // We first need to find the timezone.
            phoneTz = TimeZone.getDefault();
            utcTz = TimeZone.getTimeZone("UTC");
            Log.d("getReadableTime:", "Time Zone is " + phoneTz.getDisplayName());

            // Create the calendars for both the phone and UTC.
            utcCal = new GregorianCalendar(utcTz);
            utcCal.set(Calendar.HOUR_OF_DAY, hour);
            utcCal.set(Calendar.MINUTE, minute);
            utcCal.set(Calendar.SECOND, 0);

            phoneCal = new GregorianCalendar(phoneTz); // Phone Time Zone
            phoneCal.setTimeInMillis(utcCal.getTimeInMillis());
            hour = phoneCal.get(Calendar.HOUR_OF_DAY);
            minute = phoneCal.get(Calendar.MINUTE);
            /* END TIME MECHANICS */

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
            return fullTimeStr;
        } catch (Exception e) {
            Log.d("getReadableTime: ", e.getMessage());
            return "Unknown Creation Date";
        }
    }

    /**
     * Fetches an image to display into an ImageView on the page specified by a URL.
     */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int densityDpi = (int)metrics.density;
            Log.d("onPostExecute: ", "DPI Density is " + Integer.toString(densityDpi));
            imgHeight = result.getHeight();
            imgWidth = result.getWidth();
            phoneHeight = metrics.heightPixels;
            phoneWidth = metrics.widthPixels;
            Log.d("onPostExecute: ", "Image Height is " + Integer.toString(imgHeight) + "px");
            Log.d("onPostExecute: ", "Image Width is " + Integer.toString(imgWidth) + "px");
            Log.d("onPostExecute: ", "Phone Height is " + Integer.toString(phoneHeight) + "px");
            Log.d("onPostExecute: ", "Phone Width is " + Integer.toString(phoneWidth) + "px");

            // We need to make sure the picture takes up the full width of the screen and that
            // the height adjusts accordingly.
            scale = ((float)phoneWidth/imgWidth);
            imgPicture.getLayoutParams().height = (int)(imgHeight * scale);
            imgPicture.requestLayout();
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_facebookinfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.facebookInfoMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}