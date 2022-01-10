# Event-Calendar

A desktop calendar application that stores various events like your day-to-day events, important dates,
holidays, festivals, university events etc. One of the main features of this application is that it gives you 
reminder before start of any planned event (an alarm). The events handled by the application could be 
single day, full-day, multi-day and recurrent. Recurrent events form the most important category in our 
project. The application uses Java/Javafx for Front-End, JDBC to access MySQL database, namely 
‘Event_Calendar’, in this case.

## Front-End

Calendarfx is used to design calendar. The main features of our calendar are:

• Calendar consists of day, week, month, and year view.

• Events can be of be in any one category out of Festival, Important, University etc.

• Events include information like Title, Location, Start Date, End Date, Start Time, End Time, 
Recurrence etc.

• Events can be updated and deleted according to users need.

• Windows Notification is received as a Reminder of planned event.

![image](https://user-images.githubusercontent.com/82465969/148736916-abbfc06e-dc42-45ce-819a-9e77349f8feb.png)

Month View - 

![image](https://user-images.githubusercontent.com/82465969/148737052-6f88d8fb-b02b-4d83-bf60-35e7bc4fb02d.png)

## Back-End

Our database consists of 3 tables, 2 views, 1 trigger, 1 event, 8 stored procedures.
The schema is as shown below: 

![image](https://user-images.githubusercontent.com/82465969/148737170-97c940d8-3da1-4cbc-9a3d-8d6aa943c96b.png)

## Tables 

Event: Consists about info regarding events

Recurring_Event_Pattern: Consists of iCal parameters for rrule for associated recurrent events.

Recurrent_event: Consists of first 100 instances of recurring events.

## Views 

All_event: A view created by union of event and join of event and recurrent_event tables. Shows all 
events from both tables.

Recent: Contains all events planned for current day. Used to provide reminders. 

## Trigger

recurremce_delete: to delete data from recurrence_event_pattern and recurrent_event tables if recurrence 
is removed from an entry.

## Event 

Reminder: Event that runs after every 1 day to remove old events and keep entries planned for that day
only.

## Stored Procedures 

Insert_event: insert new event entry in calendar.

Update_Events, Update_Events_1, Update_Events_2: To update table event depending upon which 
parameters have been changed by user.

Update_Recurrence: To insert rrule parameters in recurring_event_pattern

Select_all: select all rows from the parameter table or view.

Delete_row : delete row for given id for given table or view.

Add_recurrent_data: To add first 100 or less recurrent entries of each recurring event.

## Limitations 

1) Can't drag from one calendar category to other directly.
2) Can't update recurrence directly; Make recurrence none and then change recurrence
3) Recurrent_events are single day only.
4) Recurrent exception events are not handled.
5) Multi-day events not handled in too much depth
