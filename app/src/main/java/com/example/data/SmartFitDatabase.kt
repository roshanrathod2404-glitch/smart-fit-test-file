package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ContactEntity::class, MessageEntity::class, ReminderEntity::class, HistoryEntity::class, ChatRoomEntity::class, CommunityEntity::class], version = 8, exportSchema = false)
abstract class SmartFitDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun reminderDao(): ReminderDao
    abstract fun historyDao(): HistoryDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun communityDao(): CommunityDao


    companion object {
        @Volatile
        private var INSTANCE: SmartFitDatabase? = null

        fun getDatabase(context: Context): SmartFitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartFitDatabase::class.java,
                    "smartfit_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val contactDao = database.contactDao()
                        if (contactDao.getContactCount() == 0) {
                            populateInitialData(database)
                        }
                    }
                }
            }

            suspend fun populateInitialData(database: SmartFitDatabase) {
                val contactDao = database.contactDao()
                val messageDao = database.messageDao()
                val reminderDao = database.reminderDao()
                val historyDao = database.historyDao()

                val c1 = contactDao.insertContact(
                    ContactEntity(
                        name = "Roshan",
                        phoneNumber = "+1 (555) 100-2001",
                        email = "roshan@wellness.com",
                        avatarInitials = "RO",
                        lastMessage = "Hey! Let's review the workout progress.",
                        lastMessageTime = "11:45 AM",
                        onlineStatus = "Online",
                        isPinned = true,
                        unreadCount = 1
                    )
                )

                val c2 = contactDao.insertContact(
                    ContactEntity(
                        name = "Manisha",
                        phoneNumber = "+1 (555) 200-3002",
                        email = "manisha@fitness.org",
                        avatarInitials = "MA",
                        lastMessage = "Sent the yoga session schedule.",
                        lastMessageTime = "10:15 AM",
                        onlineStatus = "Online",
                        isPinned = false,
                        unreadCount = 2
                    )
                )

                val c3 = contactDao.insertContact(
                    ContactEntity(
                        name = "Sanjeev",
                        phoneNumber = "+1 (555) 300-4003",
                        email = "sanjeev@health.net",
                        avatarInitials = "SA",
                        lastMessage = "Hydration milestone reached!",
                        lastMessageTime = "Yesterday",
                        onlineStatus = "Offline",
                        isPinned = false,
                        unreadCount = 0
                    )
                )

                val c4 = contactDao.insertContact(
                    ContactEntity(
                        name = "Sir",
                        phoneNumber = "+1 (555) 400-5004",
                        email = "sir@wellness.io",
                        avatarInitials = "SI",
                        lastMessage = "Great job on the cardiovascular endurance test.",
                        lastMessageTime = "Oct 12",
                        onlineStatus = "Online",
                        isPinned = false,
                        unreadCount = 0
                    )
                )

                // Two-way messages for Roshan (c1)
                messageDao.insertMessage(MessageEntity(contactId = c1, senderName = "Roshan", messageText = "Hey! Let's review the workout progress.", timestamp = "11:40 AM", isSentByMe = false, isStarred = true))
                messageDao.insertMessage(MessageEntity(contactId = c1, senderName = "You", messageText = "Ready when you are, Roshan!", timestamp = "11:42 AM", isSentByMe = true, isStarred = false))
                messageDao.insertMessage(MessageEntity(contactId = c1, senderName = "Roshan", messageText = "Here is the workout plan PDF.", timestamp = "11:45 AM", isSentByMe = false, isStarred = true, fileType = "PDF", fileName = "Workout_Plan_Roshan.pdf"))

                // Two-way messages for Manisha (c2)
                messageDao.insertMessage(MessageEntity(contactId = c2, senderName = "Manisha", messageText = "Don't forget the morning stretch session.", timestamp = "9:00 AM", isSentByMe = false, isStarred = false))
                messageDao.insertMessage(MessageEntity(contactId = c2, senderName = "You", messageText = "Will be there bright and early!", timestamp = "9:05 AM", isSentByMe = true, isStarred = true))
                messageDao.insertMessage(MessageEntity(contactId = c2, senderName = "Manisha", messageText = "Sent the yoga session schedule.", timestamp = "10:15 AM", isSentByMe = false, isStarred = false, fileType = "Document", fileName = "Yoga_Schedule.docx"))

                // Two-way messages for Sanjeev (c3)
                messageDao.insertMessage(MessageEntity(contactId = c3, senderName = "Sanjeev", messageText = "Hydration milestone reached!", timestamp = "Yesterday", isSentByMe = false, isStarred = false))
                messageDao.insertMessage(MessageEntity(contactId = c3, senderName = "You", messageText = "Awesome! Keep it up.", timestamp = "Yesterday", isSentByMe = true, isStarred = true))

                // Two-way messages for Sir (c4)
                messageDao.insertMessage(MessageEntity(contactId = c4, senderName = "Sir", messageText = "Great job on the cardiovascular endurance test.", timestamp = "Oct 12", isSentByMe = false, isStarred = false))
                messageDao.insertMessage(MessageEntity(contactId = c4, senderName = "You", messageText = "Thank you for the guidance, Sir.", timestamp = "Oct 12", isSentByMe = true, isStarred = false))

                // Initial history items
                historyDao.insertHistory(HistoryEntity(contactId = c1, type = "PDF", title = "Workout_Plan_Roshan.pdf", subtitle = "Adobe PDF • 1.2 MB", senderName = "Roshan", dateText = "10 July, 2026", timeText = "11:45 AM"))
                historyDao.insertHistory(HistoryEntity(contactId = c2, type = "Document", title = "Yoga_Schedule.docx", subtitle = "Word document • 480 KB", senderName = "Manisha", dateText = "10 July, 2026", timeText = "10:15 AM"))
            }
        }
    }
}
