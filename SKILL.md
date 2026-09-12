---
name: yumaplayer-uiux-analysis
description: >
  Analyzes, replicates, and extends the UI/UX design of YumaPlayer — a FOSS Android
  music client built on the Yuma Design System (YDS 2.1): real glassmorphism, segmented
  glass preference rows, dynamic Monet/HCT theming extracted from album art, and 120fps
  gesture-driven player sheets. Use when (a) building, reviewing, or redesigning Jetpack
  Compose UI that should follow a cohesive, music-centric design language; (b) studying
  how YumaPlayer achieves calm, depth, low visual noise, and fluid motion; or (c) applying
  its component patterns, tokens, or motion rules to another interface.
---

# YumaPlayer — UI/UX Design Analysis (Yuma Design System 2.1)

> Source of truth: `docs/design/YDS.md`, `docs/design/COMPONENT_GUIDE.md`,
> `docs/architecture/DECISIONS.md` (ADR-001/002/006/010), and the Compose
> implementation under `app/src/main/.../ui/theme/*`, `ui/settings/SettingsDimensions.kt`,
> `ui/component/*`, `ui/player/player_0/*`.

YumaPlayer is a hybrid Android music client (Spotify discovery + YouTube Music library +
Hi-Res/FLAC lossless). Its interface is **not** Material-3-default: it layers a custom
design system (YDS 2.1) on top of Material 3 Expressive, prioritizing music-as-primary
content, depth via glass, breathable whitespace, and tactile micro-interactions.

---

## 1. Design Philosophy

YDS is opinionated. Five pillars (from `YDS.md` §1–2):

1. **Calmness** — the UI never overwhelms or distracts from listening.
2. **Depth & Geometry** — translucent `glassBackground` surfaces + 0.5dp hairline
   `glassBorder` define layering; composite corner radii fuse separate rows into one
   visual group (22dp outer / 5dp inner).
3. **Low Visual Noise** — **dividers are banned** in settings lists and selection sheets.
   Separation comes only from gaps (`SegmentGap = 1.5dp`) and structure.
4. **Physicality** — every interactive element gives spring-animated tactile feedback
   (whole-card scale to `0.96f` on press).
5. **Color Driven** — the accent palette is extracted at runtime from the *currently
   playing* artwork (Dynamic Monet / HCT), driving accents, switch thumbs, and indicators.

Avoid the "generic monolithic Material 3 template with flat continuous lists." Yuma's
identity is **segmented glass geometry, accented matte icon badges, whitespace, and
tactile motion.**

---

## 2. Visual Language & Character

- **Segmented glass geometry:** grouped rows are *independent* glass cards, not one shared
  container. Group unity comes from matching corner radii + a 1.5dp gap, not borders.
- **Composite corner radii:** outer group corners `22dp`, inner joints `5dp`.
- **Hairline borders:** `0.5dp` translucent outline (`glassBorder`) for crisp edge definition.
- **Matte icon badges:** custom squircle/petal geometry, 32dp / 46dp containers, monochrome
  or accent fill — never default Material circular chips.
- **Surface hierarchy (strict 3-tier):**
  - *Solid* (`surface`, `surfaceContainer`) — main content where translucency is unnecessary.
  - *Static translucent glass* (`glassBackground` + 1dp `glassBorder`, **no blur**) — settings
    lists, cards. Modal sheets/dialogs use **opaque** `surfaceContainerHigh`.
  - *Backdrop-blur glass* (blur + tonal overlay) — **exclusively** floating overlays:
    bottom player bar, floating FAB, modal backdrops. (ADR-006)

---

## 3. Design Tokens (single source of truth)

All values come from `SettingsDimensions` / `YumaSpacing` / `YumaRadius`. **Never hardcode
dp or hex.** Key tokens (`ui/settings/SettingsDimensions.kt`):

