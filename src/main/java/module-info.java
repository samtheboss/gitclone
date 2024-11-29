module com.smartapps.gitcontrolsystem {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.smartapps.gitcontrolsystem to javafx.fxml;
    exports com.smartapps.gitcontrolsystem;
    exports com.smartapps.gitcontrolsystem.controllers;
    opens com.smartapps.gitcontrolsystem.controllers to javafx.fxml;
}