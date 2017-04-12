# SOSUML App

![sos_logo_2016.png](http://i948.photobucket.com/albums/ad328/DossarLX/sosumlapp/sos_logo_2016.png)

The SOSUML App is an Android application for the on-campus organization at UMass Lowell, Support Our Students (SOS). It is targeted towards undergraduate students at UMass Lowell. The app allows the user to do three main tasks:

* Request to donate one of their Guest Meals
* See upcoming SOS Events
* See the most recent Facebook Posts from SOS

In terms of the README, it is structured in a way to help you get up and running with the app. It may appear long at first, but it will ensure that the API calls will be functional.

# Table of Contents
* Licensing
* Considerations
* Setup
* Necessary Code Changes

## § Licensing

This application uses the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0").

## § Considerations

This app is targeted towards users with at least Android 6.0 (API 23/Marshmallow). Development is targeted towards API 25 (Nougat).

Dependencies are managed in the app gradle file to provide:

* Packages required to use the Gmail API
* Packages required to send e-mail
* Packages to use the Google Calendar API
* Packages to use the Facebook Graph API

## § Setup

Using the Google and Facebook APIs requires you to have valid credentials. This section is to help get you set up with the credentials needed to get these components working; otherwise, the calls will fail.

### Cloning this repository

Before going to any of the other steps, make sure you've cloned this repository first.

### The app gradle file

Make sure you configure a keystore in the app's gradle file.

```gradle
    signingConfigs {
        keystore_name {
            keyAlias '...'
            keyPassword '...'
            storeFile file('path_to_keystore_file')
            storePassword '...'
        }
    }
```

### Facebook Credentials

In order to use the Facebook Graph API, you must become a developer on facebook and register your app. You can find those steps at [Register and Configure an App page]("https://developers.facebook.com/docs/apps/register").

After registering your app from the above steps, go to the app's Facebook information by going to the navigation in the top right where you your profile picture is. You will be directed to the dashboard of the app, which will look something like below:

![fb_dashboard.png](http://i948.photobucket.com/albums/ad328/DossarLX/sosumlapp/fb_dashboard.png)

To the left there should be a sidebar that says `Settings`. In the `Basic` subcategory, you should see some information about the app in addition to its configured platforms. The configured platforms section will start out empty, but in my case I have added Android as a platform.

![fb_settings_basic1.png](http://i948.photobucket.com/albums/ad328/DossarLX/sosumlapp/fb_settings_basic1.png)

![fb_settings_basic2.png](http://i948.photobucket.com/albums/ad328/DossarLX/sosumlapp/fb_settings_basic2.png)

When it comes to the `Key Hashes` section, you can run either of the following commands to retrieve it. I recommend running with the alias:

```
keytool -exportcert -keystore full_path_to_keystore_file | openssl sha1 -binary | openssl base64
keytool -exportcert -alias alias_of_keystore -keystore full_path_to_keystore_file | openssl sha1 -binary | openssl base64

Example:
keytool -exportcert -keystore /c/apps/android.keystore | openssl sha1 -binary | openssl base64
keytool -exportcert -alias android -keystore /c/apps/android.keystore | openssl sha1 -binary | openssl base64
```

You should get a random-looking string. This is the string you will put into the `Key Hashes` section.

Finally, **make sure you go to the App Review section of the sidebar and have `Yes` chosen for the setting that makes the app live and available to the public**. If this setting is not on `Yes`, your calls to the Graph API may fail.

### Google Credentials

In order to use the Gmail and Google Calendar APIs, you must register your app by creating a project through the Google Developer API wizard; in this case, we'll go by the id of the Calendar API.

#### Step 1: Acquire a SHA1 fingerprint

This step will allow you to get a SHA1 fingerprint to enable the API. Locate where your keystore is on your filesystem that you're using for this app, and then type either of the following commands:
```
keytool -exportcert -keystore full_path_to_keystore_file -list -v
keytool -exportcert -alias android -keystore full_path_to_keystore_file -list -v
```

The output should look something similar to below. What you are interested in is the `SHA1` resulting string, which in this case is `D8:AA:43:97:59:EE:C5:95:26:6A:07:EE:1C:37:8E:F4:F0:C8:05:C8`. This is the SHA1 fingerprint you will use to add credentials to the app.

```
$ keytool -exportcert -alias android -keystore /c/apps/android.keystore -list -v
Enter keystore password:  debug
Alias name: android
Creation date: Feb 13, 2017
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Android Debug, O=Android, C=US
Issuer: CN=Android Debug, O=Android, C=US
Serial number: 503bd581
Valid from: Mon Feb 13 14:44:28 EST 2017 until: Fri Feb 07 14:44:28 EST 2042
Certificate fingerprints:
         MD5:  1B:2B:2D:37:E1:CE:06:8B:A0:F0:73:05:3C:A3:63:DD
         SHA1: D8:AA:43:97:59:EE:C5:95:26:6A:07:EE:1C:37:8E:F4:F0:C8:05:C8
         SHA256: F3:6F:98:51:9A:DF:C3:15:4E:48:4B:0F:91:E3:3C:6A:A0:97:DC:0A:3F:B2:D2:E1:FE:23:57:F5:EB:AC:13:30
         Signature algorithm name: SHA256withRSA
         Version: 3
```

#### Step 2: Turn on both the Gmail API and Google Calendar API

The following instructions are from the Google Quickstart page.

* Use the [Calendar API configuration wizard](https://console.developers.google.com/start/api?id=calendar) to create a project in the Google Developers Console for turning on the API. Click **Continue**, then **Go to Credentials**.
* On the **Add credentials to your project** page, click the **Cancel** button.
* At the top of the page, select the **OAuth consent screen** tab. Select an **Email address**, enter a **Product name** if not already set, and click the **Save** button.
* Select the **Credentials** tab, click the **Create credentials** button and select **OAuth client ID**.
* Select the application type **Android**.
* Copy the SHA1 fingerprint from Step 1 into the **Signing-certificate fingerprint** field.
* In the **Package name** field, enter **org.sosuml.sosumlapp**.
* Click the Create button.
* Go to the Library tab. In the search bar, type `gmail` to get to the Gmail API. Click the search result, and then click ENABLE.
* Since we already enabled the Google Calendar API with the link from the first bullet point, we do not have to go back to the Library tab to enable it. However, it never hurts to double-check. If you see DISABLE when you get to Google Calendar API, it means it is enabled.


## § Necessary Code Changes

You cannot just run the app in its current state. You need to look in several specific code files to properly substitute valid values for the API calls to work. This section assumes you have already done the Setup section above.

### EventInfo.java

The Gmail API call for sending an e-mail in the Calendar Event Signup request is done in the `createAndSendEmail()` function.

```java
emailToSendSos = createHtmlEmail(SOSEMAIL, currentUser,
                                    sosSubject, sosBody);
```

The `SOSEMAIL` variable is defined as a string, which is the e-mail to send to. For example, `test@gmail.com`. It does not have to be a gmail account. Note that the app also sends an e-mail to the UMass Lowell student e-mail entered in the Registration page.

### EventListing.java

The Calendar API call to fetch events from a public Google Calendar can be seen in the `getDataFromApi()` function.

```java
Events events = calendarService.events().list(PUBLICCALENDAR)
                .setMaxResults(NUMBUTTONS)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
```

The `PUBLICCALENDAR` variable is defined as a string, which is the gmail account with the public Google Calendar to get events from. For example, `test@gmail.com`.

### FacebookListing.java

The Graph API call uses an access token that is constructed based off of the user ID, app secret, and app ID. These three pieces of information are defined in the `strings.xml` file.

```xml
<string name="facebook_app_id">...</string>
<string name="facebook_app_secret">...</string>
<string name="facebook_user_id">...</string>
```

You also need to make sure that your Graph API call is referencing the correct public Facebook page. We want to look at the function `getFacebookPosts()`.

```java
GraphRequest request = GraphRequest.newGraphPathRequest(
    appToken,
    PAGEPATH,
    new GraphRequest.Callback() {
        // Lots of stuff
    }
);
```

The `PAGEPATH` variable is defined as a string, which is the path to the facebook page. In my case, it is `"/SOSUML"`.

### MealForm.java

The Gmail API call for sending an e-mail in requesting to donate a guest meal is done in the `createAndSendEmail()` function.

```java
emailToSendSos = createHtmlEmail(SOSEMAIL, currentUser,
                                    sosSubject, sosBody);
```

The `SOSEMAIL` variable is defined as a string, which is the e-mail to send to. For example, `test@gmail.com`. It does not have to be a gmail account.

### MealShortTerms.java

An e-mail sending Intent is started when the user clicks the "contact us" text on the Terms and Conditions page. To state which e-mail address to send to, go to the `makeLinkClickable()` function, and to the line of code shown below:

```java
i.putExtra(Intent.EXTRA_EMAIL  , new String[]{EMAILTO});
```

The `EMAILTO` variable is defined as a string, which is the e-mail to send to. For example, `test@gmail.com`. It does not have to be a gmail account.