| Token | Value | Use |
| :-- | :-- | :-- |
| `SegmentedItemGap` | `1.5.dp` | inter-row gap (replaces dividers) |
| `SectionSpacing` | `12.dp` | between independent groups |
| `ScreenHorizontalPadding` | `16.dp` | screen/card horizontal padding |
| `ScreenBottomPadding` | `24.dp` | bottom screen padding |
| `SegmentedCornerLarge` (Outer) | `22.dp` | first/last row outer corners, cards |
| `SegmentedCornerSmall` (Inner) | `5.dp` | inner joints between grouped rows |
| `BottomSheetCornerRadius` | `28.dp` | modal sheets |
| `BottomSheetListCornerRadius` | `24.dp` | inner sheet lists |
| `GlassCornerRadius` | `18.dp` | default glass card |
| `GlassBorderThickness` | `0.5.dp` | hairline outline |
| `SegmentedItemMinHeight` | `72.dp` | preference row min height |
| `SegmentedIconBoxSize` | `46.dp` | icon badge container |
| `RowIconSize` | `32.dp` | standard row icon |
| `LibraryChipHeight` | `36.dp` | filter chips |
| `PressScale` | `0.96f` | press compression |
| `PressDampingRatio` / `Stiffness` | `0.6` / `Medium` | press spring |

Radii ladder (YDS §4): `SegmentInner 5` · `Small 12` · `Medium 16` · `SegmentOuter 22` ·
`SheetList 24` · `Sheet 28` · `Max/Pill 32 / Circle`.

Hierarchy through scale (YDS §3.1): preference rows `≥72dp`, primary actions `≥56dp`,
standard controls `48×48dp` touch target, icon badges `32/46dp`.

---

## 4. Core Components & The Modifier Chain

Two primitive modifiers build everything. Both live in `ui/theme/YumaModifiers.kt`.

### 4.1 `Modifier.yumaGlassCard(...)` — the glass surface
```kotlin
@Composable
fun Modifier.yumaGlassCard(
    shape: Shape = RoundedCornerShape(SettingsDimensions.GlassCornerRadius),
    backgroundColor: Color = LocalYumaColors.current.glassBackground,
    borderColor: Color = LocalYumaColors.current.glassBorder,
    strokeWidth: Dp = SettingsDimensions.GlassBorderThickness,
    position: YumaSegmentPosition = YumaSegmentPosition.Single,
    topAlpha: Float? = null,
    bottomAlpha: Float? = null,
): Modifier
```
Internally: `clip(shape) → background(backgroundColor, shape) → glassBorder(...)`. The hairline
border is a **vertical gradient** (`topAlpha` → `bottomAlpha`) so group joints don't form
blinding double-bright seams.

### 4.2 `Modifier.yumaClickable(...)` — tactile press
```kotlin
@Composable
fun Modifier.yumaClickable(
    enabled: Boolean = true,
    pressedScale: Float = SettingsAnimations.PressScale,   // 0.96f
    onClick: () -> Unit
): Modifier
```
Spring-scale to `pressedScale` while pressed (`pressSpring()` = `spring(dampingRatio = 0.6,
stiffness = Medium)`); respects `LocalDisableAnimations`. A `yumaCombinedClickable` variant
adds long/double-click + `LocalYumaHaptics` feedback.

### 4.3 The mandatory modifier order
`.yumaClickable(...)` **MUST precede** `.yumaGlassCard(...)` so the *whole card* scales as one
unit. Putting clickable after (or inside inner containers) scales only the text → broken.
```kotlin
Box(
    modifier = modifier
        .fillMaxWidth()
        .yumaClickable(pressedScale = 0.96f, onClick = onClick)   // 1. press
        .yumaGlassCard(shape = shape, position = position)          // 2. glass
        .clip(shape)
        .padding(horizontal = 18.dp, vertical = 12.dp)
) { /* Row content: [Badge] Title/Subtitle … [Control/Chevron] */ }
```

### 4.4 Segmented group lighting (`YumaSegmentPosition`)
Each row in a group reports its position so its border gradient adapts:
`Single 0.20→0.04` · `First 0.20→0.08` · `Middle 0.08→0.08` · `Last 0.08→0.04`.
Compute with `yumaSegmentPosition(index, count)` + `segmentedSettingsItemShape(index, count)`.

### 4.5 Standard pieces built from these primitives
`GlassScaffold` (transparent `Scaffold`, transparent top-bar — `GlassScaffold.kt`),
`PreferenceGroup` / `PreferenceEntry` / `SwitchPreference` / `SegmentedPreference` /
`ListPreference` / `EditTextPreference` / `SliderPreference` / `NumberPickerPreference`
(YDS §7), provider chips (sliding 36dp `primary` thumb between YT Music / Spotify), and
selection dialogs (independent segmented cards, `surfaceContainerHigh` modal, checkmark on
selected, `primary.copy(alpha=0.16f)` tint).

