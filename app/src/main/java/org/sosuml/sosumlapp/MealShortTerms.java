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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * This class interacts with the Meal Short Terms page that displays the
 * SOS Terms and Conditions before the user can go to the Meal Donation page.
 *
 * @author Roy Van Liew
 */
public class MealShortTerms extends AppCompatActivity {

    private String EMAILTO = "test@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mealshortterms);

        // Get the terms TextView
        TextView desc = (TextView) findViewById(R.id.mealShortTermsDescription);
        setTextViewHTML(desc, getString(R.string.meal_short_terms_description));

        // Get the two buttons on the short terms page
        Button btnAccept = (Button) findViewById(R.id.mealShortTermsBtnAccept);
        Button btnDecline = (Button) findViewById(R.id.mealShortTermsBtnDecline);
        
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToMealForm();
            }
        });
        
        btnDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });

    }

    /**
     * Creates an intent to go to the Meal Form page where the user can
     * request to make a Guest Meal donation.
     */
    private void goToMealForm() {
        Intent intent = new Intent(MealShortTerms.this, MealForm.class);
        this.startActivity(intent);
    }

    /**
     * Helper function to determine if the user has an e-mail app configured
     * on the phone.
     *
     * @param context The activity that called this function.
     *
     * @return true if the user does have a configured e-mail app, false if
     *         the user does not have a configured e-mail app on the phone.
     */
    public static boolean isMailClientPresent(Context context){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, 0);
        int numClients = list.size();

        if(numClients == 0) {
            Log.d("isMailClientPresent:", "No e-mail clients present");
            return false;
        } else {
            Log.d("isMailClientPresent:", Integer.toString(numClients) + " e-mail clients are present");
            return true;
        }
    }

    /**
     * Defines the action to take when the user clicks on an anchor tag in the
     * HTML text. In my case, there will be an e-mail send intent that will
     * be started when the user clicks an anchor tag.
     */
    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
    {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                // Do something with span.getURL() to handle the link click...
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{EMAILTO});
                i.putExtra(Intent.EXTRA_SUBJECT, "Question about Terms and Conditions");
                i.putExtra(Intent.EXTRA_TEXT   , "Hello SOS, I had a question about your Terms " +
                        "and Conditions.");
                try {
                    if (isMailClientPresent(MealShortTerms.this)) {
                        Log.d("MealShortTerms: ", "makeLinkClickable: Launching e-mail picker intent");
                        startActivity(Intent.createChooser(i, "Contact SOS..."));
                    } else {
                        Toast.makeText(MealShortTerms.this,
                                "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MealShortTerms.this,
                            "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    /**
     * Fills a TextView on the page with formatted HTML text.
     *
     * @param text The TextView for displaying the formatted HTML text.
     * @param html The HTML text content as a string.
     */
    protected void setTextViewHTML(TextView text, String html)
    {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for(URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mealshortterms, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.mealShortTermsMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
