package com.example.aquallera.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.LightYellow
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.theme.MidnightBlueActive

data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavTabs = listOf(
    BottomNavTab(route = "map", label = "Map", icon = Icons.Default.Map),
    BottomNavTab(route = "orders", label = "Orders", icon = Icons.Default.Receipt),
    BottomNavTab(route = "profile", label = "Profile", icon = Icons.Default.Person),
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MidnightBlue,
        contentColor = AppBg,
    ) {
        bottomNavTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(text = tab.label, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LightYellow,
                    selectedTextColor = LightYellow,
                    unselectedIconColor = AppBg.copy(alpha = 0.7f),
                    unselectedTextColor = AppBg.copy(alpha = 0.7f),
                    indicatorColor = MidnightBlueActive,
                ),
            )
        }
    }
}
