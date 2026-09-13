---
name: pennywise-bottom-navigation
description: >
  Analyses and replicates the PennyWise AI (sarim2000/pennywiseai-tracker) bottom
  navigation bar — a single component with two user-switchable styles: NORMAL (docked
  Material 3 NavigationBar with a hairline divider and Haze frosted-glass) and FLOATING
  (Material 3 Expressive HorizontalFloatingToolbar of TonalToggleButtons with animated
  label-expanding pills, sitting on a surface gradient scrim). Covers every effect:
  Haze backdrop blur (20dp, Unbounded edges, transparent-background auto-tint), the
  blur-during-animation visibility transition (BlurredAnimatedVisibility: RenderEffect
  5→0 + alpha 0.95→1, with a RenderScript fallback below API 31), slide+fade enter/exit,
  active-indicator pill vs. icon-tint fallback, route matching that survives query args,
  content-overlay layout with 80dp/96dp/112dp clearance tokens, and the DataStore-backed
  style/blur preferences. Use when building, porting, reviewing or theming a bottom nav
  bar in Jetpack Compose — in particular when adding one to TeleBox.
---

# PennyWise AI — Bottom Navigation Bar: complete effect-by-effect analysis & replication guide

> Analysed from `sarim2000/pennywiseai-tracker` @ `main` (clone of 2026-09-12).
> Primary source: `app/src/main/java/com/pennywiseai/tracker/ui/components/PennyWiseBottomNavigation.kt`.
> Everything below is either quoted from that repo or a stated inference. Values in
> **bold** are the literal constants you must reproduce to get the same look.

The bar is **not** a `Scaffold(bottomBar = …)`. It is a floating overlay drawn on top of
full-bleed content, in one of two mutually exclusive styles chosen at runtime by a
DataStore preference, and both styles share one Haze blur pipeline.

---

## 0. Source map

| Concern | File |
| :-- | :-- |
| The bar itself (both styles) | `ui/components/PennyWiseBottomNavigation.kt` |
| Nav item model (route/title/icon) | `presentation/navigation/BottomNavItem.kt` |
| Host, Haze wiring, route gating | `ui/MainScreen.kt` (~line 600–620) |
| Blur-while-animating visibility | `ui/effects/BlurredAnimatedVisibility.kt` |
| Conditional haze helpers | `ui/effects/BlurEffects.kt` (`LocalBlurEffects`, `conditionalHazeSource/Effect`) |
| Style enum | `data/preferences/NavBarStyle.kt` (`NORMAL`, `FLOATING`) |
| Persistence + UI state | `data/preferences/UserPreferencesRepository.kt` (key `NAV_BAR_STYLE`, `BLUR_EFFECTS_ENABLED`), `ui/viewmodel/ThemeViewModel.kt` (`updateNavBarStyle`, `updateBlurEffects`) |
| Settings picker UI | `ui/screens/settings/AppearanceScreen.kt` → `NavBarStyleSelector` (~line 597) |
| Tokens | `ui/theme/Dimensions.kt`, `ui/theme/Spacing.kt`, `ui/theme/Shape.kt` |
| Theme host | `ui/theme/Theme.kt` (`PennyWiseTheme` → **`MaterialExpressiveTheme`**) |

---

## 1. Architecture: the overlay pattern (effect #0)

```kotlin
// MainScreen.kt — content and bar are siblings in one Box; the bar is NOT scaffold padding
Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    NavHost(
        navController, startDestination = "home",
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),          // ← the blur *source* is the whole content area
        enterTransition = { EnterTransition.None },   // tabs never cross-fade
        exitTransition  = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition  = { ExitTransition.None },
    ) { … }

    if (baseRoute in listOf("home", "transactions", "analytics", "chat")) {
        PennyWiseBottomNavigation(
            navController = navController,
            currentDestination = navBackStackEntry?.destination,
            navBarStyle = themeState.navBarStyle,          // NORMAL | FLOATING
            blurEffects = themeState.blurEffectsEnabled,   // global blur kill-switch
            hazeState = hazeState,                         // shared with content
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
```

Consequences you must replicate:

1. **One `HazeState` is created in the host** (`remember { HazeState() }`) and handed down.
   The content is the `hazeSource`; the bar is the `hazeEffect`. They are different
   composables — the blur only works if the source is drawn *behind* the effect.
2. **Content draws full-bleed under the bar** — that is the whole point (you see list rows
   smeared through the glass). Each tab screen therefore adds its own bottom clearance
   (§10) instead of relying on `Scaffold` padding.
3. **Route gate** — the bar is composed only on top-level routes. Detail screens
   (`settings`, `faq`, `add_account`, …) have no bar, so nothing has to animate it away.
4. **No tab cross-fade** — `EnterTransition.None` everywhere keeps tab switches instant;
   the only motion is the bar's own selection animation.
5. **Back from a non-home tab goes Home, not back** (`BackHandler` in `MainScreen`) so a
   tab switch can never leave an empty stack behind the overlay.

