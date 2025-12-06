package com.example.paisatracker

import android.app.Application
import com.example.paisatracker.data.PaisaTrackerDatabase
import com.example.paisatracker.data.PaisaTrackerRepository

class PaisaTrackerApplication : Application() {
    val database: PaisaTrackerDatabase by lazy { PaisaTrackerDatabase.getDatabase(this) }
    val repository: PaisaTrackerRepository by lazy { PaisaTrackerRepository(database.projectDao(), database.categoryDao(), database.expenseDao(), database.assetDao()) }
}
