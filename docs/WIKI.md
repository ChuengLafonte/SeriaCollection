# SeriaCollection Wiki

Dokumentasi detail untuk konfigurasi dan pengelolaan sistem SeriaCollection.

## 🛠 Commands
| Command | Alias | Permission | Description |
|---------|-------|------------|-------------|
| `/collect` | `/collection` | None | Membuka menu GUI Koleksi |
| `/scollect reload` | | `seriacollection.admin` | Reload semua konfigurasi |
| `/scollect set <player> <id> <amount>` | | `seriacollection.admin` | Mengatur jumlah koleksi pemain secara manual |

## 🛡️ Anti-Exploit System
SeriaCollection dilengkapi dengan sistem pengamanan untuk mencegah manipulasi poin koleksi:

### 1. Item Tainting
Sistem akan menandai (taint) item yang keluar dari mekanisme otomatis berikut agar **tidak memberikan poin** saat diambil:
- **Dispenser**: Item yang ditembakkan oleh dispenser otomatis ditandai.
- **Container Break**: Saat Chest, Barrel, atau Shulker Box dihancurkan, seluruh isinya ditandai.
- **Drop Manual**: Item yang dijatuhkan sengaja oleh pemain tidak akan memberikan poin jika diambil kembali.

### 2. Monitor Drop vs Pickup
Poin hanya diberikan jika item berasal dari sumber alami (Breaking Blocks, Mob Drops, Fishing, Crafting).

## 📊 Placeholders
Memerlukan **PlaceholderAPI**:
- `%seriacollection_level_<id>%`: Mengembalikan level tier saat ini dari koleksi tertentu.
- `%seriacollection_amount_<id>%`: Mengembalikan total jumlah item yang telah dikumpulkan.

Contoh: `%seriacollection_level_OAK_LOG%`

## ⚙️ Configuration

### collections.yml
Definisikan koleksi dan tier Anda di sini.
```yaml
collections:
  OAK_LOG:
    name: "Oak Wood"
    material: OAK_LOG
    category: FORAGING
    tiers:
      1:
        requirement: 100
        rewards:
          - "COMMAND: give <player> oak_sapling 5"
          - "MESSAGE: <green>You unlocked Oak Collection I!"
```

## 🔑 Permissions
- `seriacollection.admin`: Akses ke seluruh perintah administratif.
