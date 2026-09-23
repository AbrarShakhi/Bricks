package com.abrarshakhi.bricks.core.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import kotlinx.serialization.KSerializer

internal fun <T> Context.jsonDataStore(
    fileName: String,
    serializer: KSerializer<T>,
    defaultValue: T,
): DataStore<T> = DataStoreFactory.create(
    serializer = JsonDataStoreSerializer(serializer, defaultValue, LauncherJson),
    corruptionHandler = ReplaceFileCorruptionHandler { defaultValue },
    produceFile = { applicationContext.dataStoreFile(fileName) },
)
