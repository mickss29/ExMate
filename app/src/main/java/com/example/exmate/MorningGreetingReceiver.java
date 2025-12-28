package com.example.exmate;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MorningGreetingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                String name = snapshot.child("name").getValue(String.class);
                if (name == null || name.trim().isEmpty()) {
                    name = "there";
                }

                sendGreeting(context, name);
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void sendGreeting(Context context, String name) {

        Notification notification =
                new NotificationCompat.Builder(context, "morning_greeting")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("☀️ Good Morning, " + name)
                        .setContentText("I’m ready to save your transactions today.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build();

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(7001, notification);
    }
}
