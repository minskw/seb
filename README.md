# 📱 Safe Exam Browser — Android

Aplikasi Android untuk ujian online berbasis Google Form dengan mode kiosk dan sistem anti-cheat.

---

## ✨ Fitur Utama

### 📋 Manajemen Ujian
- **Daftar Ujian** — tampilkan semua ujian dengan info lengkap: Mapel, Tanggal, Waktu, Durasi
- **Tambah Ujian Manual** — isi Nama, Mata Pelajaran, Tanggal (date picker), Waktu (time picker), Durasi, dan Link Google Form
- **Scan QR** — scan kode QR untuk mengisi URL Google Form secara otomatis
- **Link Langsung** — Google Form dibuka langsung di dalam WebView aplikasi (tidak bisa keluar ke browser lain)

### 🔒 Keamanan & Anti-Cheat
- **Mode Kiosk** — tombol Home, Back, Recents diblokir selama ujian
- **Blokir Screenshot** — layar tidak bisa di-screenshot atau direkam
- **Deteksi Root** — ujian diblokir jika perangkat ter-root
- **Deteksi Developer Mode / USB Debug** — tercatat sebagai pelanggaran
- **Deteksi VPN** — tercatat sebagai pelanggaran
- **Batas Pelanggaran** — ujian otomatis dihentikan setelah 3 pelanggaran
- **Blokir Navigasi Eksternal** — WebView hanya mengizinkan domain Google

---

## 🗄️ Struktur Data Ujian

| Field | Tipe | Keterangan |
|-------|------|------------|
| `namaUjian` | String | Nama / judul ujian |
| `mapel` | String | Mata pelajaran |
| `tanggal` | String | Tanggal ujian (dd/MM/yyyy) |
| `waktu` | String | Waktu mulai (HH:mm) |
| `durasi` | Int | Durasi dalam menit |
| `url` | String | Link Google Form |

---

## 🚀 Setup & Build

### Prasyarat
- Android Studio Hedgehog (2023.1) atau lebih baru
- JDK 17
- Android SDK 34

### Build Lokal
```bash
# Clone repo
git clone https://github.com/<user>/SafeExamBrowser.git
cd SafeExamBrowser

# Build debug
./gradlew assembleDebug

# Build release (tanpa signing)
./gradlew assembleRelease
```

---

## ⚙️ GitHub Actions (CI/CD Otomatis)

### CI — `android-ci.yml`
Berjalan otomatis saat **push ke `main`/`develop`** atau **pull request ke `main`**:
1. Detekt (static analysis)
2. Android Lint
3. Unit Tests
4. Build Debug APK

### CD — `android-cd.yml`
| Trigger | Aksi |
|---------|------|
| Push ke `main` | Build + Sign APK/AAB, bump `versionCode` otomatis |
| Push tag `v*` | Build + Sign + buat GitHub Release dengan changelog otomatis |
| Jadwal harian (00:00 WIB) | Build snapshot otomatis |
| `workflow_dispatch` | Trigger manual dari GitHub UI |

### 🔑 GitHub Secrets yang Dibutuhkan

Tambahkan di **Settings → Secrets → Actions**:

| Secret | Keterangan |
|--------|-----------|
| `KEYSTORE_BASE64` | Keystore di-encode base64: `base64 -w 0 release.keystore` |
| `KEYSTORE_PASSWORD` | Password keystore |
| `KEY_ALIAS` | Alias key dalam keystore |
| `KEY_PASSWORD` | Password key |

### Membuat Keystore (sekali saja)
```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias safeexam \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Encode ke base64 untuk disimpan di secret
base64 -w 0 release.keystore
```

---

## 🏗️ Arsitektur

```
app/
├── data/
│   ├── db/
│   │   ├── entity/Exam.kt          # Model: namaUjian, mapel, tanggal, waktu, durasi, url
│   │   ├── dao/ExamDao.kt
│   │   └── AppDatabase.kt          # Room v2, migrasi 1→2 otomatis
│   └── repository/ExamRepository.kt
├── di/AppModule.kt                  # Hilt DI
├── security/
│   ├── AntiCheatManager.kt
│   ├── RootDetector.kt
│   ├── DevModeDetector.kt
│   └── VpnDetector.kt
└── ui/
    ├── home/                        # Daftar Ujian (RecyclerView)
    ├── addexam/                     # Form tambah ujian (date/time picker)
    ├── exam/                        # WebView kiosk mode
    └── qrscanner/                   # Scan QR
```

**Stack:** Kotlin · MVVM · Hilt · Room · Navigation Component · CameraX · ML Kit · Coroutines/Flow

---

## 📄 Lisensi

MIT License — bebas digunakan untuk keperluan pendidikan.
