package com.overdrive.app.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            // Main Content Area based on Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (state.currentTab) {
                    AssistantTab.CHAT -> ChatTabContent(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )

                    AssistantTab.PROVIDER -> ProviderTabContent(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )

                    AssistantTab.PRIVACY -> PrivacyTabContent(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Navigation Tabs (100% Web Parity: Assistant, Provider, Privacy)
            AssistantBottomBar(
                currentTab = state.currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    }
}

/**
 * Bottom navigation bar with 3 tabs matching the web interface.
 */
@Composable
private fun AssistantBottomBar(
    currentTab: AssistantTab,
    onTabSelected: (AssistantTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AssistantBottomBarItem(
                title = stringResource(R.string.assistant_tab_assistant),
                icon = Icons.Outlined.ChatBubbleOutline,
                isSelected = currentTab == AssistantTab.CHAT,
                onClick = { onTabSelected(AssistantTab.CHAT) },
                modifier = Modifier.weight(1f)
            )

            AssistantBottomBarItem(
                title = stringResource(R.string.assistant_tab_provider_short),
                icon = Icons.Default.CloudQueue,
                isSelected = currentTab == AssistantTab.PROVIDER,
                onClick = { onTabSelected(AssistantTab.PROVIDER) },
                modifier = Modifier.weight(1f)
            )

            AssistantBottomBarItem(
                title = stringResource(R.string.assistant_tab_privacy_short),
                icon = Icons.Default.Security,
                isSelected = currentTab == AssistantTab.PRIVACY,
                onClick = { onTabSelected(AssistantTab.PRIVACY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AssistantBottomBarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeBg = MaterialTheme.colorScheme.primaryContainer
    val activeTint = MaterialTheme.colorScheme.primary
    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeTint else inactiveTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeTint else inactiveTint
            )
        }
    }
}

/**
 * Reusable Accordion Card with expandable header and content.
 */
@Composable
private fun AssistantAccordionCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    content()
                }
            }
        }
    }
}

/**
 * Compact styled OutlinedTextField for automotive density.
 */
@Composable
private fun CompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it, fontSize = 12.sp) } },
        placeholder = placeholder?.let { { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(8.dp),
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
    )
}

/**
 * Compact Dropdown selector matching web input style.
 */
