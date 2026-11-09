package dev.legendsayantan.extendroid.echo

/**
 * Tracks whether Echo (remote access) can be used in this build.
 *
 * Echo needs a Firebase configuration (google-services.json) and a backend. Builds made
 * without them keep working, they simply hide every Echo entry point.
 */
object EchoState {
    @Volatile
    var available: Boolean = false
}
