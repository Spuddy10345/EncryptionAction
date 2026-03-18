package com.example.encryptaction.ui.navigation

sealed class NavRoutes(val route: String) {

    // Auth
    object Login : NavRoutes("auth/login")
    object Register : NavRoutes("auth/register")

    // Main bottom-nav destinations
    object Files : NavRoutes("files")
    object Encrypt : NavRoutes("encrypt")
    object Decrypt : NavRoutes("decrypt")
    object Sign : NavRoutes("sign")
    object Keys : NavRoutes("keys")
    object Admin : NavRoutes("admin")

    // Sub-screens
    object FileDetail : NavRoutes("files/{fileId}") {
        fun withId(fileId: String) = "files/$fileId"
    }
    object EncryptFile : NavRoutes("encrypt/file")
    object DecryptFile : NavRoutes("encrypt/decrypt/{fileId}") {
        fun withId(fileId: String) = "encrypt/decrypt/$fileId"
    }
    object SignFile : NavRoutes("sign/file")
    object VerifyFile : NavRoutes("sign/verify/{fileId}") {
        fun withId(fileId: String) = "sign/verify/$fileId"
    }
    object KeyDetail : NavRoutes("keys/{userId}") {
        fun withId(userId: String) = "keys/$userId"
    }
    object ImportKey : NavRoutes("keys/import")
    object ExportKey : NavRoutes("keys/export/{userId}") {
        fun withId(userId: String) = "keys/export/$userId"
    }
    object UserManagement : NavRoutes("admin/users")
    object Profile : NavRoutes("profile")
}
