package com.kiniot.uflex.core.network

import javax.inject.Qualifier

/**
 * Qualifies the OkHttpClient used to talk to external object storage
 * (Supabase Storage) via signed URLs. Unlike the default client, it does NOT
 * attach the backend Bearer token, so credentials are never sent to third parties.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StorageHttpClient
