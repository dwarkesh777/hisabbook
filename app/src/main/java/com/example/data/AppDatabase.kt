package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(entities = [Expense::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.expenseDao())
                }
            }
        }

        suspend fun populateDatabase(expenseDao: ExpenseDao) {
            val cal = Calendar.getInstance()
            
            // Current month/day timestamps
            val now = cal.timeInMillis
            
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = cal.timeInMillis
            
            cal.add(Calendar.DAY_OF_YEAR, -2)
            val threeDaysAgo = cal.timeInMillis
            
            cal.add(Calendar.DAY_OF_YEAR, -4)
            val fiveDaysAgo = cal.timeInMillis

            // Insert premium seed data
            expenseDao.insertExpense(
                Expense(
                    title = "Monthly Salary",
                    amount = 3500.00,
                    category = "Salary",
                    timestamp = fiveDaysAgo,
                    note = "Primary job direct deposit",
                    isIncome = true
                )
            )
            expenseDao.insertExpense(
                Expense(
                    title = "Apartment Rent",
                    amount = 1200.00,
                    category = "Housing",
                    timestamp = fiveDaysAgo,
                    note = "Auto-pay rent",
                    isIncome = false
                )
            )
            expenseDao.insertExpense(
                Expense(
                    title = "Weekly Groceries",
                    amount = 145.50,
                    category = "Food",
                    timestamp = threeDaysAgo,
                    note = "Trader Joe's",
                    isIncome = false
                )
            )
            expenseDao.insertExpense(
                Expense(
                    title = "Freelance UI Design",
                    amount = 650.00,
                    category = "Freelance",
                    timestamp = yesterday,
                    note = "Landing page contract work",
                    isIncome = true
                )
            )
            expenseDao.insertExpense(
                Expense(
                    title = "Steakhouse Dinner",
                    amount = 78.40,
                    category = "Entertainment",
                    timestamp = yesterday,
                    note = "Celebration dinner with team",
                    isIncome = false
                )
            )
            expenseDao.insertExpense(
                Expense(
                    title = "Uber Ride",
                    amount = 24.50,
                    category = "Transport",
                    timestamp = now,
                    note = "Downtown office commute",
                    isIncome = false
                )
            )
            expenseDao.insertExpense(
                Expense(
                    title = "Blue Bottle Coffee",
                    amount = 6.75,
                    category = "Food",
                    timestamp = now,
                    note = "Double shot latte",
                    isIncome = false
                )
            )
            expenseDao.insertExpense(
                Expense(
                    title = "Gym Membership",
                    amount = 55.00,
                    category = "Health",
                    timestamp = now,
                    note = "Monthly premium subscription",
                    isIncome = false
                )
            )
        }
    }
}