---

## 5. Color & Theming (Dynamic Monet / HCT)

- **`YumaColorScheme`** (`YumaTheme.kt`): `glassBackground`, `glassBorder`,
  `cardBackgroundOpaque`, `textPrimary`, `textSecondary`, `rippleColor`.
  - Dark: `glassBackground = onSurface@0.08`, `glassBorder = primary@0.10`,
    `cardBackgroundOpaque = 0xFF1C1C1E`.
  - Light: `glassBackground = White@0.65`, `glassBorder = primary@0.14`.
  - Exposed via `LocalYumaColors` (`staticCompositionLocalOf`). Wrap UI in `YumaTheme {}`.
- **Per-track palette** (`PlayerColorExtractor.kt`, ADR-002): `Palette` swatches → a tuned
  6-stop mesh gradient. HSV tuning boosts saturation (×1.25 when sat>0.3), derives hue-shifted
  siblings, and **falls back to greyscale stops** for B/W artwork — all on a background
  dispatcher (CPU-bound, never Main). `extractedColors` → vibrant / darkMuted / gradient.
- **Rule:** never hardcode `Color(0xFF…)`; pull from `LocalYumaColors.current.*` or
  `MaterialTheme.colorScheme.*`. Accent only where it matters (active pills, switch thumbs,
  indicators). Maintain WCAG contrast.

---

## 6. Motion & Interaction (fluid, frame-perfect)

- **Press physics:** whole-card `scale → 0.96f`, `spring(dampingRatio=0.6, stiffness=Medium)`.
- **Telegram-style morphing header** (`YumaMorphingHeader.kt`): scroll-linked `lerp3`
  (two-stage lerp) collapses an artist/album poster into a status-bar drop **without image
  distortion** — `scale 1→0.44→0`, `cornerPercent 0→32→50`, `translationY` to status-bar
  center, and a render-effect **blur only after 70% collapsed** (≤40f). `HeaderType`
  controls height ratio + bottom gradient (ARTIST 1.45, ALBUM/PLAYLIST 1.35, SPOTIFY 1.0).
- **Dual-sheet gesture engine** (Lyrics left / Queue right) — `UnifiedPlayerSheetV2.kt`,
  ADR-010, 120fps kinematics:
  1. **Draw-phase-only reading** — sheet fractions (`queueFraction`, `lyricsFraction`) are
     read **only inside `graphicsLayer { }`**, never in the Composable body (keeps the
     Composition tree static during drag → zero re-composition thrash).
  2. **Discrete `BackHandler`** anchored centrally with boolean flags (`isQueueVisible`),
     never bound to continuous thresholds.
  3. **Adaptive `CompositingStrategy`** — `Auto` during drag, `Offscreen` only when fully
     open (`fraction >= 0.99f`) for edge fades (saves GPU FBO bandwidth mid-gesture).
  4. **Isolated `matchParentSize()` underlay** trims lateral borders via negative layout
     translation; content renders as clean siblings.
  5. **Smart Freeze / Pre-warming** — instant 0-frame sheet expansion; off-screen physics
     loops + background tasks suspend via `snapshotFlow` to preserve battery/thermals.
  > Note: ADR-010 forbids agents from refactoring swipe physics/gesture handlers/sheet layers
  > unless explicitly ordered with exact specs. Treat these files as frozen.
- **Backdrop blur is reserved for floating overlays only** (ADR-006) — never on list rows.

---

## 7. Layout Patterns

- **Full player** (`PlayerLayout.kt`): a custom **single-pass `Layout`** with 3 slots —
  `toolbar`, `cover`, `controls`. The cover is a **square** constrained to remaining space,
  all slots centered horizontally, controls pinned to bottom, toolbar to top. No nested
  scroll/measure thrash.
- **Bottom player bar** — floating, backdrop-blur glass, haptic-driven, dismissible.
- **Library / home** — quick-picks grid, `LibraryChipHeight 36dp` filter chips, immersive
  artist posters, scroll-morphing headers.
- **Lyrics** — 3-tier synced (original / romaji / translation), karaoke word-by-word scroll,
  on-the-fly AI translation, shareable lyric cards.

---

## 8. Accessibility

