# BudLords - Játékmenet Útmutató (Magyar)

## Hogyan játszhatok?

Ez az útmutató elmagyarázza, hogyan működik a BudLords plugin és hogyan tudsz játszani vele.

---

## 🎮 A játék menete (Game Loop)

### 1. 🌱 Mag beszerzése
**Admin parancs:** `/straincreator`
- Megnyílik egy GUI, ahol létrehozhatsz új fajtákat
- Beállíthatod a fajta nevét, ritkaságát, erősségét és hozamát
- A mentés után 5 magot kapsz

### 2. 🪴 Ültetés
**Két módszer létezik:**

#### Cserépalapú rendszer (Ajánlott)
1. Szerezz egy **Growing Pot** (Növesztőcserép) ★-ot
2. Helyezd le a cserepet egy szilárd felületre (jobb klikk)
3. Tartsd a magokat a kezedben
4. Jobb klikk a cserépre az ültetéshez

#### Régi farmland rendszer
1. Készíts farmlandot kapával (föld/fű megmunkálása)
2. Tartsd a magokat a kezedben
3. Jobb klikk a farmlandre

### 3. 💧 Növénygondozás
A jobb minőségű termésért gondoskodj a növényeidről:

| Művelet | Hogyan | Hatás |
|---------|--------|-------|
| **Öntözés** | Vödör vízzel jobb klikk a növényre | Növeli a vízszintet |
| **Trágyázás** | Trágyával jobb klikk a növényre | Növeli a tápanyagszintet |
| **Megvilágítás** | Grow Lamp elhelyezése a közelben | Minőségi bónusz |

### 4. 🌿 Növekedés
A növények 4 szakaszon mennek át:
1. **Seed** (Mag) - Frissen ültetve
2. **Sprout** (Csíra) - Elkezdett nőni
3. **Growing** (Növő) - Fejlődik
4. **Mature** (Érett) - Betakarításra kész!

**Növekedési idő:** Alapértelmezetten ~5 perc szakaszonként (konfigurálható)

### 5. ✂️ Betakarítás
Amikor a növény **Mature** (érett) állapotba kerül:

**Ollóval (Ajánlott):**
- Használj **Harvest Scissors** ★-ot
- Jobb klikk az érett növényre
- Bónusz hozam és minőség!

**Ollő nélkül:**
- Jobb klikk vagy törd el a növényt
- Kapod a bud-okat, de kisebb hozammal

### 6. 📦 Csomagolás
A nyers budokat csomagolnod kell eladás előtt:

```
/package 1   - 1g csomag (×1.0 szorzó)
/package 3   - 3g csomag (×1.25 szorzó)
/package 5   - 5g csomag (×1.5 szorzó)
/package 10  - 10g csomag (×2.0 szorzó)
```

**Drag-and-Drop csomagolás:**
1. Dobd le a budokat a földre
2. Dobd rá a csomagot (1g, 3g, 5g, vagy 10g)
3. Vedd fel a kész csomagot!

### 7. 💰 Eladás
Keresd meg az NPC-ket:

| NPC | Admin parancs | Leírás |
|-----|---------------|--------|
| **Market Joe** | `/spawnmarket` | Normál árak, felszerelés vásárlás |
| **BlackMarket Joe** | `/spawnblackmarket` | Prémium árak ritka fajtákért |

**Eladás:**
1. Tartsd a csomagolt terméket a kezedben
2. Jobb klikk az NPC-re
3. Ha sikeres az üzlet, pénzt kapsz!

### 8. 📈 Rangok és fejlődés
Pénzszerzéssel rangot lépsz:

| Rang | Szükséges bevétel |
|------|-------------------|
| Novice | $0+ |
| Dealer | $1,000+ |
| Supplier | $5,000+ |
| Distributor | $15,000+ |
| Kingpin | $50,000+ |
| Cartel Boss | $150,000+ |
| BudLord | $500,000+ |

---

## ⭐ Csillag Minőségi Rendszer (★)

Minden felszerelésnek 1-5 csillagos minősége van:

