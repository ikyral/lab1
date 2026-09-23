module life.simulation {
    requires javafx.controls;
    requires javafx.graphics;

    opens app to javafx.graphics;
    opens agent to javafx.graphics;
    opens environment to javafx.graphics;

    exports app;
    exports agent;
    exports environment;
}