- Touch targets: **48×48dp minimum**; **56dp+** for core player actions; **72dp** min height
  for preference rows.
- `contentDescription` for non-text/decorative icons (`null` when purely decorative).
- High-contrast text: `SoftTextShadow` (offset 0,2 / blur 5 / alpha 0.3) protects white text
  on bright artwork; dynamic palette tuned for WCAG.
- All components `@ThemePreviews` (light + dark) so both themes stay verifiable.

---

## 9. DO / DON'T (anti-patterns)

**DO**
- Use `Modifier.yumaGlassCard()` (glass fill + 0.5dp hairline + position-aware lighting) for
  every preference row and selection option.
- Put `.yumaClickable(...)` **before** `.yumaGlassCard(...)` for whole-card compression.
- Separate grouped rows with `SegmentGap (1.5dp)`, not `HorizontalDivider`.
- Apply composite radii (22dp outer / 5dp inner) to unify a group.
- Use custom squircle/petal icon badges; reference tokens via `SettingsDimensions` / `LocalYumaColors`.
- Build **100% stateless** components (props + lambda callbacks; no ViewModel/Room/coroutine
  launches inside Composables).

**DON'T**
- Wrap a whole preference group in one opaque container instead of individual segmented rows.
- Apply runtime `BackdropBlur` to list items / preference rows (blur is for floating overlays only).
- Put `.yumaClickable(...)` after `.yumaGlassCard(...)` or inside inner containers (text-only scaling).
- Omit `YumaSegmentPosition` on grouped rows (causes blinding joint lines).
- Hardcode dp / hex colors / raw `TextStyle` / `Shape` outside YDS tokens.
- Use generic circular Material 3 chips; place ViewModel/Room/coroutine calls in Composables;
  omit `key` in `LazyColumn`/`LazyRow` items.

---

## 10. How to apply this skill

When asked to **build, review, or redesign** a screen in the Yuma style:

1. **Pick the surface tier** — main content = solid; lists/cards = static glass; floating
   player/FAB/modal backdrop = blur glass. No dividers anywhere.
2. **Start from tokens** — pull every dp/radius/color from `SettingsDimensions` / `LocalYumaColors`.
3. **Compose with the primitives** — `Box.modifier.yumaClickable(...).yumaGlassCard(position=...)`.
   Compute `position = yumaSegmentPosition(index, count)` and `shape = segmentedSettingsItemShape(...)`.
4. **Keep components stateless** — props + `onClick` lambdas; emit intents, don't fetch.
5. **Add motion** — press scale `0.96f` spring; read continuous fractions inside `graphicsLayer {}`.
6. **Theme dynamically** — derive accents from artwork via `PlayerColorExtractor`; respect dark/light.
7. **Verify a11y** — 48/56/72dp targets, `contentDescription`, `@ThemePreviews` light+dark.
8. **Self-check against §9** before shipping.

When **analyzing** an unfamiliar FOSS UI, mirror §1–§8: identify the philosophy, surface
hierarchy, token discipline, component primitives, theming strategy, motion model, layout
system, and accessibility — then summarize strengths, inconsistencies, and reusable patterns.

---

## 11. Source Map (for deeper reads)

| Concern | File |
| :-- | :-- |
| Design spec | `docs/design/YDS.md`, `docs/design/COMPONENT_GUIDE.md` |
| Design tokens | `app/src/main/.../ui/settings/SettingsDimensions.kt` |
| Glass + click modifiers | `app/src/main/.../ui/theme/YumaModifiers.kt` |
| Soft shadows / text shadow | `app/src/main/.../ui/theme/ModifierExt.kt` |
| Color scheme + theme | `app/src/main/.../ui/theme/YumaTheme.kt` |
| Dynamic palette extraction | `app/src/main/.../ui/theme/PlayerColorExtractor.kt` |
| Glass scaffold | `app/src/main/.../ui/component/GlassScaffold.kt` |
| Morphing artist/album header | `app/src/main/.../ui/component/YumaMorphingHeader.kt` |
| Player layout (single-pass) | `app/src/main/.../ui/player/player_0/PlayerLayout.kt` |
| Dual-sheet gesture engine | `app/src/main/.../ui/player/player_0/UnifiedPlayerSheetV2.kt` |
| Architecture / ADRs | `docs/architecture/DECISIONS.md` (ADR-001/002/006/010) |
