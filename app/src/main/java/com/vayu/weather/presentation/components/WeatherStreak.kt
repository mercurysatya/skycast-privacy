package com.vayu.weather.presentation.components

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.ui.theme.*

/**
 * Weather Streak — gamification that keeps users coming back daily.
 * Tracks consecutive days of checking weather and awards badges.
 */
@Composable
fun WeatherStreakCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    val prefs = remember { context.getSharedPreferences("weather_streak", Context.MODE_PRIVATE) }
    val streakData = remember { loadStreakData(prefs) }
    val view = LocalView.current

    // Check and update streak on first composition
    LaunchedEffect(Unit) {
        checkAndUpdateStreak(prefs)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Weather streak: ${streakData.currentStreak} days"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = WarmOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weather Explorer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Streak counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Big streak number
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${streakData.currentStreak}",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmOrange
                    )
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Stats
                Column(modifier = Modifier.weight(1f)) {
                    StreakStat("Best Streak", "${streakData.bestStreak} days", Icons.Rounded.EmojiEvents)
                    Spacer(modifier = Modifier.height(6.dp))
                    StreakStat("Total Checks", "${streakData.totalChecks}", Icons.Rounded.CheckCircle)
                    Spacer(modifier = Modifier.height(6.dp))
                    StreakStat("This Week", "${streakData.thisWeekChecks}/7", Icons.Rounded.CalendarToday)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Weekly calendar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val today = java.time.LocalDate.now()
                for (i in 6 downTo 0) {
                    val day = today.minusDays(i.toLong())
                    val dayName = day.format(java.time.format.DateTimeFormatter.ofPattern("EEE"))
                    val isChecked = streakData.checkedDays.contains(day.toString())
                    val isToday = i == 0

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isChecked -> WarmOrange
                                        isToday -> Color.White.copy(alpha = 0.15f)
                                        else -> Color.White.copy(alpha = 0.06f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Checked",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                            } else if (isToday) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            } else {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges
            val badges = getEarnedBadges(streakData)
            if (badges.isNotEmpty()) {
                Text(
                    text = "Badges Earned",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    badges.take(5).forEach { badge ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(badge.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = badge.emoji, fontSize = 18.sp)
                            }
                            Text(
                                text = badge.name,
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakStat(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

private data class StreakBadge(val emoji: String, val name: String, val color: Color)

private fun getEarnedBadges(data: StreakData): List<StreakBadge> {
    val badges = mutableListOf<StreakBadge>()
    if (data.currentStreak >= 1) badges += StreakBadge("🌅", "First Light", FreshGreen)
    if (data.currentStreak >= 3) badges += StreakBadge("🔥", "On Fire", WarmOrange)
    if (data.currentStreak >= 7) badges += StreakBadge("⭐", "Weekly Star", AmberGlow)
    if (data.currentStreak >= 14) badges += StreakBadge("🏆", "Champion", AmberGlow)
    if (data.currentStreak >= 30) badges += StreakBadge("👑", "Weather King", SunsetRed)
    if (data.totalChecks >= 10) badges += StreakBadge("🎯", "Sharp Eye", SkyBlue)
    if (data.totalChecks >= 50) badges += StreakBadge("🌟", "Dedicated", FreshGreen)
    if (data.totalChecks >= 100) badges += StreakBadge("💎", "Legend", Color(0xFFCE93D8))
    return badges
}

private data class StreakData(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalChecks: Int = 0,
    val thisWeekChecks: Int = 0,
    val checkedDays: Set<String> = emptySet()
)

private fun loadStreakData(prefs: android.content.SharedPreferences): StreakData {
    val currentStreak = prefs.getInt("current_streak", 0)
    val bestStreak = prefs.getInt("best_streak", 0)
    val totalChecks = prefs.getInt("total_checks", 0)
    val thisWeekChecks = prefs.getInt("this_week_checks", 0)
    val checkedDaysJson = prefs.getStringSet("checked_days", emptySet()) ?: emptySet()
    return StreakData(currentStreak, bestStreak, totalChecks, thisWeekChecks, checkedDaysJson)
}

private fun checkAndUpdateStreak(prefs: android.content.SharedPreferences) {
    val today = java.time.LocalDate.now().toString()
    val checkedDays = (prefs.getStringSet("checked_days", emptySet()) ?: emptySet()).toMutableSet()

    if (!checkedDays.contains(today)) {
        // First check today
        checkedDays.add(today)

        val lastCheckDate = prefs.getString("last_check_date", null)
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()

        val currentStreak = if (lastCheckDate == yesterday) {
            prefs.getInt("current_streak", 0) + 1
        } else if (lastCheckDate == today) {
            prefs.getInt("current_streak", 0)
        } else {
            1 // Streak broken
        }

        val bestStreak = maxOf(currentStreak, prefs.getInt("best_streak", 0))
        val totalChecks = prefs.getInt("total_checks", 0) + 1

        // Count this week's checks
        val weekStart = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY)
        val thisWeekChecks = checkedDays.count { day ->
            try {
                val date = java.time.LocalDate.parse(day)
                !date.isBefore(weekStart)
            } catch (e: Exception) { false }
        }

        // Keep only last 30 days
        val cutoff = java.time.LocalDate.now().minusDays(30).toString()
        val trimmedDays = checkedDays.filter { it >= cutoff }.toSet()

        prefs.edit()
            .putInt("current_streak", currentStreak)
            .putInt("best_streak", bestStreak)
            .putInt("total_checks", totalChecks)
            .putInt("this_week_checks", thisWeekChecks)
            .putString("last_check_date", today)
            .putStringSet("checked_days", trimmedDays)
            .apply()
    }
}
