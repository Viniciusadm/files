package com.archieapps.fles.ui.navigation

object AppRoutes {
    const val Login = "login"
    const val Messages = "messages"
    const val Thread = "thread/{folderId}"
    const val Files = "files"
    const val FolderFiles = "files/{folderId}"
    const val Favorites = "favorites"
    const val Search = "search?query={query}"
    const val Key = "key"
    const val Share = "share"

    fun thread(folderId: String): String = "thread/$folderId"
    fun folderFiles(folderId: String): String = "files/$folderId"
    fun search(query: String): String = "search?query=$query"
}
