package com.example.mylibrary.ui.viewer

/**
 * In-memory cache that holds the URI and title before navigating to the viewer.
 * This avoids URL-encoding the content:// URI through NavHost which breaks
 * the permission grant.
 */
object ViewerCache {
    var pendingTitle:    String = ""
    var pendingFilePath: String = ""
}