# Wakey Wakey Android Client

Wakey Wakey is a network alarm clock system. The webapp side of the project can be found here:

[https://github.com/victor-chew/wakey-wakey](https://github.com/victor-chew/wakey-wakey)

This repository is for the Android client that reads the alarm schedule from the backend database and basically functions as an alarm clock.

For something as humble as an  alarm clock app, it actually turned out to be rather difficult to implement. I originally wanted to write it in Javascript using a toolkit such as Cordova, but after much research, it did not appear to be very feasible. There is a whole lot of platform-specific stuff to deal with, such as:

* how to run periodically in the background to schedule alarams

* how to activate the alarm, even if the application is not in the foreground, or the phone is asleep

* how to setup everything again after a reboot etc.

So I decided to go native instead. This client is written using Java under Android Studio 4.0 The target SDK is 19, because I need to run the client on old KitKat and Lollipop phones.

The UI is extremely plain and simple. The client will turn the volume up to maximum, so the alarms will ring even if the phone has been accidentally muted or airplane mode has been switched on (yeah, I can't remember how many times my kids have done that to me). 

Once an alarm goes off, it will continue for 10 minutes, or until the alarm is remotely disabled on the webapp. The code is actually there to dismiss the alarm by touching the screen, but I have commented it out!

Replication happens in the background to a local database, so the alarm clock will function even if wifi is down (provided the changes have already been replicated to the local instance before the network went down).

# Screenshots
![Screenshot](https://wakey.randseq.org/screenshots/screenshot03.png "Screenshot 1")

# Download
![Ver 0.1.0 APK](https://wakey.randseq.org/android/wakeywakey-0.1.0.apk "Ver 0.1.0 APK")

# Libraries used
* [Cloudant Sync for Android](https://github.com/cloudant/sync-android) For syncing with the couchdb database.

# Live Demo
The webapp can be accessed here:
[https://wakey.randseq.org/](https://wakey.randseq.org/ "Wakey Wakey Live Demo")
