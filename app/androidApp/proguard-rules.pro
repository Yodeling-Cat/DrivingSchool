# Rules for the minified release build.
#
# R8 is on for release (see build.gradle.kts), so anything the app reaches without a static
# reference has to be kept here by hand. Everything else — Compose, Koin, kotlinx.serialization,
# Coil, AndroidX — ships its own consumer rules and needs nothing from this file.

# The shared module reads the application's build fields by name, because it is a library and
# generates no BuildConfig of its own:
#
#   app/shared/src/androidMain/.../AppVersion.android.kt    Class.forName(...).getField("VERSION_NAME")
#   app/shared/src/androidMain/.../DebugBuild.android.kt    Class.forName(...).getField("DEBUG")
#
# Those are the app's only reflective lookups. Without this the fields are shrunk away and both
# callers quietly answer null and false — the About screen loses its version line, and the debug
# build stops being recognized as one.
-keep class com.spiraclestudios.autoskola.BuildConfig { *; }

# Keep the source file and line number of every frame, so a crash report from a release build
# points at a line rather than at nothing. The mapping file PostHog reads needs this to be useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
