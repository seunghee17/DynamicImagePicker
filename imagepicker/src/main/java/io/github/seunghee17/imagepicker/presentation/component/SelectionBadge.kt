package io.github.seunghee17.imagepicker.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 선택된 이미지의 순서 번호를 표시하는 원형 배지.
 */
@Composable
internal fun SelectionBadge(
    modifier: Modifier = Modifier,
    order: Int?,
    onTap: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                if (order != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary,
                CircleShape
            )
            .clickable{
                onTap()
            },
        contentAlignment = Alignment.Center
    ) {
        if (order != null) { // order != null 이면 select가 된 상태라는 의미
            Text(
                text = order.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Preview("SelectionBadge UnSelect State",showBackground = true)
@Composable
fun UnSelectionBadgePreview() {
    SelectionBadge(order = null, onTap = {})
}

@Preview("SelectionBadge Select State",showBackground = true)
@Composable
fun SelectionBadgePreview() {
    SelectionBadge(order = 1, onTap = {})
}