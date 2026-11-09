package com.example.scpd_tracker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun RolePicker(selected: AccountRole, onSelect: (AccountRole) -> Unit, onContinue: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val type = MaterialTheme.typography
    EntryCanvas(expanded = true, rolePicker = true) {
        Text("Welcome to s-CPD", Modifier.semantics { heading() }, style = type.headlineSmall)
        Text("Choose your role before you sign in.", style = type.titleSmall)
        Column(
            Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            RoleOption(AccountRole.Committee, "Organise club events and manage attendance.", Icons.Rounded.Groups, selected, onSelect)
            RoleOption(AccountRole.Student, "Discover events and track your development.", Icons.Rounded.School, selected, onSelect)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Info, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Your university sign-in confirms which role you can use.", style = type.bodySmall)
        }
        // Form actions close the form, so the primary action is always the last item.
        PrimaryAction("Continue to sign in", teal = true, onClick = onContinue)
    }
}

@Composable
private fun RoleOption(
    role: AccountRole, description: String, icon: ImageVector, selectedRole: AccountRole, onSelect: (AccountRole) -> Unit,
) {
    val selected = role == selectedRole
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) colors.secondaryContainer else colors.surfaceContainerLowest,
        contentColor = colors.onSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.secondary else colors.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 100.dp)
                .selectable(selected, role = Role.RadioButton, onClick = { onSelect(role) }).padding(spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).background(
                    if (selected) colors.surfaceContainerLowest else colors.background, MaterialTheme.shapes.small,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, Modifier.size(24.dp), tint = colors.onSurface)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                Text(role.label, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            // Radio indicator: unselected ring uses the 3:1 outline token, not #CFD4D8 (audit 3.6).
            Box(
                Modifier.size(24.dp).background(if (selected) colors.secondary else colors.surfaceContainerLowest, CircleShape)
                    .border(1.5.dp, if (selected) colors.secondary else colors.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(16.dp), tint = colors.onSecondary)
            }
        }
    }
}

@Composable
internal fun SignIn(
    role: AccountRole,
    email: String,
    password: String,
    error: DemoSignInResult?,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSignIn: () -> Unit,
    onChangeRole: () -> Unit,
    onPasswordHelp: () -> Unit,
    onContinueAsStudent: () -> Unit,
    onReturnToSignIn: () -> Unit,
) {
    val student = role == AccountRole.Student
    val passwordFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    EntryCanvas(expanded = true, committee = !student) {
        // 48dp text button with a real icon, replacing the 17px "‹ Change role" text (audit 2.7, 3.10).
        TextButton(onClick = onChangeRole, contentPadding = PaddingValues(horizontal = ScpdTheme.spacing.space2)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(ScpdTheme.spacing.space1))
            Text("Change role", style = type.labelMedium)
        }
        Text("Sign in to s-CPD", Modifier.semantics { heading() }, style = type.headlineSmall)
        // Role badge as a solid-container chip. Teal or gold text on white was 2.52:1 / 1.97:1 (audit 3.3, 3.4).
        if (student) {
            TagChip("Student account", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.School)
        } else {
            TagChip("Committee account", colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.Groups)
        }
        Text("Use your university email and password.", style = type.bodySmall)
        AuthField(
            label = "University email", value = email, placeholder = "name@nottingham.edu.my",
            onValueChange = onEmail, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
            onIme = { passwordFocus.requestFocus() },
        )
        AuthField(
            label = "Password", value = password, placeholder = "Enter your password",
            onValueChange = onPassword, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
            onIme = {
                focus.clearFocus()
                onSignIn()
            },
            focusRequester = passwordFocus, secret = true,
        )
        TextButton(onClick = onPasswordHelp, contentPadding = PaddingValues(horizontal = ScpdTheme.spacing.space2)) {
            Text("Forgot password?", style = type.titleSmall)
        }
        if (error != null) {
            SignInNotice(student = student, roleNotAllowed = error == DemoSignInResult.RoleNotAllowed)
        }
        if (error == DemoSignInResult.RoleNotAllowed && !student) {
            PrimaryAction("Continue as Student", onClick = onContinueAsStudent)
            SecondaryAction("Return to Sign in", onClick = onReturnToSignIn)
        } else {
            PrimaryAction("Sign in") {
                focus.clearFocus()
                onSignIn()
            }
        }
    }
}

/** Sign-in problems are an inline banner above the form actions, never a dialog. One title, one short line. */
@Composable
private fun SignInNotice(student: Boolean, roleNotAllowed: Boolean) {
    when {
        roleNotAllowed && student -> StatusBanner(
            "This is a committee account", body = "Change your role to continue.", tone = BannerTone.Warning,
        )
        roleNotAllowed -> StatusBanner(
            "No committee access", body = "This account can only sign in as a student.", tone = BannerTone.Warning,
        )
        else -> StatusBanner(
            "Email or password not recognised", body = "Check your details and try again.", tone = BannerTone.Warning,
        )
    }
}

