/*
 *  Copyright (C) 2017 Dirk Lemmermann Software & Consulting (dlsc.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.calendarfx.app;
import java.sql.*;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.DateTime;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.Calendar.Style;
import com.calendarfx.model.CalendarEvent;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.model.Interval;
import com.calendarfx.view.CalendarView;
import java.awt.SystemTray;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;


public class CalendarApp extends Application {
    int flag=0;
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost/Event_Calendar";

   //  Database credentials
   static final String USER = "root";
   static final String PASS = "12345";
   
   LinkedHashMap<Entry,Set> map = new LinkedHashMap<>();
   final ArrayList<PreparedStatement> preparedStmt=new ArrayList<>();
   
   ArrayList<Calendar> Calendars=new ArrayList<>();
    @Override
    public void start(Stage primaryStage) throws Exception {
        
        
        CalendarView calendarView = new CalendarView();
        
        Calendar imp_dates=new Calendar();
        Calendar university=new Calendar();
        Calendar festival=new Calendar();
        Calendar General=new Calendar();
        
         Calendars.add(university);
        Calendars.add(imp_dates);
         Calendars.add(festival);
         Calendars.add(General);
        
       
         imp_dates.setShortName("I");
        university.setShortName("U");
        festival.setShortName("F");
        General.setShortName("G");
        university.setName("University");
        imp_dates.setName("Important");
        festival.setName("Festival");
        General.setName("General");
       
        university.setStyle(Style.STYLE6);
        imp_dates.setStyle(Style.STYLE5);
        General.setStyle(Style.STYLE4);

        CalendarSource familyCalendarSource = new CalendarSource("Family");
        familyCalendarSource.getCalendars().addAll(imp_dates,university,festival,General);

        calendarView.getCalendarSources().setAll(familyCalendarSource);
        calendarView.setRequestedTime(LocalTime.now());
        calendarView.setTranslateY(-40);
        Extract_From_database();
         
        HBox h=new HBox();   
       Button bt =new Button("Save");
        bt.setMinWidth(70);
        h.setTranslateX(750);
        h.getChildren().addAll(bt);
        h.setTranslateY(38);
       
        
     FlowPane f=new FlowPane();
    f.setStyle("-fx-background-color: white");
    f.setTranslateY(720);
    f.setHgap(1200);
    f.setMinWidth(805);
    f.setMinHeight(70);
      
       HBox hbox=new HBox();
       
       hbox.setMinHeight(40);
       hbox.setTranslateY(25);
       hbox.setSpacing(10);
       hbox.setAlignment(Pos.BOTTOM_RIGHT);
       
         f.getChildren().addAll(h,hbox);
         
           VBox stackPane = new VBox();
           stackPane.getChildren().addAll(f,calendarView); 
        
        
        calendarView.setShowAddCalendarButton(false);
        calendarView.setShowSourceTray(false);
        calendarView.setShowPrintButton(false);
       
       ArrayList<EventHandler<CalendarEvent>> handler=new ArrayList<>();
        for(int i=0;i<Calendars.size();++i)
       {
        handler.add(evt -> foo(evt));
       Calendars.get(i).addEventHandler(handler.get(i));
       }
      Thread updateTimeThread = new Thread("Calendar: Update Time Thread") {
            @Override
            public void run() {
                while (true) {
                    Platform.runLater(() -> {
                        calendarView.setToday(LocalDate.now());
                        calendarView.setTime(LocalTime.now());
                    });

                    try {
                        // update every 10 seconds
                        sleep(10000);
                    } catch (InterruptedException e) {
                    }

                }
            }
        };
   
      
      
        updateTimeThread.setPriority(Thread.MIN_PRIORITY);
        updateTimeThread.setDaemon(true);
        updateTimeThread.start();

           
        Scene scene = new Scene(stackPane);
       
        
         
        bt.setOnMouseClicked(e->{
                    UpdateEntries();
            });
          
         
         Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        primaryStage.setTitle("Calendar");
        primaryStage.setScene(scene);
        primaryStage.setWidth(screenBounds.getWidth()+15);
        primaryStage.setHeight(screenBounds.getHeight()-32);
        primaryStage.centerOnScreen();
        primaryStage.show();
       
    }

    
    void Extract_From_database()
    {
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
      String query1 ="select * from recurring_event_pattern where Event_id = ?";
      
      preparedStmt.add(conn1.prepareStatement(query));
      preparedStmt.add(conn1.prepareStatement(query1));
       preparedStmt.get(0).setString(1,"event");
      ResultSet data = preparedStmt.get(0).executeQuery();
      
      while(data.next()){
        
          flag=1;
         String id  = Integer.toString(data.getInt("id"));
        String title = data.getString("event_title");
         String location = data.getString("location");
         String category=data.getString("Category");
         Date sdate=data.getDate("start_date");
         Date edate=data.getDate("end_date");
         Time stime=data.getTime("start_time");
         Time etime=data.getTime("end_time");
         Boolean ire=data.getBoolean("is_recurring_event");
         Boolean ifde=data.getBoolean("is_full_day_event");
         Boolean imde=data.getBoolean("is_multi_day_event");
        Entry NewEntry= new Entry(title,new Interval(sdate.toLocalDate(),stime.toLocalTime(), edate.toLocalDate(), etime.toLocalTime()));
          for(int i=0;i<Calendars.size();++i)
         {
             if(Calendars.get(i).getName().equalsIgnoreCase(category))
                Calendars.get(i).addEntry(NewEntry);
         }
         
         NewEntry.setId(id);
         NewEntry.setLocation(location);
         NewEntry.setFullDay(ifde);
        
      if(ire)
      { 
          preparedStmt.get(1).setInt(1,Integer.parseInt(id));
          ResultSet data1 = preparedStmt.get(1).executeQuery();
          String s;
          while(data1.next())
          { int Interval=data1.getInt("Separation_count");
          int count=data1.getInt("Maximum_occurrences");
          String dow=data1.getString("Day_of_week");
          s ="FREQ="+data1.getString("recurrence_type")+";";
              
          if(Interval!=0 || (count!=0||dow!=null))   
              s=s.concat("INTERVAL="+Integer.toString(Interval)+";");
          if(count!=0)          
              s=s.concat("COUNT="+Integer.toString(count)+";");
           if(dow!=null)
               s=dow!=null?s.concat("BYDAY="+dow):s;
           else
               s=s.substring(0,s.length()-1);
          
           NewEntry.setRecurrenceRule(s);
          }
        }
      
      }
      
 preparedStmt.clear();
      
      flag=0;
   }catch(SQLException | ClassNotFoundException se){se.printStackTrace();}
    
   finally{
      
      try{
         if(stmt!=null)
            stmt.close();
      }catch(SQLException se2){
      }
   }   
    }
    
    
    
    
    
    
     void foo(CalendarEvent er)
     {
         if(flag==0)
         {
            Entry d=er.getEntry();
           
            if(er.isEntryAdded()==true)
          { 
          AddEntry(d);
          map.put(d,new LinkedHashSet<CalendarEvent>());
          }
             
          else if(er.isEntryRemoved()==true)
          {
              RemoveEntry(d.getId());
              map.remove(d);
          }
          
          else 
          {
             if(!map.containsKey(d))
                map.put(d,new LinkedHashSet<CalendarEvent>());
             
             map.get(d).add(er.getEventType());
           }
         }
     }
     
     
     void RemoveEntry(String Id)
     {
         
         Connection conn=null;
   Statement stmt = null;
   try{
      
      Class.forName("com.mysql.jdbc.Driver");
      System.out.println("Connecting to database...");
      conn = DriverManager.getConnection(DB_URL, USER, PASS);
      System.out.println("Creating database...");
      stmt = conn.createStatement();
      
     final Connection conn1=conn;
      String query = "{ call delete_row(?,?)}";
      
             try{
                 preparedStmt.add(conn1.prepareStatement(query));
                 preparedStmt.get(0).setString(1,"event");
                 preparedStmt.get(0).setInt(2,Integer.parseInt(Id));
                 preparedStmt.get(0).execute();
                 preparedStmt.remove(0);
                }
catch(SQLException s)
                  {
                  s.printStackTrace();
                  }
             
                              
   }catch(SQLException | ClassNotFoundException se){
             
   }
    finally{
      
      try{
         if(stmt!=null)
            stmt.close();
      }catch(SQLException se2){
      }
   } 
 }
     
     
     void AddEntry(Entry E)
     {
      int f=0;
         
      for(int i=0;i<Calendars.size();++i)
           f+=Calendars.get(i).findEntriesId(E.getId()).size();
         
         if(f>1)
            E.getCalendar().removeEntry(E);
                  
        
         else
         {
             Connection conn=null;
             Statement stmt = null;
   try{
      
      Class.forName("com.mysql.jdbc.Driver");
      System.out.println("Connecting to database...");
      conn = DriverManager.getConnection(DB_URL, USER, PASS);
      System.out.println("Creating database...");
      stmt = conn.createStatement();
      stmt=conn.createStatement();  
     
       String query = "{ call Insert_Event(?,?,?,?,?,?,?) }";
       
      final Connection conn1=conn;
        
       
      try{
      preparedStmt.add(conn1.prepareStatement(query));
      preparedStmt.get(0).setInt(1,Integer.parseInt(E.getId()));
      preparedStmt.get(0).setString(2,E.getTitle());
      preparedStmt.get(0).setDate(3,Date.valueOf(E.getStartDate()));
      preparedStmt.get(0).setDate(4,Date.valueOf(E.getEndDate()));
      preparedStmt.get(0).setTime(5,Time.valueOf(E.getStartTime()));
      preparedStmt.get(0).setTime(6,Time.valueOf(E.getEndTime()));
      preparedStmt.get(0).setString(7,E.getCalendar().getName());
      preparedStmt.get(0).execute();
      preparedStmt.remove(0);
       }
catch(SQLException s)
                  {
                      s.printStackTrace();
                  }
                              
   }catch(SQLException | ClassNotFoundException se){
         }
    
    finally{
    
      try{
         if(stmt!=null)
            stmt.close();
      }catch(SQLException se2){
      }
   }
  }
 }
     
     void UpdateEntries()
     {
        Connection conn=null;
       Statement stmt = null;
   try{
      
      Class.forName("com.mysql.jdbc.Driver");
      System.out.println("Connecting to database...");
      conn = DriverManager.getConnection(DB_URL, USER, PASS);
      System.out.println("Creating database...");
      stmt = conn.createStatement();
      stmt=conn.createStatement();  
      final Connection conn1=conn;
     map.entrySet().forEach( entry -> {
      if(!entry.getValue().isEmpty())
      {
          entry.getValue().forEach( element -> {
       
        if(element.equals(CalendarEvent.ENTRY_TITLE_CHANGED))
        {
            
            String query = "{ call Update_Events_1(?,?,?) }";
           try{
               
      preparedStmt.add(conn1.prepareStatement(query));
      preparedStmt.get(0).setString(1,"event_title");
      preparedStmt.get(0).setString(2,entry.getKey().getTitle());
      preparedStmt.get(0).setInt(3,Integer.parseInt(entry.getKey().getId()));
      preparedStmt.get(0).execute();
      preparedStmt.clear();
      }
catch(SQLException s)
                  {
                  s.printStackTrace();
                  }
           
        }
        
       else if(element.equals(CalendarEvent.ENTRY_INTERVAL_CHANGED))
                {
                   String query = "{ call Update_Events_2(?,?,?,?,?) }";
           try{
      preparedStmt.add(conn1.prepareStatement(query));
      preparedStmt.get(0).setTime(1,Time.valueOf(entry.getKey().getStartTime()));
      preparedStmt.get(0).setTime(2,Time.valueOf(entry.getKey().getEndTime()));
      preparedStmt.get(0).setDate(3,Date.valueOf(entry.getKey().getStartDate()));
      preparedStmt.get(0).setDate(4,Date.valueOf(entry.getKey().getEndDate()));
      preparedStmt.get(0).setInt(5,Integer.parseInt(entry.getKey().getId()));
      preparedStmt.get(0).execute();
      preparedStmt.clear();
       }
catch(SQLException s)
                  {
                    s.printStackTrace();
                  } 
                }
        
        
        else if(element.equals(CalendarEvent.ENTRY_FULL_DAY_CHANGED))
                {
                   String query = "{ call Update_Events(?,?,?) }";
           try{
      preparedStmt.add(conn1.prepareStatement(query));
      preparedStmt.get(0).setString(1,"is_full_day_event");
      preparedStmt.get(0).setBoolean(2,entry.getKey().isFullDay());
      preparedStmt.get(0).setInt(3,Integer.parseInt(entry.getKey().getId()));
      preparedStmt.get(0).execute();
      preparedStmt.clear();
       
           if(entry.getKey().isFullDay())
           {
                    String query1 = "{ call Update_Events_2(?,?,?,?,?) }";
          
      preparedStmt.add(conn1.prepareStatement(query1));
      preparedStmt.get(0).setTime(1,Time.valueOf(LocalTime.MIN));
      preparedStmt.get(0).setTime(2,Time.valueOf(LocalTime.MAX));
      preparedStmt.get(0).setDate(3,Date.valueOf(entry.getKey().getStartDate()));
      preparedStmt.get(0).setDate(4,Date.valueOf(entry.getKey().getEndDate()));
      preparedStmt.get(0).setInt(5,Integer.parseInt(entry.getKey().getId()));
      preparedStmt.get(0).execute();
      preparedStmt.clear();
           }
          }
catch(SQLException s)
                  {  s.printStackTrace();
                  } 
                }
        
        else if(element.equals(CalendarEvent.ENTRY_RECURRENCE_RULE_CHANGED))
        {
           
           String query = "{call Update_Events(?,?,?)}";
           try{
      preparedStmt.add(conn1.prepareStatement(query));
      preparedStmt.get(0).setString(1,"is_recurring_event");
      preparedStmt.get(0).setBoolean(2,entry.getKey().isRecurring());
      preparedStmt.get(0).setInt(3,Integer.parseInt(entry.getKey().getId()));
      preparedStmt.get(0).execute();
      preparedStmt.clear();
     
         if(entry.getKey().isRecurring())
           {
               String query1 = "{ call Update_Recurrence(?,?,?,?,?,?) }";
      ArrayList<String> str = new ArrayList<>(Arrays.asList(entry.getKey().getRecurrenceRule().split("[=;]")));
                  
      preparedStmt.add(conn1.prepareStatement(query1));
 
      preparedStmt.get(0).setInt(1,Integer.parseInt(entry.getKey().getId()));
      
      if(str.contains("INTERVAL")==true) 
      preparedStmt.get(0).setInt(2,Integer.parseInt(str.get(str.indexOf("INTERVAL")+1)));
      else
          preparedStmt.get(0).setNull(2,Types.INTEGER);
      
      if(str.contains("COUNT")==true) 
           preparedStmt.get(0).setInt(3,Integer.parseInt(str.get(str.indexOf("COUNT")+1)));
      else
          preparedStmt.get(0).setNull(3,Types.INTEGER);
    
      if(str.contains("BYDAY")==true)
        preparedStmt.get(0).setString(4,str.get(str.indexOf("BYDAY")+1));
      
      else
         preparedStmt.get(0).setString(4,null);
      
    
        if(entry.getKey().getRecurrenceEnd().getYear()!=999999999)
            preparedStmt.get(0).setDate(5,Date.valueOf(entry.getKey().getRecurrenceEnd()) );
        
     else
        {
            
            preparedStmt.get(0).setNull(5,Types.DATE);
        }
      
      
      preparedStmt.get(0).setString(6,str.get(1));
        
         
      
      preparedStmt.get(0).execute();
      preparedStmt.clear();
      
           }
        }
catch(SQLException s)
                  {
                      s.printStackTrace();
                  }
        }
        
        else if(element.equals(CalendarEvent.ENTRY_LOCATION_CHANGED))
        {
                 String query = "{ call Update_Events_1(?,?,?) }";
           try{
      preparedStmt.add(conn1.prepareStatement(query));
      preparedStmt.get(0).setString(1,"location");
      preparedStmt.get(0).setString(2,entry.getKey().getLocation());
      preparedStmt.get(0).setInt(3,Integer.parseInt(entry.getKey().getId()));
      preparedStmt.get(0).execute();
      preparedStmt.clear();
       }
catch(SQLException s)
                  {
                  s.printStackTrace();  }
        }
        
         if(entry.getKey().isMultiDay())
        {
                 String query = "{ call Update_Events(?,?,?) }";
           try{
      preparedStmt.add(conn1.prepareStatement(query));
      preparedStmt.get(0).setString(1,"is_multi_day_event");
      preparedStmt.get(0).setBoolean(2,entry.getKey().isMultiDay());
      preparedStmt.get(0).setInt(3,Integer.parseInt(entry.getKey().getId()));
      preparedStmt.get(0).execute();
      preparedStmt.clear();
       }
catch(SQLException s)
                  {
                  s.printStackTrace();  }
        }
      });
     } 
     
     });
   }catch(SQLException | ClassNotFoundException se){se.printStackTrace();
         }
    
    finally{
    
      try{
         if(stmt!=null)
            stmt.close();
      }catch(SQLException se2){
      }
   }   
         
    map.clear();
     }
     
     
     
    public static void main(String[] args) throws InterruptedException {
        
         if (SystemTray.isSupported()) {
            
        Timer time = new Timer(); // Instantiate Timer Object
        ScheduledTask1 st = new ScheduledTask1(); // Instantiate SheduledTask class
         ScheduledTask st1 = new ScheduledTask(); // Instantiate SheduledTask class
        time.schedule(st, 0, 60000); //1 min
        time.schedule(st1, 0, 1800000); //30 mins
         }
          else {
            System.err.println("System tray not supported!");
        }
        
        launch(args);
    }
}

 
class ScheduledTask1  extends TimerTask {
 
     static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
     static final String DB_URL = "jdbc:mysql://localhost/Event_Calendar";
     static final String USER = "root";
     static final String PASS = "12345";
     PreparedStatement preparedStmt,preparedStmt1;
     java.util.Date now; // to display current time

    @Override
    public void run() {
        now = new java.util.Date(); // initialize date
        RecurrenceRule rule = null;
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
      
      try{
      preparedStmt=conn1.prepareStatement(query);
      preparedStmt.setString(1,"recurring_event_pattern");
      ResultSet data = preparedStmt.executeQuery();
      
      while(data.next()){   
         
          String rrule=new String();
          int id=data.getInt("Event_id");
         
          String freq=data.getString("Recurrence_type");
          rrule="FREQ="+freq;
          
          String e_date=data.getString("Recur_end_date");
          Date e=data.getDate("Recur_end_date");
                  
          String interval=data.getString("Separation_count"); 
         if(interval!=null)
             rrule=rrule.concat(";INTERVAL="+interval);
         
         String count=data.getString("Maximum_occurrences");
          if(count!=null) 
            rrule=rrule.concat(";COUNT="+count);            
         
          String dow=data.getString("Day_of_week");
          if(dow!=null) 
            rrule=rrule.concat(";BYDAY="+dow); 
          try{
              
          rule = new RecurrenceRule(rrule);
          DateTime start = new DateTime(LocalDate.now().getYear(),LocalDate.now().getMonthValue(),LocalDate.now().getDayOfMonth());
         RecurrenceRuleIterator it = rule.iterator(start);
         int maxInstances = 100; // limit instances for rules that recur forever
         
        while (it.hasNext() && (!rule.isInfinite() || maxInstances-- > 0))
        {
             
            DateTime nextInstance = it.nextDateTime();
             String st=new String();
             if(nextInstance.getMonth()==0)
             st=st.concat(String.valueOf(nextInstance.getYear())+"-"+String.valueOf(nextInstance.getMonth()+12)+"-"+String.valueOf(nextInstance.getDayOfMonth()));
             else
                 st=st.concat(String.valueOf(nextInstance.getYear())+"-"+String.valueOf(nextInstance.getMonth())+"-"+String.valueOf(nextInstance.getDayOfMonth())); 
            
             java.sql.Date st_date= java.sql.Date.valueOf(st);
               java.sql.Date et_date=st_date;   
                  
               String query1 ="{call add_recurrent_data(?,?,?)}";
      
            try{
                 preparedStmt=conn1.prepareStatement(query1);
                 preparedStmt.setInt(1,id);
              
                 preparedStmt.setDate(2,st_date);
                 preparedStmt.setDate(3,et_date);
                 preparedStmt.execute();
                 
             }catch(SQLException s)
                  {
                      s.printStackTrace();
                  }
       } 
      }catch (InvalidRecurrenceRuleException ex) {
              Logger.getLogger(ScheduledTask1.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
    }catch(SQLException s)
                  {
                      s.printStackTrace();
                  }
}catch(SQLException | ClassNotFoundException se){se.printStackTrace();}
    
   finally{
       try{
         if(stmt!=null)
            stmt.close();
      }catch(SQLException se2){}}}}