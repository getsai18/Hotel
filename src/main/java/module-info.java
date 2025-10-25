module dev.codegets.project.hotel {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires java.base;

    exports dev.codegets.project.hotel;

    exports dev.codegets.project.hotel.models;

    exports dev.codegets.project.hotel.utils;

    opens dev.codegets.project.hotel.controllers to javafx.fxml;

    opens dev.codegets.project.hotel to javafx.fxml;

    opens dev.codegets.project.hotel.models to javafx.base;


}