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
 * This class is used to determine whether or not a user has sent a
 * Meal Donation Request.
 *
 * @author Roy Van Liew
 */
public class MealDbDate {

    // We will only have one meal entry in the DB
    public static final int MEAL_ID = 1;

    //private variables
    int _id;
    int _month;
    int _day;
    int _year;

    // Empty constructor
    public MealDbDate(){

    }

    // constructor
    public MealDbDate(int id, int month, int day, int year){
        this._id = id;
        this._month = month;
        this._day = day;
        this._year = year;
    }

    // constructor
    public MealDbDate(int month, int day, int year){
        this._id = MEAL_ID;
        this._month = month;
        this._day = day;
        this._year = year;
    }

    public int getID(){
        return this._id;
    }

    public void setID(int id){
        this._id = id;
    }

    public int getMonth(){
        return this._month;
    }

    public void setMonth(int month){
        this._month = month;
    }

    public int getDay(){
        return this._day;
    }

    public void setDay(int day){
        this._day = day;
    }

    public int getYear(){
        return this._year;
    }

    public void setYear(int year){
        this._year = year;
    }
}
