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

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class interacts with the Facebook Listing page to fetch and display the most recent
 * Facebook posts from a group; in my case, it is the SOSUML Facebook page.
 *
 * @author Roy Van Liew
 */
public class FacebookListing extends AppCompatActivity {

    // Access Token string in the form of app_id|app_secret
    String applicationId, applicationSecret, userId, accessTokenStr;
    String PAGEPATH = "/name_of_page";

    // Access Token that will be used to make the graph api requests
    AccessToken appToken;

    // Table contents, including image and text
    ImageView img0, img1, img2, img3, img4;
    ImageButton btnRefresh;
    Button btnText0, btnText1, btnText2, btnText3, btnText4;

    // Other UI items
    ProgressDialog graphApiProgress;

    // The information we store about the posts.
    private List<String> postImageUrls;
    private List<String> postMessages;
    private List<String> postDescriptions;
    private List<String> postTimes;
    private List<String> postNames;
    private List<String> postStories;

    // Information from SOS page
    JSONObject graphPostResults;
    JSONObject postsField;
    JSONArray dataFieldArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_facebooklisting);

        // Get the imageviews
        img0 = (ImageView) findViewById(R.id.imgResult0);
        img1 = (ImageView) findViewById(R.id.imgResult1);
        img2 = (ImageView) findViewById(R.id.imgResult2);
        img3 = (ImageView) findViewById(R.id.imgResult3);
        img4 = (ImageView) findViewById(R.id.imgResult4);

        // Get the buttons
        btnRefresh = (ImageButton) findViewById(R.id.btnRefresh);
        btnText0 = (Button) findViewById(R.id.msgResult0);
        btnText1 = (Button) findViewById(R.id.msgResult1);
        btnText2 = (Button) findViewById(R.id.msgResult2);
        btnText3 = (Button) findViewById(R.id.msgResult3);
        btnText4 = (Button) findViewById(R.id.msgResult4);

        // Add Callback Listeners for the buttons
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    getFacebookPosts();
                } catch (Exception e) {
                    Log.d("FacebookListing: ", "Manual Facebook Post Refresh on "
                            + "page enter failed because " + e.getMessage());
                    graphApiProgress.hide();
                }
            }
        });
        btnText0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToPostByIndex(0);
            }
        });
        btnText1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToPostByIndex(1);
            }
        });
        btnText2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToPostByIndex(2);
            }
        });
        btnText3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToPostByIndex(3);
            }
        });
        btnText4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToPostByIndex(4);
            }
        });

        // Intialize all the string info lists
        postImageUrls = new ArrayList<String>();
        postMessages = new ArrayList<String>();
        postDescriptions = new ArrayList<String>();
        postTimes = new ArrayList<String>();
        postNames = new ArrayList<String>();
        postStories = new ArrayList<String>();

        // Initialize everything to "None" strings, five of them since we have five results
        String placeholder = "None";
        for (int i = 0; i < 5; i++) {
            postImageUrls.add(placeholder);
            postMessages.add(placeholder);
            postDescriptions.add(placeholder);
            postTimes.add(placeholder);
            postNames.add(placeholder);
            postStories.add(placeholder);
        }

        // Get string resources
        applicationId = getResources().getString(R.string.facebook_app_id);
        applicationSecret = getResources().getString(R.string.facebook_app_secret);
        userId = getResources().getString(R.string.facebook_user_id);
        accessTokenStr = applicationId + "|" + applicationSecret;

        // Create an access token for the app with an infinite expiration time
        appToken = new AccessToken(accessTokenStr, applicationId, userId,
                null, null, null, null, null);

        // Initialize progress dialog
        graphApiProgress = new ProgressDialog(this, R.style.SOSAlertDialog);
        graphApiProgress.setMessage("Fetching Facebook Posts...");

        // Make the automatic request. Disable the post buttons at first.
        disablePostButtons();
        try {
            getFacebookPosts(); // Automatically get results. User can refresh later
        } catch (Exception e) {
            Log.d("FacebookListing: ", "Automatic Facebook Post Refresh on "
                    + "page enter failed because " + e.getMessage());
            graphApiProgress.hide();
        }

    }

    /**
     * The information for each Facebook post result is stored in order by index.
     * This provides an easy way to pass the data to another page; in our case,
     * going to the Facebook Info page that displays more information about one post.
     *
     * @param index Which facebook post info we want to display in the
     *              Facebook Info page.
     */
    private void goToPostByIndex(int index) {

        // Create the intent to transfer control over to the facebook info page.
        Intent intent = new Intent(FacebookListing.this, FacebookInfo.class);

        // Make sure we pass the correct values (by index) over as well.
        // More data can be passed in the future.
        intent.putExtra("pictureUrl", postImageUrls.get(index));
        intent.putExtra("message", postMessages.get(index));
        intent.putExtra("description", postDescriptions.get(index));
        intent.putExtra("time", postTimes.get(index));
        intent.putExtra("name", postNames.get(index));
        intent.putExtra("story", postStories.get(index));
        this.startActivity(intent);
    }

    /**
     * Disables the Facebook post result buttons.
     */
    private void disablePostButtons() {
        btnText0.setEnabled(false);
        btnText1.setEnabled(false);
        btnText2.setEnabled(false);
        btnText3.setEnabled(false);
        btnText4.setEnabled(false);
        btnText0.setText(getResources().getString(R.string.facebook_listing_click_refresh));
        btnText1.setText(getResources().getString(R.string.facebook_listing_click_refresh));
        btnText2.setText(getResources().getString(R.string.facebook_listing_click_refresh));
        btnText3.setText(getResources().getString(R.string.facebook_listing_click_refresh));
        btnText4.setText(getResources().getString(R.string.facebook_listing_click_refresh));
    }

    /**
     * Re-enables the Facebook post result buttons.
     */
    private void enablePostButtons() {
        btnText0.setEnabled(true);
        btnText1.setEnabled(true);
        btnText2.setEnabled(true);
        btnText3.setEnabled(true);
        btnText4.setEnabled(true);
    }

    /**
     * Makes the Facebook Graph API call to fetch the most recent Facebook posts
     * from the SOSUML page. Note that you must have a valid access token for this
     * to work! In my case, I am using an app secret, app id, and user id.
     */
    private void getFacebookPosts() {

        // Disable the post buttons first.
        disablePostButtons();
        graphApiProgress.show();

        // Make the Graph API request for getting posts from the SOSUML public page
        // This is basically an authentication step.
        GraphRequest request = GraphRequest.newGraphPathRequest(
                appToken,
                PAGEPATH,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        // Insert your code here
                        Log.d("onCompleted: ", "Successfully made the Graph API Request");
                        graphPostResults = response.getJSONObject();

                        // Try to get the posts field (another JSON object)
                        try {
                            postsField = graphPostResults.getJSONObject("posts");
                        } catch (Exception e) {
                            Log.d("onCompleted: ", "Failed getting posts field from request, " + e.getMessage());
                        }

                        // In the posts JSON object is a field 'data' that has a JSON Array
                        try {
                            dataFieldArray = postsField.getJSONArray("data");
                        } catch (Exception e) {
                            Log.d("onCompleted: ", "Failed getting data field array, " + e.getMessage());
                        }

                        // At this point we should have the JSON Array in the posts field.
                        // Each array item is a JSON object with 'message' and 'created_time' fields
                        try {
                            Log.d("onCompleted: ", "Attempting to print JSONs, data first then message");
                            JSONObject post;
                            String msgField = "message";
                            String descField = "description";
                            String timeField = "created_time";
                            String picField = "full_picture";
                            String nameField = "name";
                            String storyField = "story";
                            for (int i = 0, size = dataFieldArray.length(); i < size; i++) {
                                post = dataFieldArray.getJSONObject(i);

                                // Now add messages and/or picture URLs as necessary
                                postTimes.set(i, post.getString(timeField));
                                Log.d("onCompleted: ", "> GOING THROUGH JSON ENTRY");
                                Log.d("onCompleted: ", "JSON Time: " + post.getString(timeField));

                                if (post.has(msgField)) {
                                    postMessages.set(i, post.getString(msgField));
                                    Log.d("onCompleted: ", "JSON Message: " + post.getString(msgField));
                                }
                                if (post.has(descField)) {
                                    postDescriptions.set(i, post.getString(descField));
                                    Log.d("onCompleted: ", "JSON Description: " + post.getString(descField));
                                }
                                if (post.has(picField)) {
                                    postImageUrls.set(i, post.getString(picField));
                                    Log.d("onCompleted: ", "JSON Picture Link: " + post.getString(picField));
                                }
                                if (post.has(nameField)) {
                                    postNames.set(i, post.getString(nameField));
                                    Log.d("onCompleted: ", "JSON Name: " + post.getString(nameField));
                                }
                                if (post.has(storyField)) {
                                    postStories.set(i, post.getString(storyField));
                                    Log.d("onCompleted: ", "JSON Story: " + post.getString(storyField));
                                }
                            }
                        } catch (Exception e) {
                            Log.d("onCompleted: ", "Failed printing out posts JSON data, " + e.getMessage());
                        }

                        try {

                            // Set all 5 button text contents
                            if (postMessages.get(0).equals("None")) {
                                btnText0.setText(postDescriptions.get(0));
                            } else {
                                btnText0.setText(postMessages.get(0));
                            }

                            if (postMessages.get(1).equals("None")) {
                                btnText1.setText(postDescriptions.get(1));
                            } else {
                                btnText1.setText(postMessages.get(1));
                            }

                            if (postMessages.get(2).equals("None")) {
                                btnText2.setText(postDescriptions.get(2));
                            } else {
                                btnText2.setText(postMessages.get(2));
                            }

                            if (postMessages.get(3).equals("None")) {
                                btnText3.setText(postDescriptions.get(3));
                            } else {
                                btnText3.setText(postMessages.get(3));
                            }

                            if (postMessages.get(4).equals("None")) {
                                btnText4.setText(postDescriptions.get(4));
                            } else {
                                btnText4.setText(postMessages.get(4));
                            }

                            // Set all 5 picture contents in the future
                            if (!postImageUrls.get(0).equals("None")) {
                                new DownloadImageTask(img0).execute(postImageUrls.get(0));
                            }
                            if (!postImageUrls.get(1).equals("None")) {
                                new DownloadImageTask(img1).execute(postImageUrls.get(1));
                            }
                            if (!postImageUrls.get(2).equals("None")) {
                                new DownloadImageTask(img2).execute(postImageUrls.get(2));
                            }
                            if (!postImageUrls.get(3).equals("None")) {
                                new DownloadImageTask(img3).execute(postImageUrls.get(3));
                            }
                            if (!postImageUrls.get(4).equals("None")) {
                                new DownloadImageTask(img4).execute(postImageUrls.get(4));
                            }


                        } catch (Exception e) {
                            Log.d("onCompleted: ", "Failed setting button text contents, " + e.getMessage());
                        }

                        // Now hide the progress dialog.
                        graphApiProgress.hide();
                        enablePostButtons();
                    }
                }
        );

        // Now we apply parameters for the request, and then execute it.
        Bundle parameters = new Bundle();
        parameters.putString("fields", "posts.limit(5){full_picture,message,created_time,"
                + "description,name,shares,status_type,story}");
        request.setParameters(parameters);
        request.executeAsync();

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
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_facebooklisting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item click from user on the exit option.
        int id = item.getItemId();
        if (id == R.id.facebookListingMenuRtn) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}