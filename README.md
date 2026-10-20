# Altus Task Flow

## Desktop reminders

Task reminders are delivered by the Java application through the operating system's
system tray (including Windows), not through browser notifications. No browser
notification permission is required, and closing the browser does not stop reminders.

Keep the Java application running in your logged-in desktop session. The header
shows whether the system tray is available. Headless servers and sessions without a
system tray cannot display notifications; other app features remain available.
Windows notification settings and Do Not Disturb can silence or hide reminders.

Reminder times use `Asia/Tehran`, matching the Persian date/time picker. The app
checks every 15 seconds and delivers each due reminder once per app run, within the
existing one-minute due window. Old reminders are not replayed after a long shutdown;
restarting within that window may deliver the reminder again.

Restart the Java app after updating to activate desktop reminders. If deployed on
another machine, notifications appear on that host's desktop, not a remote browser.
