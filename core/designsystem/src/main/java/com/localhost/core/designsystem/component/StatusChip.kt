package com.localhost.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localhost.core.designsystem.theme.StatusGray
import com.localhost.core.designsystem.theme.StatusGrayBg
import com.localhost.core.designsystem.theme.StatusGreen
import com.localhost.core.designsystem.theme.StatusGreenBg
import com.localhost.core.designsystem.theme.StatusRed
import com.localhost.core.designsystem.theme.StatusRedBg
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.StatusYellowBg
import com.localhost.core.model.ProjectStatus

@Composable
fun StatusChip(
    status: ProjectStatus,
    modifier: Modifier = Modifier
) {
    val (targetBgColor, targetTextColor, targetDotColor, text) = when (status) {
        ProjectStatus.RUNNING -> Quadruple(StatusGreenBg, StatusGreen, StatusGreen, "RUNNING")
        ProjectStatus.STARTING -> Quadruple(StatusYellowBg, StatusYellow, StatusYellow, "STARTING")
        ProjectStatus.RESTARTING -> Quadruple(StatusYellowBg, StatusYellow, StatusYellow, "RESTARTING")
        ProjectStatus.STOPPED -> Quadruple(StatusGrayBg, StatusGray, StatusGray, "STOPPED")
        ProjectStatus.CRASHED -> Quadruple(StatusRedBg, StatusRed, StatusRed, "CRASHED")
        ProjectStatus.STOPPING -> Quadruple(StatusYellowBg, StatusYellow, StatusYellow, "STOPPING")
    }

    val bgColor by animateColorAsState(targetBgColor, label = "chipBg")
    val textColor by animateColorAsState(targetTextColor, label = "chipText")
    val dotColor by animateColorAsState(targetDotColor, label = "chipDot")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
