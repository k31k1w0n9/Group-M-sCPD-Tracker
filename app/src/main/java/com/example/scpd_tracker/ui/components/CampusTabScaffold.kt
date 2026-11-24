package com.example.scpd_tracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.R
import com.example.scpd_tracker.ui.theme.ScpdTheme
import kotlin.math.roundToInt

/** Shared root header. Search stays in one place in the composition as the campus panel collapses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampusTabScaffold(
    title: String,
    subtitle: String,
    expandedTitle: String = title,
    mascot: CampusMascot = CampusMascot.Wave,
    context: (@Composable ColumnScope.() -> Unit)? = null,
    search: (@Composable () -> Unit)? = null,
    contentOverlap: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val hero = ScpdTheme.colors
    val appBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = appBarState)
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val density = LocalDensity.current
        val keyboardHeight = with(density) { WindowInsets.ime.getBottom(this).toDp() }
        val titleSwitchPx = with(density) { 20.dp.roundToPx() }
        val showsCompactTitle by remember(appBarState, titleSwitchPx) {
            derivedStateOf {
                (appBarState.heightOffset - appBarState.heightOffsetLimit).roundToInt() <= titleSwitchPx
            }
        }
        val compactOnly = maxHeight + keyboardHeight < 480.dp
        Column(Modifier.fillMaxSize().then(if (compactOnly) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection))) {
            Box(Modifier.fillMaxWidth().background(colors.primary)) {
                if (!compactOnly) {
                    CampusBackground(
                        Modifier.matchParentSize().graphicsLayer {
                            alpha = (1f - appBarState.collapsedFraction * 1.25f).coerceAtLeast(0f)
                        },
                        home = true,
                    )
                }
                Column(Modifier.statusBarsPadding()) {
                    val compactTitle: @Composable (Modifier) -> Unit = { modifier ->
                        ReadableWidth(modifier) {
                            Box(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                    .padding(horizontal = spacing.gutter, vertical = spacing.space2),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, color = hero.onHero)
                            }
                        }
                    }
                    if (compactOnly) {
                        compactTitle(Modifier)
                    } else {
                        Layout(
                            content = {
                                ReadableWidth(if (showsCompactTitle) Modifier.clearAndSetSemantics {} else Modifier) {
                                    BrandIdentities(Modifier.heightIn(min = 78.dp).padding(horizontal = spacing.gutter))
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 114.dp)
                                            .padding(horizontal = spacing.gutter, vertical = spacing.space3),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
                                    ) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                                            if (expandedTitle != title) {
                                                Text(title, style = MaterialTheme.typography.titleMedium, color = hero.onHero)
                                            }
                                            Text(
                                                expandedTitle, Modifier.semantics { heading() },
                                                style = MaterialTheme.typography.headlineMedium, color = hero.onHero,
                                            )
                                            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = hero.onHeroMuted)
                                        }
                                        GilbertSprite(Modifier.size(75.dp), mascot.scale, mascot.x, mascot.y)
                                    }
                                    if (context != null) {
                                        Column(
                                            Modifier.padding(start = spacing.gutter, end = spacing.gutter, bottom = spacing.space4),
                                            verticalArrangement = Arrangement.spacedBy(spacing.space2),
                                            content = context,
                                        )
                                    }
                                }
                                compactTitle(if (showsCompactTitle) Modifier else Modifier.clearAndSetSemantics {})
                            },
                            modifier = Modifier.fillMaxWidth().clipToBounds(),
                        ) { measurables, constraints ->
                            val looseHeight = constraints.copy(minHeight = 0)
                            val expanded = measurables[0].measure(looseHeight)
                            val compact = measurables[1].measure(looseHeight)
                            val limit = (compact.height - expanded.height).coerceAtMost(0).toFloat()
                            if (appBarState.heightOffsetLimit != limit) appBarState.heightOffsetLimit = limit
                            val offset = appBarState.heightOffset.coerceIn(limit, 0f)
                            val height = expanded.height + offset.roundToInt()
                            layout(constraints.maxWidth, height) {
                                if (height - compact.height <= titleSwitchPx) {
                                    compact.placeRelative(0, 0)
                                } else {
                                    expanded.placeRelative(0, offset.roundToInt())
                                }
                            }
                        }
                    }
                    if (search != null) {
                        ReadableWidth {
                            Box(Modifier.fillMaxWidth().padding(start = spacing.gutter, end = spacing.gutter, bottom = spacing.space4)) {
                                search()
                            }
                        }
                    }
                }
            }
            if (contentOverlap == 0.dp) {
                content()
            } else {
                Column(
                    Modifier.weight(1f).fillMaxWidth().layout { measurable, constraints ->
                        // Profile's avatar crosses the extended edge. The scroll pane clears the title
                        // bar as it collapses, and starts below it on short or large-text layouts.
                        val overlap = if (compactOnly || fontScale >= 1.3f) 0 else
                            (contentOverlap.toPx() * (1f - 2f * appBarState.collapsedFraction).coerceAtLeast(0f)).roundToInt()
                        val body = measurable.measure(constraints.copy(
                            minHeight = constraints.minHeight + overlap,
                            maxHeight = constraints.maxHeight + overlap,
                        ))
                        layout(body.width, body.height - overlap) { body.placeRelative(0, -overlap) }
                    },
                    content = content,
                )
            }
        }
    }
}

/** Artwork positions on the same sprite sheet used in the Refined v2 headers. */
internal enum class CampusMascot(val scale: Float, val x: Float, val y: Float) {
    Wave(3.9809f, -0.0095f, -0.0476f),
    Calendar(3.9559f, -0.0063f, -1.0568f),
    Scan(4f, -1f, -2.04f),
    Garden(4.1523f, -2.0894f, -3.1523f),
    Profile(3.9809f, -2.9619f, -3.02f),
}

@Composable
internal fun GilbertSprite(modifier: Modifier, scale: Float, x: Float, y: Float) {
    Box(modifier.clipToBounds()) {
        Image(
            painterResource(R.drawable.gilbert_sheet),
            contentDescription = null,
            modifier = Modifier.matchParentSize().graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
                translationX = x * size.width
                translationY = y * size.height
            },
            contentScale = ContentScale.FillBounds,
        )
    }
}
