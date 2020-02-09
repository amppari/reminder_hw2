package x.pitkanen.mobcomp.reminder

import android.R.attr.path
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import org.jetbrains.anko.toast


class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val text = intent.getStringExtra("message").toString()
        context.toast(text)

        val notificationPath: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ring = RingtoneManager.getRingtone(context, notificationPath)
        ring.play()
    }

}