@Composable
private fun <T> CompactDropdownSelector(
    label: String,
    selectedValue: T,
    options: List<Pair<T, String>>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selectedValue }?.second ?: selectedValue.toString()

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (key, title) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (key == selectedValue) FontWeight.Bold else FontWeight.Normal,
                                color = if (key == selectedValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onValueChange(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// =========================================================================
// TAB 1: ASSISTANT (CHAT, ACCORDIONS, INSIGHTS)
// =========================================================================

@Composable
private fun ChatTabContent(
    state: AssistantUiState,
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Status Banner: GenAI is off (Web parity banner) or Model active card
        if (!state.isEnabled || state.status == GenAiStatus.DISABLED) {
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Computer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.assistant_genai_off_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.assistant_genai_off_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OverdriveButton(
                        text = stringResource(R.string.assistant_btn_configure),
                        variant = OverdriveButtonVariant.OUTLINED,
                        onClick = { viewModel.selectTab(AssistantTab.PROVIDER) }
                    )
                }
            }
        } else {
            // GenAI Active / Hero banner with tightened padding
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = state.provider.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = state.model,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (state.status == GenAiStatus.READY) {
                                    stringResource(R.string.assistant_hero_ready_desc)
                                } else {
                                    stringResource(R.string.assistant_hero_setup_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.assistant_clear_history),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Accordion: Routine suggestions (Web screenshot 1)
        AssistantAccordionCard(
            title = stringResource(R.string.assistant_routine_suggestions_title),
            icon = Icons.Default.WbSunny,
            isExpanded = state.routineSuggestionsExpanded,
            onToggle = { viewModel.toggleRoutineSuggestions() }
        ) {
            Text(
                text = stringResource(R.string.assistant_routine_suggestions_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 3. Accordion: Incident evidence packs (Web screenshot 1)
        AssistantAccordionCard(
            title = stringResource(R.string.assistant_evidence_packs_title),
            icon = Icons.Default.Description,
            isExpanded = state.evidencePacksExpanded,
            onToggle = { viewModel.toggleEvidencePacks() }
        ) {
            Text(
                text = stringResource(R.string.assistant_evidence_packs_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 4. Quick Action Chips Row (100% Web Parity: Analytics & Vehicle queries)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val webChips = listOf(
                stringResource(R.string.assistant_chip_search_history),
                stringResource(R.string.assistant_chip_current_vehicle),
                stringResource(R.string.assistant_chip_explain_trip),
                stringResource(R.string.assistant_chip_why_battery),
                stringResource(R.string.assistant_chip_summarize_events),
                stringResource(R.string.assistant_chip_roadsense_hazards),
                stringResource(R.string.assistant_chip_charging_review),
            )
            webChips.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { viewModel.sendMessage(prompt) }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 5. Chat Box (Empty welcome state or conversation history)
        OverdriveCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 340.dp),
            contentPadding = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Messages area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (state.messages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.assistant_ask_about_vehicle),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.assistant_ask_about_vehicle_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(state.messages, key = { it.id }) { msg ->
                                ChatMessageBubble(message = msg)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Input Bar (Mic button + Input field + Send button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Voice Mic Icon Button
                    val defaultVoicePrompt = stringResource(R.string.assistant_voice_prompt_default)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                viewModel.sendMessage(defaultVoicePrompt)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = stringResource(R.string.assistant_btn_voice),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Text Field
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.assistant_input_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage(null) })
                    )

                    // Send Button
                    Surface(
                        shape = CircleShape,
                        color = if (state.inputText.isNotBlank() && !state.isThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(enabled = state.inputText.isNotBlank() && !state.isThinking) {
                                viewModel.sendMessage(null)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.assistant_btn_send),
                                tint = if (state.inputText.isNotBlank() && !state.isThinking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Disclaimer under input
                Text(
                    text = stringResource(R.string.assistant_voice_disclaimer),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }

        // 6. Accordion: Generate an insight (Web screenshot 3)
        AssistantAccordionCard(
            title = stringResource(R.string.assistant_generate_insight_title),
            icon = Icons.Default.AutoAwesome,
            isExpanded = state.generateInsightExpanded,
            onToggle = { viewModel.toggleGenerateInsight() }
        ) {
            val insightModes = listOf(
                "overview" to stringResource(R.string.assistant_insight_mode_overview),
                "current_vehicle" to stringResource(R.string.assistant_insight_mode_current_vehicle),
                "latest_trip" to stringResource(R.string.assistant_insight_mode_latest_trip),
                "recent_events" to stringResource(R.string.assistant_insight_mode_recent_events),
                "roadsense" to stringResource(R.string.assistant_insight_mode_roadsense),
                "charging" to stringResource(R.string.assistant_insight_mode_charging)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactDropdownSelector(
                    label = stringResource(R.string.assistant_insight_type_label),
                    selectedValue = state.insightType,
                    options = insightModes,
                    onValueChange = { viewModel.updateInsightType(it) },
                    modifier = Modifier.weight(1f)
                )

                CompactOutlinedTextField(
                    value = state.optionalFocus,
                    onValueChange = { viewModel.updateOptionalFocus(it) },
                    label = stringResource(R.string.assistant_optional_focus_label),
                    placeholder = stringResource(R.string.assistant_optional_focus_placeholder),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.assistant_also_send_notification),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.assistant_also_send_notification_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = state.alsoSendNotification,
                    onCheckedChange = { viewModel.updateAlsoSendNotification(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OverdriveButton(
                text = if (state.isGeneratingInsight) stringResource(R.string.assistant_generating_insight) else stringResource(R.string.assistant_btn_generate_now),
                variant = OverdriveButtonVariant.PRIMARY,
                enabled = !state.isGeneratingInsight,
                onClick = {
                    viewModel.generateInsight(state.insightType, state.optionalFocus, state.alsoSendNotification)
                }
            )

            // Result display if insight was generated
            state.lastGeneratedInsight?.let { insight ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 7. Accordion: Dashboard insights (Web screenshot 4 & 5)
        AssistantAccordionCard(
            title = stringResource(R.string.assistant_dashboard_insights_title),
            icon = Icons.Default.Schedule,
            isExpanded = state.dashboardInsightsExpanded,
            onToggle = { viewModel.toggleDashboardInsights() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.assistant_show_latest_on_dashboards),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.assistant_show_latest_on_dashboards_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = state.showInsightOnDashboards,
                    onCheckedChange = { viewModel.updateShowInsightOnDashboards(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val frequencies = listOf(
                    "off" to stringResource(R.string.assistant_frequency_off),
                    "daily" to stringResource(R.string.assistant_frequency_daily),
                    "weekly" to stringResource(R.string.assistant_frequency_weekly)
                )
                CompactDropdownSelector(
                    label = stringResource(R.string.assistant_frequency_label),
                    selectedValue = state.insightFrequency,
                    options = frequencies,
                    onValueChange = { viewModel.updateInsightFrequency(it) },
                    modifier = Modifier.weight(1f)
                )

                CompactOutlinedTextField(
                    value = state.insightLocalTime,
                    onValueChange = { viewModel.updateInsightLocalTime(it) },
                    label = stringResource(R.string.assistant_local_time_label),
                    placeholder = stringResource(R.string.assistant_local_time_placeholder),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val summaryTypes = listOf(
                "overview" to stringResource(R.string.assistant_insight_mode_overview),
                "latest_trip" to stringResource(R.string.assistant_insight_mode_latest_trip),
                "recent_events" to stringResource(R.string.assistant_insight_mode_recent_events),
                "charging" to stringResource(R.string.assistant_insight_mode_charging)
            )
            CompactDropdownSelector(
                label = stringResource(R.string.assistant_scheduled_summary_type_label),
                selectedValue = state.scheduledSummaryType,
                options = summaryTypes,
                onValueChange = { viewModel.updateScheduledSummaryType(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.assistant_notify_when_ready),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.assistant_notify_when_ready_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = state.notifyWhenReady,
                    onCheckedChange = { viewModel.updateNotifyWhenReady(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.assistant_dashboard_insights_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverdriveButton(
                    text = stringResource(R.string.assistant_btn_save),
                    variant = OverdriveButtonVariant.PRIMARY,
                    onClick = {
                        viewModel.saveDashboardInsights(
                            state.showInsightOnDashboards,
                            state.insightFrequency,
                            state.insightLocalTime,
                            state.scheduledSummaryType,
                            state.notifyWhenReady
                        )
                    }
                )

                OverdriveButton(
                    text = stringResource(R.string.assistant_btn_open_automations),
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = { /* Open automations sheet */ }
                )
            }
        }
    }
}

// =========================================================================
// TAB 2: PROVIDER (BRING YOUR OWN PROVIDER, TEXTBOXES & BUTTONS)
// =========================================================================

@Composable
private fun ProviderTabContent(
    state: AssistantUiState,
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier,
) {
    var enabled by remember(state.isEnabled) { mutableStateOf(state.isEnabled) }
    var selectedProvider by remember(state.provider) { mutableStateOf(state.provider) }
    var modelName by remember(state.model) { mutableStateOf(state.model) }
    var apiKey by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var baseUrl by remember(state.baseUrl) { mutableStateOf(state.baseUrl) }
    var maxTokens by remember(state.maxOutputTokens) { mutableStateOf(state.maxOutputTokens.toString()) }
    var realtimeModel by remember(state.realtimeModel) { mutableStateOf(state.realtimeModel) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Feedback banners
        state.saveSuccessMessage?.let {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        state.errorMessage?.let {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        state.testResultMessage?.let { testMsg ->
            val isSuccess = state.testSuccess ?: true
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = testMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Accordion: Bring your own provider (Web screenshot 1 & 2)
        AssistantAccordionCard(
            title = stringResource(R.string.assistant_bring_your_own_provider),
            icon = Icons.Default.CloudQueue,
            isExpanded = state.bringYourOwnProviderExpanded,
            onToggle = { viewModel.toggleBringYourOwnProvider() }
        ) {
            // 1. Enable GenAI Master Kill Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.assistant_enable_genai_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.assistant_enable_genai_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Provider and Text Model (compact row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val providers = listOf(
                    "openai" to stringResource(R.string.assistant_provider_openai),
                    "gemini" to stringResource(R.string.assistant_provider_gemini),
                    "anthropic" to stringResource(R.string.assistant_provider_anthropic),
                    "openai_compatible" to stringResource(R.string.assistant_provider_custom)
                )
                CompactDropdownSelector(
                    label = stringResource(R.string.assistant_provider_label),
                    selectedValue = selectedProvider,
                    options = providers,
                    onValueChange = { key ->
                        selectedProvider = key
                        if (key == "gemini") {
                            modelName = "gemini-2.5-flash"
                            baseUrl = "https://generativelanguage.googleapis.com"
                        } else if (key == "openai") {
                            modelName = "gpt-5.6-sol"
                            baseUrl = "https://api.openai.com"
                        } else if (key == "anthropic") {
                            modelName = "claude-3-5-sonnet-20241022"
                            baseUrl = "https://api.anthropic.com"
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Column(modifier = Modifier.weight(1f)) {
                    CompactOutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = stringResource(R.string.assistant_model_label),
                        placeholder = "gpt-5.6-sol",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                text = stringResource(R.string.assistant_model_suggestions_hint),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Provider Base URL
            CompactOutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = stringResource(R.string.assistant_base_url_label),
                placeholder = "https://api.openai.com",
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.assistant_base_url_hint),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. API Key & Max Output Tokens (compact row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactOutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = stringResource(R.string.assistant_api_key_label),
                    placeholder = if (state.hasApiKey) stringResource(R.string.assistant_api_key_encrypted_placeholder) else stringResource(R.string.assistant_api_key_paste_placeholder),
                    modifier = Modifier.weight(1.4f),
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                CompactOutlinedTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it.filter { ch -> ch.isDigit() } },
                    label = stringResource(R.string.assistant_max_output_tokens_label),
                    placeholder = "1200",
                    modifier = Modifier.weight(0.9f)
                )
            }
            Text(
                text = stringResource(R.string.assistant_api_key_disclaimer),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Realtime voice model (optional)
            val realtimeVoiceOptions = listOf(
                "" to stringResource(R.string.assistant_realtime_voice_none),
                "gpt-4o-realtime-preview" to "gpt-4o-realtime-preview",
                "gemini-2.0-flash-exp" to "gemini-2.0-flash-exp"
            )
            CompactDropdownSelector(
                label = stringResource(R.string.assistant_realtime_voice_label),
                selectedValue = realtimeModel,
                options = realtimeVoiceOptions,
                onValueChange = { realtimeModel = it },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.assistant_realtime_voice_desc),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Action Buttons: Kaydet, Test provider, Disable/Clear key
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverdriveButton(
                    text = stringResource(R.string.assistant_btn_save),
                    variant = OverdriveButtonVariant.PRIMARY,
                    onClick = {
                        val tokens = maxTokens.toIntOrNull() ?: 1200
                        viewModel.saveProviderSettings(enabled, selectedProvider, modelName, apiKey, baseUrl, tokens, realtimeModel)
                    }
                )

                OverdriveButton(
                    text = if (state.isTestingProvider) stringResource(R.string.assistant_testing_provider) else stringResource(R.string.assistant_btn_test_provider),
                    variant = OverdriveButtonVariant.OUTLINED,
                    enabled = !state.isTestingProvider,
                    onClick = { viewModel.testProvider() }
                )

                if (state.hasApiKey) {
                    OverdriveButton(
                        text = stringResource(R.string.assistant_btn_clear_key),
                        variant = OverdriveButtonVariant.DANGER,
                        onClick = { viewModel.clearApiKey() }
                    )
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: PRIVACY & RUNTIME (ACCORDIONS, ROUTINES, DIAGNOSTICS & BUTTONS)
// =========================================================================

@Composable
private fun PrivacyTabContent(
    state: AssistantUiState,
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Feedback banner
        state.saveSuccessMessage?.let {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        state.errorMessage?.let {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Accordion: Privacy and runtime (Web screenshot 1 & 2)
        AssistantAccordionCard(
            title = stringResource(R.string.assistant_privacy_runtime_title),
            icon = Icons.Default.Security,
            isExpanded = state.privacyRuntimeExpanded,
            onToggle = { viewModel.togglePrivacyRuntime() }
        ) {
            // 6 Security Bullet Points
            val bullets = listOf(
                stringResource(R.string.assistant_privacy_bullet_1),
                stringResource(R.string.assistant_privacy_bullet_2),
                stringResource(R.string.assistant_privacy_bullet_3),
                stringResource(R.string.assistant_privacy_bullet_4),
                stringResource(R.string.assistant_privacy_bullet_5),
                stringResource(R.string.assistant_privacy_bullet_6)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bullets.forEach { bullet ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = bullet,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Learn my routines locally Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.assistant_learn_routines_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.assistant_learn_routines_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.routineLearningEnabled,
                    onCheckedChange = { viewModel.toggleRoutineLearning(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Runtime Diagnostic Status Cards (2x2 Grid, Web screenshot 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticBox(
                    label = stringResource(R.string.assistant_diag_parked_label),
                    value = stringResource(R.string.assistant_diag_parked_val),
                    modifier = Modifier.weight(1f)
                )
                DiagnosticBox(
                    label = stringResource(R.string.assistant_diag_transport_label),
                    value = stringResource(R.string.assistant_diag_transport_val),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticBox(
                    label = stringResource(R.string.assistant_diag_proxy_label),
                    value = stringResource(R.string.assistant_diag_proxy_val),
                    modifier = Modifier.weight(1f)
                )
                DiagnosticBox(
                    label = stringResource(R.string.assistant_diag_active_requests_label),
                    value = state.activeRequests.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Action Buttons (Web screenshot 2: Save routine setting, Reset learned patterns, Disable and clear API key)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverdriveButton(
                    text = stringResource(R.string.assistant_btn_save_routine),
                    variant = OverdriveButtonVariant.PRIMARY,
                    onClick = {
                        viewModel.toggleRoutineLearning(state.routineLearningEnabled)
                    }
                )

                OverdriveButton(
                    text = if (state.isResettingPatterns) "…" else stringResource(R.string.assistant_btn_reset_patterns),
                    variant = OverdriveButtonVariant.OUTLINED,
                    enabled = !state.isResettingPatterns,
                    onClick = { viewModel.resetLearnedPatterns() }
                )

                OverdriveButton(
                    text = stringResource(R.string.assistant_btn_clear_key),
                    variant = OverdriveButtonVariant.DANGER,
                    onClick = { viewModel.clearApiKey() }
                )
            }
        }
    }
}

@Composable
private fun DiagnosticBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: AssistantMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (message.isPending) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.assistant_thinking),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
