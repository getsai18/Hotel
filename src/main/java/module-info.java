module dev.codegets.project.hotel {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens dev.codegets.project.hotel to javafx.fxml;
    exports dev.codegets.project.hotel;
}