---

## 2. Effect inventory (the whole list)

| # | Effect | Style | Mechanism | Values |
| :-- | :-- | :-- | :-- | :-- |
| 1 | Backdrop blur (frosted glass) | both | Haze `hazeEffect` over `hazeSource` | **blurRadius 20.dp**, `noiseFactor -1f` (= lib default 0.15), `backgroundColor = Transparent` |
| 2 | Unbounded blur edges | both | `blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded` | blur bleeds past the bar bounds → no hard seam at the top edge |
| 3 | Surface tint through the glass | NORMAL only | `tint = HazeDefaults.tint(containerColor)` | `surface` @ **0.7 alpha** (Haze's auto-tint opacity) |
| 4 | No tint through the glass | FLOATING only | style omits `tint`, background is `Transparent` → auto-tint is a no-op | tint instead comes from the toolbar's own `surfaceContainerLow@0.7` |
| 5 | Translucent container | both | colour `.copy(alpha = …)` | NORMAL `surface@0.5`, FLOATING `surfaceContainerLow@0.7` (both → **1f** when blur is off) |
| 6 | Hairline top divider | NORMAL | `HorizontalDivider` above the bar | **1.dp** (`Dimensions.Component.dividerThickness`), `outlineVariant` |
| 7 | Tonal elevation | NORMAL | `NavigationBar(tonalElevation = …)` | **1.dp** (`Dimensions.Elevation.raisedCard`) |
| 8 | Surface gradient scrim | FLOATING | `Brush.verticalGradient(Transparent → surface)` behind the toolbar | full width, bottom-aligned; fades scrolling content into the surface |
| 9 | Drop shadow (blur-off fallback) | FLOATING | `.shadow(elevation, shape)` | **16.dp** when blur off, **0.dp** when blur on (glass reads as depth, shadow would muddy it) |
| 10 | Shape clip | FLOATING | `.clip(FloatingToolbarDefaults.ContainerShape)` | M3's floating-toolbar container shape (stadium) |
| 11 | Z-order pin | FLOATING | `.zIndex(1000f)` | keeps the toolbar above sibling overlays in the same `Box` |
| 12 | Nav-bar inset handling | NORMAL / FLOATING | M3 insets / explicit `.navigationBarsPadding()` | NORMAL gets it free from `NavigationBarDefaults.windowInsets`; FLOATING must add it manually |
| 13 | Blur-during-animation | both | `BlurredAnimatedVisibility` + `graphicsLayer { renderEffect = BlurEffect(r, r, TileMode.Decal) }` | radius **5 → 0**, 300 ms `tween`, alpha **0.95 → 1.0** |
| 14 | Slide + fade enter/exit | both | `fadeIn() + slideInVertically { it }` / `fadeOut() + slideOutVertically { it }` | Compose defaults (`spring(StiffnessMediumLow)` fade, `spring(IntOffset.VisibilityThreshold)` slide) |
| 15 | Cross-fade between styles | both | two sibling `BlurredAnimatedVisibility` with `visible = visible && style == X` | switching style in Settings blurs one out and the other in, simultaneously |
| 16 | Active indicator pill | NORMAL | `NavigationBarItemDefaults.colors(indicatorColor = primaryContainer)` | M3's 64×32dp stadium pill + built-in show/hide animation |
| 17 | Selected icon tint | NORMAL | explicit `Icon(tint = …)` | `onPrimaryContainer` when the pill shows, `primary` when `hidePill` |
| 18 | Unselected icon tint | both | explicit tint | `onSurfaceVariant` |
| 19 | Icon size step-up | NORMAL | `Modifier.size(if (hidePill && hideLabels) large else medium)` | **32.dp** when the glyph is the *only* selection signal, else **24.dp** |
| 20 | Label colour / type | NORMAL | `Text(color = primary | onSurfaceVariant, style = labelMedium)` | `MaterialTheme.typography.labelMedium` |
| 21 | Toggle-button colour morph | FLOATING | `ToggleButtonDefaults.toggleButtonColors(...)` | container `surfaceBright@0.6`, checked container `tertiaryContainer@0.6`, content `inverseSurface` / `onTertiaryContainer` |
| 22 | Expanding label (the signature effect) | FLOATING | `AnimatedVisibility(selected)` `fadeIn+expandHorizontally` / `fadeOut+shrinkHorizontally` | `MaterialTheme.motionScheme.fastSpatialSpec()`, text `padding(start = 8.dp)` |
| 23 | Toggle shape morph | FLOATING | inherent to M3 Expressive `TonalToggleButton` | unchecked shape ⇄ checked shape morph |
| 24 | Item spacing | FLOATING | `Modifier.padding(horizontal = Spacing.xs)` | **4.dp** |
| 25 | Tab restore semantics | both | `navController.navigate { popUpTo(startDestinationId){saveState=true}; launchSingleTop=true; restoreState=true }` | per-tab scroll position & back stack survive |
| 26 | Query-arg-safe selection | both | `it.route?.substringBefore('?') == item.route` | `transactions?type=…` still lights the Transactions tab |

---

## 3. Style `NORMAL` — docked frosted `NavigationBar`

```kotlin
BlurredAnimatedVisibility(
    visible = visible && navBarStyle == NavBarStyle.NORMAL,
    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
    exit  = fadeOut() + slideOutVertically(targetOffsetY = { it }),
) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = Dimensions.Component.dividerThickness,   // 1.dp
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = if (blurEffects) 0.5f else 1f            // glass vs solid
            ),
            tonalElevation = Dimensions.Elevation.raisedCard,     // 1.dp
            modifier = Modifier.then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(containerColor),  // surface @ 0.7
                            blurRadius = 20.dp,
                            noiseFactor = -1f,                         // library default
                        )
                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                    }
                ) else Modifier
            ),
        ) {
            navigationItems.forEach { item -> /* NavigationBarItem — see below */ }
        }
    }
}
```

**Effect notes (why each line exists):**

- **Divider + glass, not divider + solid.** The 1dp `outlineVariant` line is what separates
  the bar from the content when the container is 50% transparent; without it a translucent
  bar dissolves into a light background.
- **`surface@0.5` + tint `surface@0.7` is the whole recipe.** The container alpha controls
  how much of the blurred image shows; the Haze tint re-establishes contrast so text/icons
  stay legible over arbitrary content. Turning blur off bumps alpha to `1f` and the bar
  becomes a plain M3 surface — one switch, two coherent looks.
- **`Unbounded` edge treatment** stops the blur from being clipped into a rectangle with a
  visible cut line at the bar's top edge.
- **Tonal elevation 1dp** is deliberately almost nothing: depth comes from the blur + divider,
  not from elevation. `Dimensions.Elevation` documents the rule — *"the app is predominantly
  flat… only genuinely floating things get elevation"* (bar 3dp, FAB 6dp, dialog 8dp).

**Item:**

```kotlin
val selected = currentDestination?.hierarchy?.any {
    it.route?.substringBefore('?') == item.route
} == true

NavigationBarItem(
    selected = selected,
    onClick = { navController.navigate(item.route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    } },
    icon = {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (selected) {
                if (hidePill) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onPrimaryContainer
            } else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(
                if (hidePill && hideLabels) Dimensions.Icon.large    // 32.dp
                else Dimensions.Icon.medium                          // 24.dp
            ),
        )
    },
    label = if (hideLabels) null else {{
        Text(item.title,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium)
    }},
    colors = NavigationBarItemDefaults.colors(
        indicatorColor = if (hidePill) Color.Transparent
                         else MaterialTheme.colorScheme.primaryContainer
    ),
)
```

The `hidePill` / `hideLabels` flags are the *degradation ladder*:

| Config | Selection signal | Icon size |
| :-- | :-- | :-- |
| pill + label (default) | `primaryContainer` pill + `onPrimaryContainer` glyph + `primary` label | 24.dp |
| `hidePill` | glyph turns `primary`, label turns `primary` | 24.dp |
| `hidePill && hideLabels` | **only** the glyph — so it grows to 32.dp to carry the weight | 32.dp |

---

## 4. Style `FLOATING` — expressive toolbar with expanding pills

```kotlin
BlurredAnimatedVisibility(
    visible = visible && navBarStyle == NavBarStyle.FLOATING,
    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
    exit  = fadeOut() + slideOutVertically(targetOffsetY = { it }),
    modifier = Modifier.align(Alignment.BottomCenter),
) {
    Box(
        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent,
                                                      MaterialTheme.colorScheme.surface))),
        contentAlignment = Alignment.BottomCenter,
    ) {
        HorizontalFloatingToolbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()                       // toolbar does NOT self-inset
                .shadow(elevation = if (blurEffects) 0.dp else 16.dp,
                        shape = MaterialTheme.shapes.extraLarge)
                .clip(FloatingToolbarDefaults.ContainerShape)
                .then(if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,   // ⇒ no auto-tint
                            blurRadius = 20.dp,
                            noiseFactor = -1f,
                        )
                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                    }
                ) else Modifier)
                .zIndex(1000f),
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    .copy(alpha = if (blurEffects) 0.7f else 1f),
            ),
            expanded = true,
        ) {
            navigationItems.forEach { item -> /* TonalToggleButton — see below */ }
        }
    }
}
```

**Effect notes:**

- **The gradient scrim is a second, quieter "fade to surface".** `Transparent → surface`
  over the full width behind the toolbar means content scrolling under the floating bar
  dissolves before it reaches the pill. This is what stops a floating bar from looking like
  it is floating over *content that is cut off*.
- **`shadow = 0.dp` when blur is on.** Haze already separates the toolbar from the content;
  a 16dp shadow *plus* glass looks dirty. The shadow only returns as the no-blur fallback.
- **Modifier order is load-bearing:** `shadow → clip → hazeEffect`. `shadow` must come
  before `clip` (it draws outside the shape) and `hazeEffect` must come after `clip` (the
  blur is applied inside the clipped region, then bleeds `Unbounded`).
- **⚠ Inconsistency worth not copying blindly:** the shadow uses
  `MaterialTheme.shapes.extraLarge` (28dp) while the clip uses
  `FloatingToolbarDefaults.ContainerShape`. If those two shapes differ in your theme the
  shadow outline will not match the clipped outline. Use one shape for both.
- **`expanded = true`** — the toolbar is permanently expanded; only the *items* animate.
- Insets: `HorizontalFloatingToolbar` does not apply window insets, hence the explicit
  `.navigationBarsPadding()`; the NORMAL branch gets insets for free from M3's
  `NavigationBarDefaults.windowInsets`.

**Item — the expanding pill:**

```kotlin
TonalToggleButton(
    checked = selected,
    onCheckedChange = { /* same navigate{...} block as NORMAL */ },
    colors = ToggleButtonDefaults.toggleButtonColors(
        containerColor        = if (blurEffects) surfaceBright.copy(0.6f) else surfaceBright,
        contentColor          = MaterialTheme.colorScheme.inverseSurface,
        disabledContainerColor= surfaceBright.copy(0.7f),
        disabledContentColor  = inverseSurface.copy(0.5f),
        checkedContainerColor = if (blurEffects) tertiaryContainer.copy(0.6f)
                                else tertiaryContainer,
        checkedContentColor   = MaterialTheme.colorScheme.onTertiaryContainer,
    ),
    modifier = Modifier.padding(horizontal = Spacing.xs),   // 4.dp
) {
    Icon(item.icon, contentDescription = item.title)
    AnimatedVisibility(
        visible = selected,
        enter = fadeIn() + expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
        exit  = fadeOut() + shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
    ) {
        Text(item.title, modifier = Modifier.padding(start = Spacing.sm))   // 8.dp
    }
}
```

The signature motion: **the selected tab is the only one with a label.** Because the label
is inside `AnimatedVisibility` with `expandHorizontally`/`shrinkHorizontally`, the button
itself grows and shrinks — a pill that "opens" as you land on the tab and "closes" as you
leave, and the neighbouring buttons slide to make room (the toolbar is a `Row`, so the row
re-centres as the widths animate). `fadeIn/fadeOut` keeps the text from clipping during the
width change.

---

## 5. `BlurredAnimatedVisibility` — the blur-during-motion effect

```kotlin
val transition = updateTransition(visible, label = "blurTransition")
val blurRadius by transition.animateFloat(
    label = "blurRadius", transitionSpec = { tween(300) }
) { state -> if (state && transition.currentState == transition.targetState) 0f else 5f }

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    AnimatedVisibility(visible, modifier, enter, exit) {
        Box(Modifier.graphicsLayer {
            renderEffect = BlurEffect(blurRadius, blurRadius, edgeTreatment = TileMode.Decal)
            alpha = 0.95f + (0.05f * (1f - blurRadius / 5f))
        }) { content() }
    }
} else { /* CustomBlurEffect: RenderScript, 0.25-scale bitmap, radius×5 clamped 0.1..25 */ }
```

- **What it looks like:** the bar blurs *while it is moving* and sharpens the moment it
  settles — a motion-blur cue. Alpha rides with it (0.95 while blurred → 1.0 sharp), so the
  bar also reads as very slightly translucent mid-flight.
- **API 31+** path is `RenderEffect` inside `graphicsLayer` (cheap, GPU).
- **API 29–30** falls back to a hand-rolled RenderScript pass that renders into a
  ¼-scale bitmap — *this path is expensive and, in the upstream code, draws a stale scaled
  bitmap and then blurs it; treat it as "best effort" or skip it entirely* (see §11).
- The `enter`/`exit` params are overridden by both call sites to slide+fade, so the default
  `scaleIn/scaleOut` never runs here.

---

## 6. Haze glass recipe (copy-paste constants)

```kotlin
// shared HazeState at the host, on the scrolling content:
Modifier.hazeSource(hazeState)

// on the bar (NORMAL — tinted):
Modifier.hazeEffect(state = hazeState) {
    style = HazeDefaults.style(
        backgroundColor = Color.Transparent,
        tint = HazeDefaults.tint(MaterialTheme.colorScheme.surface),  // ≈ surface @ 70%
        blurRadius = 20.dp,
        noiseFactor = -1f,                                            // default 0.15
    )
    blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
}

// on the bar (FLOATING — untinted; the container colour does the tinting):
Modifier.hazeEffect(state = hazeState) {
    style = HazeDefaults.style(backgroundColor = Color.Transparent,
                               blurRadius = 20.dp, noiseFactor = -1f)
    blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
}
```

Library behaviour you rely on (Haze `1.7.1`): default `blurRadius` is `20.dp`, default
noise `0.15f` (`-1f` means "use the default"), and when no `tint` is supplied Haze auto-tints
with the **background colour at 70% opacity** — which is why `backgroundColor = Transparent`
yields an untinted glass.

---

## 7. Items, selection and navigation semantics

```kotlin
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    data object Transactions : BottomNavItem("transactions", "Transactions",
        Icons.AutoMirrored.Filled.ReceiptLong)
    data object Analytics : BottomNavItem("analytics", "Analytics", Icons.Default.Analytics)
    data object Chat : BottomNavItem("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
}
val navigationItems = listOf(Home, Transactions, Analytics, Chat)
```

- **Exactly 4 items**; the list is rebuilt inside the composable (no state to keep in sync).
- Icons are **`ImageVector` from `material-icons-extended`** — mix `Icons.Default.*` and
  `Icons.AutoMirrored.Filled.*` so RTL languages mirror the meaningful ones (receipt, chat)
  but not the symmetric ones (home, analytics).
- **Selection:** `destination.hierarchy.any { it.route?.substringBefore('?') == item.route }`.
  Two things matter: `hierarchy` (nested graphs still select their parent tab) and
  `substringBefore('?')` (deep links like `transactions?type=credit` still select it).
- **Navigation:** `popUpTo(startDestinationId) { saveState = true }` + `launchSingleTop` +
  `restoreState` — the canonical "bottom nav with per-tab stacks" recipe. Never
  `navigate(route)` bare; you will stack duplicate tabs and lose scroll position.

---

## 8. Tokens & dependency versions

**Tokens used by the bar** (`ui/theme/Dimensions.kt`, `Spacing.kt`, `Shape.kt`):

| Token | Value | Use in the bar |
| :-- | :-- | :-- |
| `Spacing.xxs / xs / sm / md` | 2 / **4** / **8** / 16 dp | toggle padding (4), label gap (8) |
| `Dimensions.Icon.medium` | **24.dp** | nav icon |
| `Dimensions.Icon.large` | **32.dp** | nav icon when it is the only selection cue |
| `Dimensions.Elevation.raisedCard` | **1.dp** | NORMAL tonal elevation |
| `Dimensions.Component.dividerThickness` | **1.dp** | top hairline |
| `Dimensions.Component.bottomBarHeight` | **80.dp** | content clearance (matches M3 `NavigationBar` height) |
| `Dimensions.Component.fabBottomInset` | **96.dp** | FAB stack above the bar |
| `Dimensions.Component.fabScrollClearance` | **112.dp** | list bottom padding on screens that also have FABs |
| `MaterialTheme.shapes` | extraSmall 4 · small 8 · medium 12 · large 16 · **extraLarge 28** | shadow shape on the floating toolbar |

Full spacing scale: `none 0 · xxs 2 · xs 4 · sm 8 · smd 12 · md 16 · lg 24 · xl 32 · xxl 48 · xxxl 64`.

**Versions (from `gradle/libs.versions.toml`):**

| Dep | Version | Needed for |
| :-- | :-- | :-- |
| `composeBom` | `2026.05.01` | baseline |
| `androidx.compose.material3` | `1.5.0-alpha12` | `HorizontalFloatingToolbar`, `TonalToggleButton`, `ToggleButtonDefaults`, `MaterialExpressiveTheme`, `MaterialTheme.motionScheme` |
| `dev.chrisbanes.haze:haze` | `1.7.1` | `HazeState`, `hazeSource`, `hazeEffect`, `HazeDefaults`, `BlurredEdgeTreatment` |
| `androidx.navigation:navigation-compose` | `2.9.6` | `hierarchy`, `saveState`/`restoreState` |
| Kotlin / AGP | `2.3.21` / `9.2.1` | — |

`@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)` is
required on the composable.

---

## 9. Preferences & the settings picker

- `enum class NavBarStyle { NORMAL, FLOATING }`, persisted as `preferences[NAV_BAR_STYLE] = style.name`
  in DataStore, read back with `runCatching { NavBarStyle.valueOf(it) }.getOrDefault(FLOATING)`
  — **a corrupt value silently falls back to FLOATING**.
- `UserPreferencesRepository` defaults `navBarStyle = FLOATING`; `ThemeUiState` defaults to
  `NORMAL` (the UI state is the pre-load value; the stored preference wins once loaded).
- A second master switch, `blurEffectsEnabled` (default **true**), is threaded into the bar
  and flips alpha 0.5↔1, 0.6↔1, shadow 0↔16dp, and whether `hazeEffect` is attached at all.
- Settings UI (`NavBarStyleSelector`): two 80dp-tall cards in a `Row(spaceBy 4dp)`, each
  `weight(1f)`, with **composite radii** — left card `topStart/bottomStart 16dp`,
  `topEnd/bottomEnd 4dp`, right card mirrored — selected card filled with
  `tertiaryContainer` (Floating) / `secondaryContainer` (Normal) and `on*Container` text,
  unselected `surfaceContainerLow` + `onSurfaceVariant`. Title `bodyMedium` **Bold** +
  subtitle `labelSmall` @ 70% alpha ("Modern & Sleek" / "Standard M3"). The whole column is
  `animateContentSize()`.

---

## 10. Content clearance (the part everyone forgets)

Because the bar overlays content, every bar-hosting screen must reserve space itself:

| Screen | Reservation |
| :-- | :-- |
| Home | `contentPadding(bottom = bottomBarHeight + fabScrollClearance)` = 80 + 112 |
| Analytics | `bottom = bottomBarHeight + Spacing.md` = 80 + 16 |
| BudgetGroups / BudgetHistory | `bottom = bottomBarHeight + Spacing.md` |
| Transactions | `bottomBarClearance = if (reserveBottomBarSpace) bottomBarHeight else 0.dp` — computed, because the screen is also reused *without* the bar |
| Chat | explicit `Spacer(height = bottomBarHeight)` at the end of the list |
| FAB stacks | `padding(end = 16.dp, bottom = fabBottomInset /* 96.dp */)` |

Rule of thumb: **80dp for the bar, +16dp breathing room, +112dp if FABs sit above it.**

---

## 11. Gotchas, traps & anti-patterns

1. **Two `AnimatedVisibility` in one `Box`, both keyed on `style`.** Switching style runs an
   exit and an enter at the same time. That is intentional (cross-fade), but it means the
   composable is briefly drawing *both* bars — keep it cheap and keep `zIndex` honest.
2. **Haze needs draw order, not z-order.** `hazeSource` must be a *different, earlier-drawn*
   composable than the `hazeEffect`. Putting both on the same node, or making the bar itself
   a `hazeSource`, silently produces no blur.
3. **Don't attach `hazeEffect` when blur is disabled.** The upstream code branches to
   `Modifier` and compensates with alpha + shadow; attaching it and just zeroing the radius
   still costs a render pass.
4. **RenderScript fallback (API 29–30) is a trap.** It allocates a ¼-scale `Bitmap` per
   layout and runs a blur per frame during the transition. If you target 26+ (like TeleBox),
   gate `BlurredAnimatedVisibility`'s blur to API 31+ and degrade to a plain slide+fade on
   older devices — visually near-identical, far cheaper.
5. **Modifier order:** `shadow → clip → hazeEffect → zIndex`. Reversing `clip`/`hazeEffect`
   squares the corners of the blur; putting `shadow` after `clip` clips the shadow away.
6. **`navigationBarsPadding()` is only on the FLOATING branch.** M3's `NavigationBar` already
   consumes `NavigationBarDefaults.windowInsets`; adding it again double-counts.
7. **Route matching must strip query args**, or `transactions?focusSearch=true` leaves no tab
   selected and the bar looks broken.
8. **Never `navigate(route)` directly.** Without `popUpTo { saveState }` + `launchSingleTop` +
   `restoreState`, back navigation and scroll restoration break.
9. **Shadow shape ≠ clip shape** in the upstream FLOATING branch (28dp vs
   `FloatingToolbarDefaults.ContainerShape`). Pick one.
10. **MotionScheme requires the expressive theme.** `MaterialTheme.motionScheme` only exists
    under `MaterialExpressiveTheme`. On a plain `MaterialTheme` it will not compile — see §12.
11. **Don't put the bar in `Scaffold(bottomBar = …)`** if you want the glass effect: Scaffold
    insets the content, so there is nothing behind the bar to blur.
12. **Turning blur off is a designed state, not a fallback hack** — keep the alpha/shadow
    compensation; otherwise "blur off" produces a washed-out bar.

---

## 12. Porting this into TeleBox

TeleBox has **no** bottom navigation today: it is a `Scaffold` + `TopBar` + `FileExplorer`
with a permanent/modal `Sidebar` (`ui/dashboard/Sidebar.kt`) listing
`LibrarySection { ALL, VIDEOS, PICTURES, DOCUMENTS, OTHERS }` plus folders. A bottom bar
belongs on **compact** screens only (`ui/theme/Adaptive.kt` → `isCompact` / `useModalDrawer`),
where the drawer is modal.

### 12.1 Dependency deltas (`androidApp/app/build.gradle.kts`)

Today: `compose-bom:2024.12.01`, `material3` (BOM ⇒ 1.3.x), no Haze, no expressive.

```kotlin
// minimum viable jump
implementation(platform("androidx.compose:compose-bom:2025.08.00"))   // or newer
implementation("androidx.compose.material3:material3:1.4.0")          // FloatingToolbar + ToggleButton
implementation("dev.chrisbanes.haze:haze:1.7.1")
```

Ordering caution: bumping the BOM touches every screen. Do it as its own commit and smoke
test the file grid, preview modal and theme before adding the bar.

### 12.2 Two ways to get the FLOATING style without a full expressive migration

PennyWise's `PennyWiseTheme` wraps **`MaterialExpressiveTheme`**, which is what makes
`MaterialTheme.motionScheme` and `TonalToggleButton` available. TeleBox's `TeleBoxTheme`
(`ui/theme/Theme.kt`) wraps plain `MaterialTheme` and layers `TeleBoxExtendedColors`
(`bg, surface, primary, secondary, text, subtext, border, hover, glass, …`).

- **Option A (faithful):** switch `TeleBoxTheme` to `MaterialExpressiveTheme`. Costs: every
  component picks up Expressive motion/shape defaults — expect small diffs app-wide.
- **Option B (surgical, recommended):** keep `MaterialTheme`, and
  1. replace `MaterialTheme.motionScheme.fastSpatialSpec()` with a hand-written spring,
     e.g. `spring(dampingRatio = 0.9f, stiffness = 700f, visibilityThreshold = IntSize.VisibilityThreshold)`
     (tune to ≈150–200 ms settle — the *expressive fast-spatial* feel),
  2. if `TonalToggleButton` is unavailable on your material3 version, rebuild it as
     `Surface(shape, color, onClick) { Row { Icon; AnimatedVisibility { Text } } }` — the
     expanding-label animation is the part worth keeping, not the exact composable.

### 12.3 File plan

| New file | Contents |
| :-- | :-- |
| `ui/components/TeleBoxBottomNavigation.kt` | port of `PennyWiseBottomNavigation` (both styles), parameterised by `navBarStyle`, `blurEffects`, `hazeState` |
| `ui/navigation/BottomNavItem.kt` | sealed class over TeleBox destinations |
| `ui/effects/BlurredAnimatedVisibility.kt` | port, with the blur path gated to API 31+ (see §11.4) |
| `data/NavBarStyle.kt` | `enum class NavBarStyle { NORMAL, FLOATING }` |
| `data/PreferencesStore.kt` | add `NAV_BAR_STYLE` (+ reuse the theme/blur preference if you add one) |
| `viewmodel/ThemeViewModel.kt` | expose `navBarStyle` / `blurEffects` in the theme state |
| `ui/theme/Dimensions.kt` (new) | `bottomBarHeight 80`, `fabBottomInset 96`, `fabScrollClearance 112`, `dividerThickness 1`, `raisedCard 1` |

### 12.4 Wiring into `DashboardScreen`

`DashboardScaffold` (`ui/screens/DashboardScreen.kt` ~line 200) already sets
`contentWindowInsets = WindowInsets.navigationBars` and places the upload/download queue
panels with `bottom = if (compact) 80.dp else 16.dp` — **that 80dp is already the bar
reservation**, so a compact bottom bar slots in with no queue-panel changes. Suggested:

1. Wrap the existing `Box(Modifier.fillMaxSize().padding(innerPadding))` content in
   `Modifier.hazeSource(hazeState)` (the `FileExplorer` list is what should smear).
2. Add the bar as a sibling `Box(Modifier.align(Alignment.BottomCenter))` shown when
   `compact` — i.e. exactly PennyWise's overlay pattern.
3. Nav items for compact: `Files` (`Icons.Outlined.Folder`), `Videos`, `Photos`, `Docs`,
   bound to `viewModel.setLibrarySection(...)` / `setActiveFolder(null)` rather than a
   `NavHost` route — TeleBox has no per-tab back stack, so use the
   `NavigationBarItem(selected = …, onClick = …)` half of the pattern and skip
   `popUpTo/restoreState`.
4. Keep the sidebar for expanded widths (`useModalDrawer == false`) — the bar is a compact
   affordance, not a replacement.
5. On selection mode (`state.selectedIds.isNotEmpty()`), hide the bar with
   `BlurredAnimatedVisibility(visible = false)`; that is what the `visible` parameter exists
   for and it gives you the same blur-out as a style switch.

### 12.5 TeleBox colour mapping

| PennyWise role | TeleBox equivalent |
| :-- | :-- |
| `surface` (bar @0.5) | `TeleBoxTheme.colors.surface` |
| `surfaceContainerLow` (floating @0.7) | `TeleBoxTheme.colors.surface` or `glass` |
| `surfaceBright` (toggle) | `TeleBoxTheme.colors.surface` |
| `primaryContainer` (indicator pill) | `TeleBoxTheme.colors.primary.copy(alpha = 0.18f)` |
| `onPrimaryContainer` (selected glyph) | `TeleBoxTheme.colors.primary` |
| `tertiaryContainer` (checked toggle) | `TeleBoxTheme.colors.primary.copy(alpha = 0.85f)` or `secondary` |
| `onSurfaceVariant` (unselected) | `TeleBoxTheme.colors.subtext` |
| `outlineVariant` (divider) | `TeleBoxTheme.colors.border` |

TeleBox already has `Modifier.yumaGlassCard()` / `yumaClickable()` in
`ui/theme/YumaDesign.kt` — reuse its 0.5dp hairline idiom for the divider if you want the
bar to agree with the rest of the app's glass language.

---

## 13. Replication checklist

- [ ] Content is full-bleed and tagged `hazeSource(hazeState)`; the bar is a sibling drawn on top.
- [ ] One `HazeState` created in the host and passed down to the bar.
- [ ] NORMAL: 1dp `outlineVariant` divider, `surface@0.5`, tonal elevation 1dp, haze 20dp + `tint(surface)`.
- [ ] FLOATING: gradient scrim, `surfaceContainerLow@0.7`, `shadow(0/16dp)`, `clip(ContainerShape)`, haze 20dp untinted, `zIndex(1000f)`, `navigationBarsPadding()`.
- [ ] `hazeEffect` uses `backgroundColor = Transparent`, `noiseFactor = -1f`, `BlurredEdgeTreatment.Unbounded`.
- [ ] Both styles wrapped in `BlurredAnimatedVisibility` with `fade + slideVertically { it }`.
- [ ] Selection: `hierarchy.any { route.substringBefore('?') == item.route }`.
- [ ] Navigation: `popUpTo(startDestinationId){saveState} + launchSingleTop + restoreState`.
- [ ] `hidePill` ⇒ transparent indicator + `primary` glyph; `hidePill && hideLabels` ⇒ 32dp glyph.
- [ ] FLOATING item: `TonalToggleButton` + `AnimatedVisibility` `expand/shrinkHorizontally` label.
- [ ] Blur-off path: alphas → 1f, shadow → 16dp, no `hazeEffect`.
- [ ] Bar-hosting screens reserve 80dp (+16dp, +112dp with FABs).
- [ ] Style persisted in DataStore with a safe `valueOf` fallback; blur switch wired through.
- [ ] Verified in light, dark, AMOLED, blur-on, blur-off, RTL, and with TalkBack (M3 keeps
      `HorizontalFloatingToolbar` expanded and disables scroll-hide when touch exploration is on).

---

## 14. Appendix — minimal replication skeleton

```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TeleBoxBottomNavigation(
    items: List<BottomNavItem>,
    selectedRoute: String?,
    onSelect: (BottomNavItem) -> Unit,
    style: NavBarStyle,
    blurEffects: Boolean,
    hazeState: HazeState,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier) {
        BlurredAnimatedVisibility(
            visible = visible && style == NavBarStyle.NORMAL,
            enter = fadeIn() + slideInVertically { it },
            exit  = fadeOut() + slideOutVertically { it },
        ) {
            Column(Modifier.fillMaxWidth()) {
                HorizontalDivider(thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = surface.copy(alpha = if (blurEffects) 0.5f else 1f),
                    tonalElevation = 1.dp,
                    modifier = Modifier.then(if (blurEffects) Modifier.hazeEffect(hazeState) {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(surface),
                            blurRadius = 20.dp, noiseFactor = -1f)
                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                    } else Modifier),
                ) {
                    items.forEach { item ->
                        val selected = selectedRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onSelect(item) },
                            icon = { Icon(item.icon, item.title,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)) },
                            label = { Text(item.title, style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                }
            }
        }

        BlurredAnimatedVisibility(
            visible = visible && style == NavBarStyle.FLOATING,
            enter = fadeIn() + slideInVertically { it },
            exit  = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, surface))),
                contentAlignment = Alignment.BottomCenter) {
                val shape = MaterialTheme.shapes.extraLarge       // use ONE shape for clip+shadow
                HorizontalFloatingToolbar(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .shadow(if (blurEffects) 0.dp else 16.dp, shape)
                        .clip(shape)
                        .then(if (blurEffects) Modifier.hazeEffect(hazeState) {
                            style = HazeDefaults.style(backgroundColor = Color.Transparent,
                                blurRadius = 20.dp, noiseFactor = -1f)
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        } else Modifier)
                        .zIndex(1000f),
                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                        toolbarContainerColor =
                            MaterialTheme.colorScheme.surfaceContainerLow
                                .copy(alpha = if (blurEffects) 0.7f else 1f)),
                    expanded = true,
                ) {
                    items.forEach { item ->
                        val selected = selectedRoute == item.route
                        TonalToggleButton(
                            checked = selected,
                            onCheckedChange = { onSelect(item) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Icon(item.icon, contentDescription = item.title)
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn() + expandHorizontally(),
                                exit  = fadeOut() + shrinkHorizontally(),
                            ) { Text(item.title, modifier = Modifier.padding(start = 8.dp)) }
                        }
                    }
                }
            }
        }
    }
}
```

> Related skill kept in this repo: [`docs/skills/yumaplayer-uiux-analysis.md`](docs/skills/yumaplayer-uiux-analysis.md)
> (Yuma Design System 2.1 — glass surfaces, segmented rows, press physics). When porting the
> bar into TeleBox, take the *structure* from this skill and the *surface language* from that
> one.
