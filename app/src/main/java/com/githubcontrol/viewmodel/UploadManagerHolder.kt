package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import com.githubcontrol.upload.UploadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin Hilt ViewModel that exposes the singleton [UploadManager] to Compose
 * destinations that aren't already wired through a feature ViewModel. Keeps the
 * Health screen dependency-free of UploadViewModel.
 */
@HiltViewModel
class UploadManagerHolder @Inject constructor(
    val uploadManager: UploadManager
) : ViewModel()
