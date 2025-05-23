package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use case that Setups Camera Uploads Setting and backup
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property updateBackupStateUseCase [UpdateBackupStateUseCase]
 */
class SetupCameraUploadsSettingUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val updateBackupStateUseCase: UpdateBackupStateUseCase,
) {

    /**
     * Invocation function
     *
     * @param isEnabled [Boolean]
     */
    suspend operator fun invoke(isEnabled: Boolean) {
        cameraUploadsRepository.setCameraUploadsEnabled(isEnabled)
        cameraUploadsRepository.getBackupFolderId(CameraUploadFolderType.Primary)?.let { backupId ->
            updateBackupStateUseCase(
                backupId = backupId,
                backupState = if (isEnabled) BackupState.ACTIVE else BackupState.DISABLED
            )
        }
    }
}
