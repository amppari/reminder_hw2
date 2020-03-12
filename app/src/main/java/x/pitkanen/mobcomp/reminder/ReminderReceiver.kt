package x.pitkanen.mobcomp.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.room.Room
import org.jetbrains.anko.doAsync


class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val uid = intent.getIntExtra("uid", 0)
        val text = intent.getStringExtra("message").toString()
        //context.toast(text)

        MainActivity.showNotification(context, text!!)

        val notificationPath: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ring = RingtoneManager.getRingtone(context, notificationPath)
        ring.play()

        // Remove reminder from the db after it has been shown
        doAsync {
            val db = Room.databaseBuilder(
                context,
                ReminderAppDb::class.java,
                "reminders"
            ).build()
            db.reminderDao().delete(uid)
            db.close()
        }
    }

}