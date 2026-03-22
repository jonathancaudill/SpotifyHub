# Compose Overscroll Performance Report

Date: March 22, 2026

## Scope

This report covers the custom Compose overscroll modifier implemented in
[app/src/main/java/com/spotifyhub/ui/common/BounceOverscroll.kt](/Users/jonathancaudill/Programming/SpotifyHub/app/src/main/java/com/spotifyhub/ui/common/BounceOverscroll.kt).

This is a static implementation review plus build verification. It is not a device-profiled benchmark report. No `Macrobenchmark`, `Perfetto`, `adb shell dumpsys gfxinfo`, or frame-time capture was run in this workspace.

## What The Modifier Adds

Per decorated scroll surface, the implementation adds:

- One `NestedScrollConnection`
- One `Animatable<Float>`
- One translated `graphicsLayer`
- Lightweight float math for drag and fling overflow handling

The modifier does not remeasure or deform child items during overscroll. It translates the already-laid-out scroll container on the render layer.

## Cost Breakdown

### CPU

Expected impact: low.

Hot-path work during drag:

- Read the relevant axis from `Offset`
- Apply resistance
- Clamp to max offset
- Snap the animated offset

Hot-path work during edge fling:

- Read leftover fling velocity
- Run a bounded decay animation
- Run a spring-back animation

This is substantially cheaper than approaches that mutate each row/card or trigger relayout.

### GPU / Rendering

Expected impact: very low to low.

The visual effect is a container translation using `translationX` or `translationY`. That is generally a cheap operation because the list moves as one layer instead of redrawing each child with deformation.

### Memory

Expected impact: negligible.

The modifier adds one animation object and a few state values per decorated list.

## Real Overhead Sources In This Implementation

1. Every decorated list participates in nested-scroll interception.
2. Overscroll drag updates issue repeated `snapTo` updates.
3. Edge flings now run an extra decay and spring animation when leftover velocity exists.
4. Each decorated list renders through a translated graphics layer while overscrolled.

## Why This Should Stay Cheap

The important performance property is that overscroll happens at the container level, not the item level.

That means:

- no per-item transforms
- no per-item animation bookkeeping
- no extra measure/layout pass caused by overscroll itself
- no Android 12+ stretch deformation pipeline

## Main Risks

1. Several independently overscrolling lists animating at once will stack the layer-animation cost.
2. Repeated drag updates still create animation-state churn, even though the work is small.
3. Very image-heavy screens may show jank from image decode/bind work long before this overscroll modifier becomes the dominant cost.

## Current Conclusion

Expected performance hit: low.

Given the current implementation shape, the most likely user-visible effect is a small increase in per-frame work during aggressive overscroll interaction, not a broad regression in normal scrolling.

For this app, the larger performance variables are still likely to be:

- image loading
- list content complexity
- crossfade / screen transition work

## What Is Missing For A True Measured Report

To turn this into an empirical report with frame numbers, run:

1. `Macrobenchmark` scroll tests on Home, Library, Search, and Detail
2. `Perfetto` frame timeline capture on an Android 12+ device
3. `adb shell dumpsys gfxinfo <package>` before and after this feature

That would let us report actual frame-time deltas and jank percentages instead of a static engineering estimate.
