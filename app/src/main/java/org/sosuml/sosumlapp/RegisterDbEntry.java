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
 * This class is used to store and update the user's registration information.
 *
 * @author Roy Van Liew
 */
public class RegisterDbEntry {

    // We will only have one register entry in the DB
    public static final int REGISTER_ID = 1;

    //private variables
    int _id;
    String _name;
    String _student_email;
    String _email;

    // Empty constructor
    public RegisterDbEntry(){

    }

    // constructor
    public RegisterDbEntry(int id, String name, String student_email,
                           String email){
        this._id = id;
        this._name = name;
        this._student_email = student_email;
        this._email = email;
    }

    // constructor
    public RegisterDbEntry(String name, String student_email, String email){
        this._id = REGISTER_ID;
        this._name = name;
        this._student_email = student_email;
        this._email = email;
    }

    public int getID(){
        return this._id;
    }

    public void setID(int id){
        this._id = id;
    }

    public String getName(){
        return this._name;
    }

    public void setName(String name){
        this._name = name;
    }

    public String getStudentEmail(){
        return this._student_email;
    }

    public void setStudentEmail(String student_email){
        this._student_email = student_email;
    }

    public String getEmail(){
        return this._email;
    }

    public void setEmail(String email){
        this._email = email;
    }
}