| Értékelés | Szín | Minőségi szorzó | Növekedési sebesség |
|-----------|------|-----------------|---------------------|
| ★☆☆☆☆ | Szürke | ×1.0 | -20% |
| ★★☆☆☆ | Sárga | ×1.15 | -10% |
| ★★★☆☆ | Zöld | ×1.35 | Normál |
| ★★★★☆ | Kék | ×1.6 | +15% |
| ★★★★★ | Arany | ×2.0 | +35% |

**A végső bud ★ értékelése ezekből számolódik:**
- Cserép (20%)
- Mag (25%)
- Lámpa (20%)
- Trágya (15%)
- Olló (10%)
- Gondozás minősége (10%)

---

## 🚬 Joint Sodró Minijáték

1. Vegyél **Grinder**-t és **Tobacco**-t Market Joe-tól
2. Őröld meg a budokat (jobb klikk grinderrel bud tartása közben)
3. Jobb klikk az őrölt buddal (sodrópapír és dohány legyen az inventoryban)
4. Végezd el a 4 szakaszos minijátékot!

**Szakaszok:**
1. **Paper Pull** - Időzítéses játék
2. **Tobacco Roll** - Gyors kattintás
3. **Ganja Grinding** - Cél követése
4. **Final Roll** - Erőmérő időzítése

---

## 🧬 Crossbreeding Lab

Egyedi hibrid fajták létrehozása:
```
/crossbreed
```
- Kombinálj két szülő fajtát
- Esély mutációkra, amelyek javítják a tulajdonságokat
- Fedezz fel új legendás fajtákat!

---

## 🏆 Prestige Rendszer

Reseteld a haladásodat állandó bónuszokért:
```
/prestige
```
- 10 prestige szint elérhető
- Szintenként bónuszok:
  - +10% Bevétel
  - +5% Növekedési sebesség
  - +8% Minőségi bónusz
  - +2% Üzlet sikeresség

---

## 📋 Napi és Heti Kihívások

```
/challenges
```
- 3 napi kihívás (Könnyű, Közepes, Nehéz)
- 3 heti kihívás (Nehéz/Legendás)
- Bónusz pénz és XP jutalom

---

## 📊 Statisztikák és Ranglista

```
/stats         - Saját statisztikák
/leaderboard   - Szerver ranglista
```

---

## 💡 Parancsok összefoglaló

### Játékos parancsok
| Parancs | Leírás |
|---------|--------|
| `/bal` | Egyenleg és rang ellenőrzése |
| `/pay <játékos> <összeg>` | Pénz küldése |
| `/package <mennyiség>` | Budok csomagolása |
| `/stats` | Statisztikák megtekintése |
| `/prestige` | Prestige menü |
| `/challenges` | Kihívások |
| `/crossbreed` | Hibridizálás |
| `/leaderboard` | Ranglista |

### Admin parancsok
| Parancs | Leírás |
|---------|--------|
| `/addmoney <játékos> <összeg>` | Pénz hozzáadása |
| `/straincreator` | Fajta létrehozó GUI |
| `/spawnmarket` | Market Joe spawolása |
| `/spawnblackmarket` | BlackMarket Joe spawolása |
| `/budlords reload` | Konfiguráció újratöltése |

---

## 🔧 Hibaelhárítás

**"Seeds can only be planted on farmland or in Growing Pots!"**
- Használj Growing Pot-ot (jobb klikk szilárd felületre)
- Vagy készíts farmlandot kapával

**A növény lassan nő**
- Használj jobb ★ minőségű cserepet
- Tartsd a vízszintet 70% felett
- Trágyázz rendszeresen
- Tegyél grow lampát a közelbe

**Alacsony bud minőség**
- Használj jobb ★ felszerelést mindenhol
- Gondozd rendszeresen a növényeket
- Harvest Scissors használata betakarításkor

---

*BudLords v1.0.0 - Minecraft fűtermesztő gazdasági plugin ⭐ Star Quality Rendszerrel*
