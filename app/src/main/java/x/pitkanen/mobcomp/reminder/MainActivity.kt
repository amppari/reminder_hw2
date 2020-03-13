package x.pitkanen.mobcomp.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.NotificationCompat
import androidx.room.Room

import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        var fabsOpened = false

        addItemFab.setOnClickListener {
            if (fabsOpened) {
                fabsOpened = false
                mapFab.animate().translationY(0f)
                timerFab.animate().translationY(0f)
            } else {
                fabsOpened = true
                mapFab.animate().translationY(-resources.getDimension(R.dimen.fab_shift_66))
                timerFab.animate().translationY(-resources.getDimension(R.dimen.fab_shift_132))
            }
        }

        mapFab.setOnClickListener {
            val intent = Intent(applicationContext, MapActivity::class.java)
            startActivity(intent)
        }

        timerFab.setOnClickListener {
            val intent = Intent(applicationContext, TimeActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    // TODO Testing menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // TODO Testing menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshList() {
        doAsync {
            val db = Room.databaseBuilder(
                applicationContext,
                ReminderAppDb::class.java,
                "reminders").build()
            val reminders = db.reminderDao().getReminders()
            db.close()

            uiThread {

                if(reminders.isNotEmpty()){
                    val reminderAdapter = ReminderAdapter(applicationContext, reminders)
                    mainList.adapter = reminderAdapter
                }
                else
                    toast("No active reminders")

            }

        }
    }

    companion object {
      fun showNotification(context: Context, message: String) {
          val CHANNEL_ID = "REMINDER_NOTIFICATION_CHANNEL"
          var notificationId = 1736
          notificationId += Random(notificationId).nextInt(1, 30)

          val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
              .setSmallIcon(R.drawable.ic_alarm)
              .setContentTitle(context.getString(R.string.app_name))
              .setContentText(message)
              .setStyle( NotificationCompat.BigTextStyle().bigText(message))
              .setPriority(NotificationCompat.PRIORITY_DEFAULT)

          val notificationManager =
              context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              val channel = NotificationChannel(
                  CHANNEL_ID,
                  context.getString(R.string.app_name),
                  NotificationManager.IMPORTANCE_DEFAULT
              ).apply {
                  description = context.getString(R.string.app_name)
              }
              notificationManager.createNotificationChannel(channel)
          }
          notificationManager.notify(notificationId, notificationBuilder.build())

      }

    }


}
