package com.example.tesisv3

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class BottomNavDestination {
    DASHBOARD,
    GROUPS,
    CALENDAR,
    CARE
}

@Composable
fun AppBottomNav(
    current: BottomNavDestination,
    modifier: Modifier = Modifier,
    indicatorColor: Color,
    selectedColor: Color,
    unselectedColor: Color
) {
    val context = LocalContext.current

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 4.dp,
        modifier = modifier
    ) {
        val items = listOf(
            BottomNavDestination.DASHBOARD to Icons.Filled.Home,
            BottomNavDestination.GROUPS to Icons.Filled.Groups,
            BottomNavDestination.CALENDAR to Icons.Filled.DateRange,
            BottomNavDestination.CARE to Icons.Filled.LocalHospital
        )

        items.forEach { (dest, icon) ->
            val isSelected = current == dest
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (isSelected) return@NavigationBarItem
                    when (dest) {
                        BottomNavDestination.DASHBOARD -> {
                            context.startActivity(Intent(context, DashboardActivity::class.java))
                        }
                        BottomNavDestination.GROUPS -> {
                            context.startActivity(Intent(context, GroupsActivity::class.java))
                        }
                        BottomNavDestination.CALENDAR -> {
                            context.startActivity(Intent(context, CalendarActivity::class.java))
                        }
                        BottomNavDestination.CARE -> {
                            context.startActivity(Intent(context, CareActivity::class.java))
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    unselectedIconColor = unselectedColor,
                    indicatorColor = indicatorColor
                )
            )
        }
    }
}
