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

/**
 * This class is used to determine whether or not a user has signed up for
 * a Google Calendar event. Identifiers for an event are its title and
 * its time. The limitation is that the title cannot have double quotes.
 *
 * @author Roy Van Liew
 */
public class CalSignupDbEntry {

    // Private variables
    int _id;
    String _title;
    String _time;

    // Empty constructor
    public CalSignupDbEntry(){}

    // Constructor
    public CalSignupDbEntry(String title, String time){
        this._title = title;
        this._time = time;
    }

    public CalSignupDbEntry(int id, String title, String time){
        this._id = id;
        this._title = title;
        this._time = time;
    }

    public int getID(){ return this._id; }
    public void setID(int id){this._id = id;}
    public String getTitle(){return this._title;}
    public void setTitle(String title){this._title = title;}
    public String getTime(){return this._time;}
    public void setTime(String time){this._time = time;}

}
