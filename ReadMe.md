# KoKoRoKo
Android App that interfaces with a wake up routine off-site, manages wake up time and alarm cancellation.

This app is a result of not having a way to input variable hours and minutes into a get web request to notify a webhook Interfaced wake up system

In case you'd like to implement it yourself the wake-up service interface gets sent a get request to an user in-app defined webhook as follows:

URL get request = "${YourWebhook}?hours=${inputHours}&minutes=${inputMinutes}" 

If your webhook was "https://mywebhook", the app would make a get request to https://mywebhook?hours=10&minutes=10 if the user chose 10:10 AM as the wake up time

BTW if you're going to try and do this yourself note that when an alarm cancellation is selected the app would make a request to https://mywebhook?hours=-999&minutes=-999, note that the parameters hours and minutes are -999 to signal the off-site service to cancel the current alarm.

At last, in case you still want to do it, you can implement the off-site wake up service as you will but it was pretty simple to me to implement it in an android app called macrodroid with the webhook action as trigger.

That's it, hope it was interesting 

PD: This is my first app, so please, don't judge 😅😅
