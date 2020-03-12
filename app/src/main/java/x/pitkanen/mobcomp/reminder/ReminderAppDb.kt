package x.pitkanen.mobcomp.reminder

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Reminder::class], version = 3)
abstract class ReminderAppDb : RoomDatabase() {
    abstract fun reminderDao() : ReminderDao




}