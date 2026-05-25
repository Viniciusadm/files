package com.archieapps.fles.ui.navigation

object AppRoutes {
    const val Login = "login"
    const val Messages = "messages"
    const val Thread = "thread/{folderId}"
    const val Gallery = "gallery"
    const val Files = "files"
    const val FolderFiles = "files/{folderId}"
    const val Settings = "settings"
    const val Search = "search?query={query}"
    const val Key = "key"
    const val SecureFolders = "secure-folders"

    fun thread(folderId: String): String = "thread/$folderId"
    fun folderFiles(folderId: String): String = "files/$folderId"
    fun search(query: String): String = "search?query=$query"
}
