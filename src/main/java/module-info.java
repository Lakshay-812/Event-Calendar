
module com.calendarfx.app {
    requires transitive javafx.graphics;
    requires java.sql;
    requires javafx.controls;
    requires com.calendarfx.view;
    requires javafx.base;
    exports com.calendarfx.app;
    requires java.desktop;
    requires lib.recur;
    requires rfc5545.datetime;
       }
  


