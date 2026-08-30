# NexRhythm Roadmap

> Target: **small working MVP first**.  
> Jangan memperpanjang roadmap sebelum core trainer benar-benar berjalan.

---

## Phase 0 — Repository Bootstrap

**Target:** project siap dikembangkan.

- [ ] Finalize root repository files
- [ ] Create `nexrhythm_flutter`
- [ ] Basic Flutter app launches
- [ ] Establish minimal project structure
- [ ] Add initial README

**Done when:** aplikasi Flutter bisa dijalankan sebagai app shell tanpa fitur rhythm.

---

## Phase 1 — Timing Engine

**Target:** membuktikan core rhythm timing bekerja dengan benar.

- [ ] BPM control
- [ ] Stable main beat click
- [ ] 1–8 equal subdivisions per beat
- [ ] Main beat accent
- [ ] Start / Stop
- [ ] Verify no obvious accumulated drift
- [ ] Keep audio timing independent from UI animation where practical

**Priority:** tertinggi. UI sederhana tidak masalah.

**Done when:** user dapat memilih BPM + subdivision dan mendengar pembagian beat yang stabil.

---

## Phase 2 — Trainer MVP

**Target:** membuat core learning experience usable.

- [ ] Show subdivision number `1–8`
- [ ] Show default TAKA syllables
- [ ] Visual subdivision spacing
- [ ] Basic beat/subdivision indicator
- [ ] Adjustable beats per subdivision
- [ ] Basic guide modes:
  - main click
  - subdivision tick
  - syllable/voice guide if feasible without destabilizing MVP
- [ ] Simple `1 → 8 → 1` Pyramid mode

**Done when:** user bisa **hear + see + follow** subdivision dari satu screen utama.

---

## Phase 3 — MVP Polish & Verification

**Target:** aplikasi cukup stabil untuk dipakai latihan nyata.

- [ ] Clean basic UI
- [ ] Handle pause/stop/restart consistently
- [ ] Basic error states
- [ ] Static analysis
- [ ] Related tests
- [ ] Android build check
- [ ] Manual timing/practice verification
- [ ] Update README and project docs

**MVP Complete when:**

> NexRhythm dapat digunakan untuk memilih tempo, memainkan subdivision 1–8 secara stabil, menampilkan syllable/visual guide, dan menjalankan latihan dasar tanpa workflow yang membingungkan.

---

## Post-MVP — Deferred

Kerjakan hanya setelah MVP stabil dan ada keputusan baru.

- Meter / bar visualization
- Rhythm patterns
- Rest / accent / syncopation
- Expanded theory layer
- Polyrhythm
- Polymeter
- Instrument application examples
- Tap accuracy detection
- Microphone / pad input
- Advanced practice presets
- Live / performance mode

---

## Product Direction

```text
NOW
Pulse + Beat + Subdivision
        ↓
MVP
1–8 Subdivision Trainer
        ↓
NEXT
Meter + Rhythm Pattern + Theory
        ↓
LATER
Polyrhythm + Advanced Rhythm Practice
```

**Rule:** jangan menambah fase hanya karena fiturnya menarik. Tambahkan ketika kebutuhan MVP atau keputusan produk membutuhkannya.
