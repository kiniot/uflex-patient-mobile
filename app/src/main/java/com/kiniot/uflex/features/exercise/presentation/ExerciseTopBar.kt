package com.kiniot.uflex.features.exercise.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kiniot.uflex.R

@Composable
fun ExerciseTopBar(onBackClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(72.dp)
                    .padding(horizontal = 16.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_uflex_combination_mark_original),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .sizeIn(maxWidth = 92.dp, maxHeight = 30.dp),
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.exercise_back),
                        )
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f),
            )
        }
    }
}
