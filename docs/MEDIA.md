# Media (subida de imagen/video) — móvil

Feature en `features/media/`. Sube archivos a Supabase Storage mediante el flujo de
**URL firmada** contra el backend (`/api/v1/media`).

> Guía completa: `uflex-project-report/docs/media-storage-implementation.md`.

## Uso (ViewModel con Hilt)

```kotlin
@HiltViewModel
class EvidenceViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    fun upload(uri: Uri, patientId: String) = viewModelScope.launch {
        val resolver = context.contentResolver
        val contentType = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
        mediaRepository.upload(
            bytes = bytes,
            fileName = uri.lastPathSegment ?: "evidence",
            contentType = contentType,
            ownerType = "PATIENT_EVIDENCE",
            ownerId = patientId,
        ).onSuccess { asset -> /* AsyncImage(model = asset.downloadUrl) */ }
         .onFailure { /* error */ }
    }
}
```

Selección de archivo: usa el **Android Photo Picker** (`PickVisualMedia`), sin
permisos de almacenamiento.

## Archivos

- `data/remote/dto/MediaDtos.kt` — DTOs.
- `data/remote/api/MediaApiService.kt` — endpoints Retrofit.
- `data/remote/SupabaseMediaUploader.kt` — PUT directo a Supabase (OkHttp **sin** `AuthInterceptor`).
- `data/repository/MediaRepository.kt` — orquesta crear → subir → confirmar.
- `di/MediaModule.kt` — provee `MediaApiService` y el `OkHttpClient` `@StorageHttpClient`.

## Detalles

- La subida a Supabase usa un `OkHttpClient` sin auth (qualifier `@StorageHttpClient`)
  para no enviar el JWT a terceros. `writeTimeout` = 5 min para videos.
- Las `downloadUrl` caducan (~1 h); vuelve a pedir el asset si hace falta.
- `ownerType`: `PHYSIOTHERAPIST_RECORD` · `PATIENT_EVIDENCE` · `PROFILE_PHOTO` · `GENERIC`.
