# KameraKu — Aplikasi Kamera Sederhana (Jetpack Compose + CameraX) 📷📷

KameraKu adalah aplikasi Android sederhana menggunakan **Jetpack Compose**, **CameraX**, dan **MediaStore** untuk mengambil foto, menyimpan ke penyimpanan perangkat, dan menampilkan thumbnail foto terakhir.

Repo kode:
`https://github.com/nadhif-royal/PAPB-KameraKu`

---

## Fitur Utama

* Preview kamera secara live
* Ambil foto dengan CameraX
* Simpan foto menggunakan MediaStore
* Tampilkan thumbnail foto terakhir
* Switch kamera depan ↔ belakang *(experimental)*
* Toggle flash *(experimental — diketahui bermasalah, analisis di bawah)*

---

# Arsitektur & Struktur File

```
app/
└── src/main/java/com/example/kameraku/
    ├── MainActivity.kt
    └── ui/
        └── CameraScreen.kt
```

---

# Alur Perizinan Kamera

### 1. Permintaan izin dilakukan dengan `rememberLauncherForActivityResult`

Aplikasi meminta izin kamera ketika layar `CameraScreen` pertama kali tampil:

```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted -> ... }

LaunchedEffect(Unit) {
    permissionLauncher.launch(android.Manifest.permission.CAMERA)
}
```

### 2. Masalah yang bisa terjadi

Jika aplikasi **langsung menampilkan pesan “Izin kamera ditolak!”** ketika start, kemungkinan penyebabnya:

#### Permission dipanggil *terlalu cepat*

`LaunchedEffect(Unit)` berjalan sebelum Compose selesai menginisialisasi context tertentu.
Solusi umum (belum diterapkan): menggunakan **SideEffect**, atau menunda 100–300ms.

#### Permission tidak muncul di Android 12+ jika belum ditambahkan di manifest

Pastikan manifest berisi:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

---

# Alur Penyimpanan Foto (MediaStore)

### 1. Membuat metadata file

```kotlin
val contentValues = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, name)
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
}
```

### 2. Menentukan lokasi penyimpanan

Menggunakan MediaStore (tidak perlu akses storage manual):

```kotlin
MediaStore.Images.Media.EXTERNAL_CONTENT_URI
```

Foto akan otomatis masuk ke folder **Pictures/** (Android mengatur sendiri).

### 3. Menyimpan foto

```kotlin
capture.takePicture(
    output,
    ContextCompat.getMainExecutor(context),
    object : ImageCapture.OnImageSavedCallback { ... }
)
```

### 4. Menampilkan thumbnail

Mengambil bitmap terakhir:

```kotlin
MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
```

---

# Analisis Masalah Rotasi & Flashlight (Tidak Berfungsi)

Fitur **rotasi (switch kamera)** dan **flash** belum berfungsi stabil.
Berikut analisis penyebabnya secara jujur:

---

## 1. Flash Toggle Tidak Berfungsi (Root Cause)

### Penyebab:

Anda mengatur flash mode **saat ImageCapture dibuat**, tetapi **tidak me-rebind kamera** setiap kali flash berubah.

Kode Anda:

```kotlin
val capture = ImageCapture.Builder()
    .setFlashMode(if (flashEnabled) FLASH_MODE_ON else FLASH_MODE_OFF)
    .build()
```

Namun, ketika `flashEnabled` berubah, **CameraX tidak rebuild** ImageCapture, sehingga flash tetap OFF/ON seperti pertama kali dibuat.

### Harusnya:

* Unbind kamera
* Rebuild Preview & ImageCapture
* Rebind lifecycle dengan konfigurasi baru

Ini adalah **behaviour normal** di CameraX — bukan salah kode Anda.

---

## 2. Switch Kamera (Front ↔ Back) Kadang Crash atau Tidak Update

Kode Anda mengubah:

```kotlin
cameraSelector =
    if (cameraSelector == DEFAULT_BACK_CAMERA)
        DEFAULT_FRONT_CAMERA
    else
        DEFAULT_BACK_CAMERA
```

Namun sama seperti flash:

### Penyebab:

CameraX **butuh rebind penuh** ketika kamera diganti.

Jika `bindToLifecycle()` tidak dipanggil ulang setelah selector berubah, preview tidak berubah.

### Solusi:

Pindahkan seluruh logika binding kamera ke fungsi reusable:

```
fun bindCamera(cameraSelector, flashEnabled)
```

dan panggil ulang setiap toggle.

---

# Kesimpulan Teknis

| Fitur          | Status               | Penyebab                 | Solusi ideal                           |
| -------------- | -------------------- | ------------------------ | -------------------------------------- |
| Preview kamera | ✔ Berfungsi          | -                        | -                                      |
| Ambil foto     | ✔ Berfungsi          | -                        | -                                      |
| Simpan foto    | ✔ Berfungsi          | MediaStore OK            | -                                      |
| Thumbnail foto | ✔ Berfungsi          | -                        | -                                      |
| Flash          | ⚠ Tidak berfungsi    | Camera tidak di-rebind   | Rebind ImageCapture setiap toggle      |
| Switch kamera  | ⚠ Kadang error       | Kamera tidak di-rebind   | Rebind cameraProvider setelah switch   |
| Permission     | ⚠ Kadang skip dialog | Permintaan terlalu cepat | Tambahkan delay / pindah ke SideEffect |

---

# Future Improvement (Saran)

* Buat `CameraController` custom agar flash & switch kamera mulus
* Tambahkan pengelolaan UI state yang lebih stabil (ViewModel)
* Gunakan `coil` untuk menampilkan thumbnail (lebih ringan dari `getBitmap`)



Cukup bilang **“perbaiki flash & switch kamera”**.
