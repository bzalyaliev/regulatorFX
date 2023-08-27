module com.github.bzalyaliev.regulatorfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires regulator;
    requires org.json;

    opens com.github.bzalyaliev.regulatorfx to javafx.fxml;
    exports com.github.bzalyaliev.regulatorfx;
    exports com.github.bzalyaliev.regulatorfx.controller;
    opens com.github.bzalyaliev.regulatorfx.controller to javafx.fxml;
}