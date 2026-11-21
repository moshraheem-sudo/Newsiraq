package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.AppUpdateStage
import com.example.data.GitHubReleaseInfo
import com.example.data.GitHubUpdateManager
import java.io.File
import kotlin.math.roundToInt

@Composable
fun AppUpdateBanner(
  updateState: AppUpdateStage,
  strings: AppStrings,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bannerText: String? =
    when (updateState) {
      is AppUpdateStage.UpdateAvailable -> strings.updateAvailableBanner(updateState.release.versionName)
      is AppUpdateStage.Downloading -> {
        val dl = GitHubUpdateManager.formatMegabytes(updateState.downloadedBytes)
        val total =
          if (updateState.totalBytes > 0L) {
            " / ${GitHubUpdateManager.formatMegabytes(updateState.totalBytes)}"
          } else {
            ""
          }
        "${strings.downloadingUpdate} ($dl$total)"
      }
      is AppUpdateStage.ReadyToInstall -> strings.updateReadyBanner(updateState.release.versionName)
      else -> null
    }

  if (bannerText == null) return

  val isReady = updateState is AppUpdateStage.ReadyToInstall
  Surface(
    color =
      if (isReady) MaterialTheme.colorScheme.tertiaryContainer
      else MaterialTheme.colorScheme.primaryContainer,
    shape = RoundedCornerShape(14.dp),
    border =
      BorderStroke(
        1.dp,
        if (isReady) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
      ),
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 6.dp)
        .clickable { onClick() }
        .testTag("app_update_banner"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (isReady) Icons.Default.InstallMobile else Icons.Default.SystemUpdate,
        contentDescription = null,
        tint =
          if (isReady) MaterialTheme.colorScheme.onTertiaryContainer
          else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(20.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = bannerText,
          style = MaterialTheme.typography.labelLarge,
          color =
            if (isReady) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (updateState is AppUpdateStage.Downloading) {
          val fraction = updateState.progressFraction
          if (fraction >= 0f) {
            LinearProgressIndicator(
              progress = { fraction },
              modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
            )
          } else {
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
            )
          }
        }
      }
    }
  }
}

@Composable
fun AppUpdateDialog(
  updateState: AppUpdateStage,
  strings: AppStrings,
  onStartDownload: (GitHubReleaseInfo) -> Unit,
  onCancelDownload: () -> Unit,
  onInstallApk: (File) -> Unit,
  onOpenReleasePage: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val release: GitHubReleaseInfo? =
    when (updateState) {
      is AppUpdateStage.UpdateAvailable -> updateState.release
      is AppUpdateStage.Downloading -> updateState.release
      is AppUpdateStage.ReadyToInstall -> updateState.release
      is AppUpdateStage.Error -> updateState.release
      else -> null
    }
  if (release == null) return

  val installedVersion =
    when (updateState) {
      is AppUpdateStage.UpdateAvailable -> updateState.installedVersion
      is AppUpdateStage.Downloading -> updateState.installedVersion
      is AppUpdateStage.ReadyToInstall -> updateState.installedVersion
      is AppUpdateStage.Error -> updateState.installedVersion
      else -> ""
    }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector =
                if (updateState is AppUpdateStage.ReadyToInstall) Icons.Default.InstallMobile
                else Icons.Default.SystemUpdate,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(24.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = strings.updateAvailableTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = release.title,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Installed vs GitHub version comparison card
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = strings.currentVersionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "v$installedVersion",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = strings.latestVersionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "v${release.versionName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            val sizeBytes =
              if (updateState is AppUpdateStage.ReadyToInstall) updateState.apkSizeBytes
              else release.apkSizeBytes
            if (sizeBytes > 0L) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = strings.updateSizeLabel,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = GitHubUpdateManager.formatMegabytes(sizeBytes),
                  style = MaterialTheme.typography.labelMedium,
                )
              }
            }
          }
        }

        if (release.releaseNotes.isNotBlank()) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = strings.releaseNotesTitle,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
            ) {
              Text(
                text = release.releaseNotes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState()),
              )
            }
          }
        }

        if (updateState is AppUpdateStage.Downloading) {
          DownloadProgressBlock(
            downloading = updateState,
            strings = strings,
            onCancel = onCancelDownload,
            cancelTag = "dialog_cancel_update_btn",
          )
        } else if (updateState is AppUpdateStage.ReadyToInstall) {
          Button(
            onClick = { onInstallApk(updateState.apkFile) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_install_update_btn"),
          ) {
            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.installUpdateButton, style = MaterialTheme.typography.labelLarge)
          }
        } else {
          if (updateState is AppUpdateStage.Error && updateState.message.isNotBlank()) {
            Text(
              text = updateState.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
          if (!release.apkDownloadUrl.isNullOrBlank()) {
            Button(
              onClick = { onStartDownload(release) },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_download_update_btn"),
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(strings.updateNowButton, style = MaterialTheme.typography.labelLarge)
            }
          } else {
            Text(
              text = strings.noApkAssetMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = { onOpenReleasePage(release.releasePageUrl) },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(strings.openReleasePage, style = MaterialTheme.typography.labelLarge)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("dialog_later_update_btn"),
          ) {
            Text(strings.laterButton)
          }
        }
      }
    }
  }
}

