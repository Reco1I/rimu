package com.reco1l.api.net

/**
 * @author Reco1l
 */
interface IDownloaderObserver
{
    fun onDownloadEnd(downloader: Downloader?) = Unit

    fun onDownloadCancel(downloader: Downloader?) = Unit

    fun onDownloadUpdate(downloader: Downloader?) = Unit

    fun onDownloadError(downloader: Downloader?, exception: Exception) = Unit
}
