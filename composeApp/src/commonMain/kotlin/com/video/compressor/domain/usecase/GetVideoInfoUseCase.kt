package com.video.compressor.domain.usecase

import com.video.compressor.domain.model.VideoFile
import com.video.compressor.domain.repository.VideoRepository

/**
 * 获取视频信息用例
 */
class GetVideoInfoUseCase(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(filePath: String): Result<VideoFile> {
        return videoRepository.getVideoInfo(filePath)
    }
}