@Composable
fun GitHubUpdateSettingsCard(
  updateState: AppUpdateStage,
  strings: AppStrings,
  onCheckForUpdates: () -> Unit,
  onStartDownload: (GitHubReleaseInfo) -> Unit,
  onCancelDownload: () -> Unit,
  onInstallApk: (File) -> Unit,
  onOpenRepo: () -> Unit,
  onOpenReleasePage: (String) -> Unit,
) {
  val installedVersion =
    when (updateState) {
      is AppUpdateStage.Idle -> updateState.installedVersion
      is AppUpdateStage.Checking -> updateState.installedVersion
      is AppUpdateStage.UpToDate -> updateState.installedVersion
      is AppUpdateStage.UpdateAvailable -> updateState.installedVersion
      is AppUpdateStage.Downloading -> updateState.installedVersion
      is AppUpdateStage.ReadyToInstall -> updateState.installedVersion
      is AppUpdateStage.Error -> updateState.installedVersion
    }

  val latestRelease =
    when (updateState) {
      is AppUpdateStage.Idle -> updateState.latestRelease
      is AppUpdateStage.UpToDate -> updateState.latestRelease
      is AppUpdateStage.UpdateAvailable -> updateState.release
      is AppUpdateStage.Downloading -> updateState.release
      is AppUpdateStage.ReadyToInstall -> updateState.release
      is AppUpdateStage.Error -> updateState.release
      is AppUpdateStage.Checking -> null
    }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth().testTag("github_update_card"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Default.SystemUpdate,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(19.dp),
        )
        Text(
          text = strings.appUpdatesSection,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = strings.currentVersionLabel,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = "v$installedVersion", style = MaterialTheme.typography.labelLarge)
      }

      if (latestRelease != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = strings.latestVersionLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "v${latestRelease.versionName}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      when (updateState) {
        is AppUpdateStage.UpToDate -> {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = strings.upToDateMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        is AppUpdateStage.Downloading -> {
          DownloadProgressBlock(
            downloading = updateState,
            strings = strings,
            onCancel = onCancelDownload,
            cancelTag = "settings_cancel_update_btn",
          )
        }
        is AppUpdateStage.ReadyToInstall -> {
          Button(
            onClick = { onInstallApk(updateState.apkFile) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_install_update_btn"),
          ) {
            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.installUpdateButton, style = MaterialTheme.typography.labelLarge)
          }
        }
        is AppUpdateStage.UpdateAvailable -> {
          if (!updateState.release.apkDownloadUrl.isNullOrBlank()) {
            Button(
              onClick = { onStartDownload(updateState.release) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_download_update_btn"),
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              val sizeSuffix =
                if (updateState.release.apkSizeBytes > 0L) {
                  " (${GitHubUpdateManager.formatMegabytes(updateState.release.apkSizeBytes)})"
                } else {
                  ""
                }
              Text("${strings.updateNowButton}$sizeSuffix", style = MaterialTheme.typography.labelLarge)
            }
          } else {
            OutlinedButton(
              onClick = { onOpenReleasePage(updateState.release.releasePageUrl) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(strings.openReleasePage, style = MaterialTheme.typography.labelLarge)
            }
          }
        }
        is AppUpdateStage.Error -> {
          Text(
            text = strings.updateCheckFailedMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        else -> Unit
      }

      if (updateState !is AppUpdateStage.Downloading && updateState !is AppUpdateStage.ReadyToInstall) {
        FilledTonalButton(
          onClick = onCheckForUpdates,
          enabled = updateState !is AppUpdateStage.Checking,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("check_updates_button"),
        ) {
          if (updateState is AppUpdateStage.Checking) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.checkingUpdates, style = MaterialTheme.typography.labelLarge)
          } else {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.checkForUpdatesButton, style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      OutlinedButton(
        onClick = onOpenRepo,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("open_github_repo_btn"),
      ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(strings.openGitHubRepo, style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
private fun DownloadProgressBlock(
  downloading: AppUpdateStage.Downloading,
  strings: AppStrings,
  onCancel: () -> Unit,
  cancelTag: String,
) {
  val fraction = downloading.progressFraction
  val downloadedText = GitHubUpdateManager.formatMegabytes(downloading.downloadedBytes)
  val totalText =
    if (downloading.totalBytes > 0L) {
      " / ${GitHubUpdateManager.formatMegabytes(downloading.totalBytes)}"
    } else {
      ""
    }
  val percentText =
    if (fraction >= 0f) " (${(fraction * 100f).roundToInt()}%)" else ""

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = strings.downloadingUpdate,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (fraction >= 0f) {
      LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
      )
    } else {
      LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
      )
    }
    Text(
      text = "$downloadedText$totalText$percentText",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.testTag("download_mb_progress_text"),
    )
    OutlinedButton(
      onClick = onCancel,
      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().height(46.dp).testTag(cancelTag),
    ) {
      Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(strings.stopAndCancelButton, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
fun InstallPermissionRationaleDialog(
  strings: AppStrings,
  onGrantClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Default.InstallMobile, contentDescription = null) },
    title = { Text(strings.installPermissionTitle) },
    text = { Text(strings.installPermissionMessage) },
    confirmButton = {
      Button(
        onClick = onGrantClick,
        modifier = Modifier.testTag("grant_install_permission_btn"),
      ) {
        Text(strings.grantPermissionButton)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.laterButton)
      }
    },
  )
}
