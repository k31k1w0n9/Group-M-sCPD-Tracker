package com.example.scpd_tracker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.ui.theme.ScpdTheme

private data class InterestArtwork(
    val label: String, val image: Int, val logo: Int,
    val logoX: Float, val logoY: Float, val logoWidth: Float, val logoHeight: Float,
)

// Logo positions are in the 128dp artwork's own coordinates; InterestTile scales them to the real size.
private val InterestChoices = listOf(
    InterestArtwork("Leadership", R.drawable.interest_leadership, R.drawable.interest_logo_leadership, 56.4f, 60.4f, 10.8f, 3.947f),
    InterestArtwork("Sports", R.drawable.interest_sports, R.drawable.interest_logo_sports, 58.8f, 60f, 9.6f, 3.509f),
    InterestArtwork("Tech", R.drawable.interest_tech, R.drawable.interest_logo_tech, 50f, 61.6f, 6.8f, 2.485f),
    InterestArtwork("Volunteering", R.drawable.interest_volunteering, R.drawable.interest_logo_volunteering, 59.2f, 64.8f, 7.2f, 2.631f),
    InterestArtwork("Arts", R.drawable.interest_arts, R.drawable.interest_logo_arts, 56.8f, 62.4f, 9.2f, 3.362f),
    InterestArtwork("Career", R.drawable.interest_career, R.drawable.interest_logo_career, 57.2f, 64.8f, 10f, 3.655f),
)

private const val ArtworkSize = 128f

@Composable
internal fun InterestsScreen(
    selected: List<String>, onChange: (List<String>) -> Unit, onBack: () -> Unit, onContinue: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val spacing = ScpdTheme.spacing
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        CompactHeader("Make it yours", onBack)
        ReadableWidth(Modifier.weight(1f)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = spacing.gutter, vertical = spacing.space3),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                // One heading and one line of guidance; the old overline and subtitle repeated the same idea.
                Text("What sparks\nyour curiosity?", Modifier.semantics { heading() }, style = type.headlineLarge)
                Text("Pick a few. We’ll recommend events that match.", style = type.bodyLarge)
                BoxWithConstraints {
                    // Three tiles per row once there is room (landscape, tablets), two on phones in portrait.
                    val columns = if (maxWidth >= 520.dp) 3 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                        InterestChoices.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                                row.forEach { interest ->
                                    InterestTile(interest, interest.label in selected, Modifier.weight(1f)) {
                                        onChange(if (interest.label in selected) selected - interest.label else selected + interest.label)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        BottomActionBar(supporting = "You can change these later in Profile.") {
            PrimaryAction(
                if (selected.size == 1) "Continue with 1 interest" else "Continue with ${selected.size} interests",
                enabled = selected.isNotEmpty(), onClick = onContinue,
            )
        }
    }
}

@Composable
private fun InterestTile(art: InterestArtwork, selected: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() }).padding(spacing.space3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            // Square artwork that shrinks on narrow screens instead of overflowing a fixed 128dp box.
            BoxWithConstraints(Modifier.widthIn(max = ArtworkSize.dp).fillMaxWidth().aspectRatio(1f)) {
                val scale = maxWidth.value / ArtworkSize
                Image(painterResource(art.image), null, Modifier.fillMaxSize())
                Image(
                    painterResource(art.logo), null,
                    Modifier.offset((art.logoX * scale).dp, (art.logoY * scale).dp)
                        .size((art.logoWidth * scale).dp, (art.logoHeight * scale).dp),
                )
            }
            Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(art.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                // The checkbox role already announces "selected"; the old "Tap to choose" line was redundant.
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null, Modifier.size(20.dp),
                    tint = if (selected) colors.primary else colors.outline,
                )
            }
        }
    }
}
