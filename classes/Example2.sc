// Define the superclass
// Define the superclass
Superclass {
    classvar <>classVar; // Define a class variable with getter and setter

    // Constructor to initialize the class variable
    *new {
        classVar = 100; // Set a default value for the class variable
        ^super.new.init;
    }
}

// Define the subclass
Subclass : Superclass {
    var instanceVar; // Define an instance variable

    // Constructor to initialize the instance variable
    *new { |initValue|
        ^super.new.init(initValue);
    }

    // Override the init method to accept an initial value
    init { |initValue|
        instanceVar = initValue; // Set the instance variable to the passed value

        // Additional initialization or setup using both variables
        ("Class Variable: " ++ classVar).postln;
        ("Instance Variable: " ++ instanceVar).postln;
    }

    // Method to display the inherited and instance variables
    showVariables {
        ("Class Variable: " ++ classVar).postln;
        ("Instance Variable: " ++ instanceVar).postln;
    }
}