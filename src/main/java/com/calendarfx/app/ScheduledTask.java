/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.calendarfx.app;
import java.awt.AWTException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.ParseException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author AMIT JAIN
 */
public class ScheduledTask extends TimerTask{
    NewClass td = new NewClass();
     // to display current time
static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost/Event_Calendar";
      PreparedStatement preparedStmt;
   //  Database credentials
   static final String USER = "root";
   static final String PASS = "12345";
    // Add your task here
    @Override
    public void run() {
         // initialize date
         Connection conn=null;
        Statement stmt = null;
        try{
            
      
      Class.forName("com.mysql.jdbc.Driver");
      System.out.println("Connecting to database...");
      conn = DriverManager.getConnection(DB_URL, USER, PASS);
      System.out.println("Creating database...");
      stmt = conn.createStatement();
      final Connection conn1=conn;
     
      String query ="{call select_all(?)}";
     
      preparedStmt=conn1.prepareStatement(query);
     preparedStmt.setString(1,"recent");
      ResultSet data = preparedStmt.executeQuery();
      
      while(data.next()){
          
          String title = data.getString("event_title");
          String location = data.getString("location");
          java.sql.Date sdate=data.getDate("start_date");
          Time stime=data.getTime("start_time");
      if(stime.toLocalTime().isBefore(java.time.LocalTime.now().plusMinutes(60))&&stime.toLocalTime().isAfter(java.time.LocalTime.now().minusMinutes(1)))
      {  System.out.print("Yes");
          try{
          td.displayTray(title,location,stime);     
        } catch (AWTException ex) {
            Logger.getLogger(ScheduledTask.class.getName()).log(Level.SEVERE, null, ex);
        }     catch (ParseException ex) {
                  Logger.getLogger(ScheduledTask.class.getName()).log(Level.SEVERE, null, ex);
              }
      }
      }
     }catch(SQLException | ClassNotFoundException se){se.printStackTrace();}
    
   finally{
      
      try{
         if(stmt!=null)
            stmt.close();
      }catch(SQLException se2){
      }
    }
}
}