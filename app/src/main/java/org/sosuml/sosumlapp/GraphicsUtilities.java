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
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

/**
 * This class is used for customized UI elements that are commonly used
 * throughout the app, such as an AlertDialog.
 *
 * @author Roy Van Liew
 */
public class GraphicsUtilities {

    /**
     * Private helper method to display an alert.
     */
    public static void displayAlert(Context context, String message, String title, String buttonText) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context, R.style.SOSAlertDialog);
        alert.setMessage(message);
        alert.setTitle(title);
        alert.setNeutralButton(
                buttonText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        Log.d("displayAlert: ", "OK");
                    }
                }
        );
        alert.create();
        alert.show();
    }
}
