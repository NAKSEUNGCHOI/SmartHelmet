/**
 * @author Nakseung Choi
 * @date 12/1/2022
 * @description User class
 */
package com.example.ble_keyboard;

public class User {

    public String fullName, email, emergencyContact;

    // empty constructor
    public User(){

    }

    public User(String fullName, String email, String emergencyContact){
        this.fullName = fullName;
        this.email = email;
        this.emergencyContact = emergencyContact;
    }

}
