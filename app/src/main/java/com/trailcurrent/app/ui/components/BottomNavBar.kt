package com.trailcurrent.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.trailcurrent.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // Determine how many items to show based on screen width
    // Each nav item needs roughly 80dp, "More" button needs 80dp
    val maxVisibleItems = when {
        screenWidthDp >= 600 -> 7  // Tablet - show all
        screenWidthDp >= 480 -> 5  // Large phone landscape
        screenWidthDp >= 360 -> 4  // Normal phone
        else -> 3                   // Small phone
    }

    val allItems = Screen.navBarItems
    val needsOverflow = allItems.size > maxVisibleItems

    // If we need overflow, reserve one slot for "More" button
    val visibleCount = if (needsOverflow) maxVisibleItems - 1 else allItems.size
    val visibleItems = allItems.take(visibleCount)
    val overflowItems = if (needsOverflow) allItems.drop(visibleCount) else emptyList()

    // Check if current route is in overflow
    val isCurrentInOverflow = overflowItems.any { it.route == currentRoute }

    var showOverflowSheet by remember { mutableStateOf(false) }

    NavigationBar(modifier = modifier) {
        visibleItems.forEach { screen ->
            NavigationBarItem(
                icon = {
                    screen.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = screen.title
                        )
                    }
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen) },
                alwaysShowLabel = true
            )
        }

        // "More" button for overflow items
        if (needsOverflow) {
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More"
                    )
                },
                label = { Text("More") },
                selected = isCurrentInOverflow,
                onClick = { showOverflowSheet = true },
                alwaysShowLabel = true
            )
        }
    }

    // Bottom sheet for overflow items
    if (showOverflowSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOverflowSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "More Options",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                overflowItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = {
                            screen.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title
                                )
                            }
                        },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            showOverflowSheet = false
                            onNavigate(screen)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}