@Composable
private fun AuthField(
    label: String, value: String, placeholder: String, onValueChange: (String) -> Unit,
    keyboardType: KeyboardType, imeAction: ImeAction, onIme: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() }, secret: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val shape = MaterialTheme.shapes.medium
    BasicTextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }.semantics { contentDescription = label },
        singleLine = true,
        textStyle = type.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onNext = { onIme() }, onDone = { onIme() }),
        decorationBox = { inner ->
            Column(
                Modifier.fillMaxWidth().heightIn(min = 76.dp)
                    .background(colors.surfaceContainerLowest, shape)
                    // Unfocused border uses the 4.21:1 outline token; #CFD4D8 was 1.49:1 (audit 3.6).
                    .border(if (focused) 2.dp else 1.dp, if (focused) colors.primary else colors.outline, shape)
                    .padding(horizontal = spacing.space4, vertical = spacing.space3),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(label, style = type.labelSmall, color = colors.onSurfaceVariant)
                Box {
                    if (value.isEmpty()) Text(placeholder, style = type.bodyLarge, color = ScpdTheme.colors.textTertiary)
                    inner()
                }
            }
        },
    )
}

@Composable
private fun HeroCopy(modifier: Modifier = Modifier) {
    val extra = ScpdTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space3)) {
        Text(
            "Your campus.\nYour community.", Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall, color = extra.onHero,
        )
        Text(
            "Find events, clubs and people beyond the classroom.",
            style = MaterialTheme.typography.bodyLarge, color = extra.onHero,
        )
    }
}

@Composable
private fun EntryCanvas(expanded: Boolean, rolePicker: Boolean = false, committee: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Landscape phones and landscape tablets get two panes (audit 4.7): campus story on the left,
        // the form card on the right with its own scroll. Portrait keeps the stacked layout.
        if (maxWidth >= 600.dp && maxWidth > maxHeight) {
            TwoPaneEntry(expanded, committee, content)
        } else {
            StackedEntry(expanded, rolePicker, committee, content)
        }
    }
}

@Composable
private fun TwoPaneEntry(expanded: Boolean, committee: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val spacing = ScpdTheme.spacing
    Box(Modifier.fillMaxSize()) {
        CampusBackground(Modifier.matchParentSize(), compact = !expanded, committee = committee)
        Row(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(spacing.space8),
        ) {
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = spacing.space5),
                verticalArrangement = Arrangement.spacedBy(spacing.space6),
            ) {
                BrandIdentities()
                if (expanded) HeroCopy()
            }
            Surface(
                color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f).widthIn(max = 480.dp).fillMaxHeight().padding(vertical = spacing.space3),
            ) {
                Column(
                    Modifier.imePadding().verticalScroll(rememberScrollState()).padding(spacing.space5),
                    verticalArrangement = Arrangement.spacedBy(spacing.space4),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun StackedEntry(expanded: Boolean, rolePicker: Boolean, committee: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val spacing = ScpdTheme.spacing
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val brandPadding = ((if (expanded) 63.dp else 49.dp) - statusInset).coerceAtLeast(spacing.space2)
    // Below the photo the canvas is the app background, not bare #CFD4D8 (audit 3.5).
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CampusBackground(Modifier.fillMaxWidth().height(if (expanded) 387.dp else 356.dp), compact = !expanded, committee = committee)
        Column(
            // Status-bar inset sits outside the scroll, so content never scrolls up under the clock.
            Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState())
                .padding(start = spacing.gutter, end = spacing.gutter, top = brandPadding, bottom = spacing.space6)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                BrandIdentities()
                if (expanded) {
                    Spacer(Modifier.height(spacing.space6))
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 178.dp).zIndex(1f),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        HeroCopy(Modifier.weight(1f).padding(top = spacing.space8, bottom = spacing.space4))
                        Image(
                            painterResource(R.drawable.gilbert), null,
                            Modifier.offset(y = if (rolePicker) 19.dp else 32.dp).size(88.dp),
                        )
                    }
                    Spacer(Modifier.height(spacing.space6))
                } else {
                    Spacer(Modifier.height(spacing.space3))
                }
                Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(spacing.space5), verticalArrangement = Arrangement.spacedBy(spacing.space4), content = content)
                }
            }
        }
    }
}

@Composable
internal fun PasswordHelp(onBack: () -> Unit) {
    val spacing = ScpdTheme.spacing
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        CompactHeader("Password recovery", onBack)
        ReadableWidth {
            Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space5)) {
                Text("Recover your university account", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Use the university account recovery service or contact the IT Service Desk.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                SecondaryAction("Back to sign in", onClick = onBack)
            }
        }
    }
}

@Composable
internal fun CommitteeLanding(onSignOut: () -> Unit) {
    EntryCanvas(expanded = false) {
        Text("Committee Dashboard", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
        SecondaryAction("Sign out", onClick = onSignOut)
    }
}
